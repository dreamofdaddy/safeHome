package com.safehome.backend.domain.property.dto;

import com.safehome.backend.domain.property.PropertyOwner;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PropertyOwnerResponse(
        Long id,
        Long propertyId,
        Long registrySnapshotId,
        String ownerName,
        BigDecimal ownershipRatio,
        LocalDateTime registeredAt,
        LocalDateTime verifiedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static PropertyOwnerResponse from(PropertyOwner owner) {
        return new PropertyOwnerResponse(
                owner.getId(), owner.getProperty().getId(),
                owner.getRegistrySnapshot().getId(), owner.getOwnerName(),
                owner.getOwnershipRatio(), owner.getRegisteredAt(),
                owner.getVerifiedAt(), owner.getCreatedAt(), owner.getUpdatedAt());
    }
}
