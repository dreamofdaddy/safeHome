package com.safehome.backend.domain.auth.dto;

import com.safehome.backend.domain.user.dto.UserResponse;
import java.time.LocalDateTime;

public record LoginResponse(String accessToken, String tokenType,
                            LocalDateTime expiresAt, UserResponse user) {}
