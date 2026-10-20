package com.safehome.backend.domain.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {
    Optional<AuthToken> findByTokenHash(String tokenHash);
    List<AuthToken> findAllByUserIdAndRevokedAtIsNull(Long userId);
}
