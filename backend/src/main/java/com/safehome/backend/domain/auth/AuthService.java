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

@Service
public class AuthService {
    private static final SecureRandom RANDOM = new SecureRandom();
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
        if (users.existsByEmail(email)) throw new BusinessException(ErrorCode.INVALID_REQUEST);
        User user = new User(email, request.name().trim(), request.phone(), "UNVERIFIED", "ACTIVE");
        user.setPasswordHash(passwords.encode(request.password()));
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
        AuthToken token = requireToken(authorization);
        User user = users.findById(token.getUserId())
            .orElseThrow(() -> new BusinessException(ErrorCode.UNAUTHORIZED));
        if (!"ACTIVE".equals(user.getStatus())) throw new BusinessException(ErrorCode.UNAUTHORIZED);
        return user;
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

    private String hash(String raw) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
}
