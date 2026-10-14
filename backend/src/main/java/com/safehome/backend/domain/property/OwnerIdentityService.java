package com.safehome.backend.domain.property;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HexFormat;

@Service
public class OwnerIdentityService {
    private final JdbcTemplate jdbcTemplate;
    private final String encryptionKey;
    private final String hashKey;

    public OwnerIdentityService(JdbcTemplate jdbcTemplate,
                                @Value("${safehome.owner-identity.encryption-key:}") String encryptionKey,
                                @Value("${safehome.owner-identity.hash-key:}") String hashKey) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionKey = encryptionKey;
        this.hashKey = hashKey;
    }

    public byte[] encryptMaskedIdentifier(String maskedIdentifier) {
        validate(maskedIdentifier);
        requireKeys();
        return jdbcTemplate.queryForObject(
                "SELECT pgp_sym_encrypt(?, ?, 'cipher-algo=aes256')",
                byte[].class, maskedIdentifier, encryptionKey);
    }

    public String hashBirth6(String maskedIdentifier) {
        validate(maskedIdentifier);
        requireKeys();
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hashKey.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(maskedIdentifier.substring(0, 6)
                    .getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("소유자 식별정보 해시 생성 실패", ex);
        }
    }

    private void validate(String value) {
        if (value == null || !value.matches("[0-9]{6}-\\*{7}")) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void requireKeys() {
        if (encryptionKey.length() < 32 || hashKey.length() < 32) {
            throw new IllegalStateException("소유자 식별정보 암호화 키가 설정되지 않았습니다.");
        }
    }
}
