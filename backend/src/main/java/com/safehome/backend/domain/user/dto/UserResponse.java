package com.safehome.backend.domain.user.dto;

import com.safehome.backend.domain.user.User;

import java.time.LocalDateTime;

public record UserResponse(
        Long id,
        String email,
        String name,
        String phone,
        String verificationStatus,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getVerificationStatus(),
                user.getStatus(),
                user.getCreatedAt(),
                user.getUpdatedAt()
        );
    }
}