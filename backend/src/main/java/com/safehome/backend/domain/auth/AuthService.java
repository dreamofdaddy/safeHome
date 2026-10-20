package com.safehome.backend.domain.auth;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.auth.dto.*;
import com.safehome.backend.domain.user.User;
import com.safehome.backend.domain.user.UserRepository;
import com.safehome.backend.domain.user.dto.UserResponse;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^(?=.{1,64}@)[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            + "@(?:[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?\\.)+[A-Za-z]{2,63}$");
    private final UserRepository users;
    private final AuthTokenRepository tokens;
    private final BCryptPasswordEncoder passwords = new BCryptPasswordEncoder();

    public AuthService(UserRepository users, AuthTokenRepository tokens) {
        this.users = users;
        this.tokens = tokens;
    }

    @Transactional
    public UserResponse signup(SignupRequest request) {
        String email = normalize(request.email());
        if (!EMAIL_PATTERN.matcher(email).matches()) throw new BusinessException(ErrorCode.INVALID_EMAIL);
        if (users.existsByEmail(email)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        assertStrongPassword(request.password());
        User user = new User(email, request.name().trim(), normalizePhone(request.phone()), "UNVERIFIED", "ACTIVE");
        user.setPasswordHash(passwords.encode(request.password()));
        user.markPasswordChangedNow();
        return UserResponse.from(users.save(user));
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = users.findByEmail(normalize(request.email()))
            .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
        if (!"ACTIVE".equals(user.getStatus()) || user.getPasswordHash() == null
                || !passwords.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
        byte[] secret = new byte[32];
        RANDOM.nextBytes(secret);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(secret);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(12);
        tokens.save(new AuthToken(user.getId(), hash(raw), expiresAt));
        return new LoginResponse(raw, "Bearer", expiresAt, UserResponse.from(user));
    }

    @Transactional(readOnly = true)
    public User requireUser(String authorization) {
        User user = requireUserAllowExpiredPassword(authorization);
        if (user.isPasswordChangeRequired()) throw new BusinessException(ErrorCode.PASSWORD_CHANGE_REQUIRED);
        return user;
    }

    @Transactional(readOnly = true)
    public User requireUserAllowExpiredPassword(String authorization) {
        AuthToken token = requireToken(authorization);
        User user = users.findById(token.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!"ACTIVE".equals(user.getStatus())) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return user;
    }

    @Transactional
    public UserResponse changePassword(String authorization, ChangePasswordRequest request) {
        User user = requireUserAllowExpiredPassword(authorization);
        if (!passwords.matches(request.currentPassword(), user.getPasswordHash()))
            throw new BusinessException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
        assertStrongPassword(request.newPassword());
        boolean reusedCurrent = passwords.matches(request.newPassword(), user.getPasswordHash());
        boolean reusedPrevious = user.getPreviousPasswordHash() != null
            && passwords.matches(request.newPassword(), user.getPreviousPasswordHash());
        if (reusedCurrent || reusedPrevious) throw new BusinessException(ErrorCode.PASSWORD_REUSE);
        user.changePassword(passwords.encode(request.newPassword()));
        tokens.findAllByUserIdAndRevokedAtIsNull(user.getId()).forEach(AuthToken::revoke);
        return UserResponse.from(user);
    }

    @Transactional
    public void logout(String authorization) { requireToken(authorization).revoke(); }

    private AuthToken requireToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")
                || authorization.length() > 100 || authorization.length() <= 7) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        String raw = authorization.substring(7);
        return tokens.findByTokenHash(hash(raw))
            .filter(t -> t.getRevokedAt() == null && t.getExpiresAt().isAfter(LocalDateTime.now()))
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
    }

    private String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String digits = phone.replaceAll("[^0-9]", "");
        if (!digits.matches("010[0-9]{8}")) throw new BusinessException(ErrorCode.INVALID_PHONE);
        return digits.substring(0, 3) + "-" + digits.substring(3, 7) + "-" + digits.substring(7);
    }

    private void assertStrongPassword(String password) {
        if (password == null || password.length() < 12 || password.length() > 72
                || password.chars().anyMatch(Character::isWhitespace)
                || !password.matches(".*[A-Za-z].*") || !password.matches(".*[0-9].*")
                || !password.matches(".*[^A-Za-z0-9\\s].*")) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD_POLICY);
        }
    }

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
