package com.safehome.backend.domain.auth;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "auth_tokens")
public class AuthToken {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected AuthToken() {}
    public AuthToken(Long userId, String tokenHash, LocalDateTime expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = LocalDateTime.now();
    }
    public Long getUserId() { return userId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getRevokedAt() { return revokedAt; }
    public void revoke() { revokedAt = LocalDateTime.now(); }
}
