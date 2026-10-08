package com.safehome.backend.domain.property.dto;

import com.safehome.backend.domain.property.PropertyOwner;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PropertyOwnerResponse(
        Long id,
        Long propertyId,
        Long userId,
        BigDecimal ownershipRatio,
        LocalDateTime verifiedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static PropertyOwnerResponse from(PropertyOwner propertyOwner) {
        return new PropertyOwnerResponse(
                propertyOwner.getId(),
                propertyOwner.getProperty().getId(),
                propertyOwner.getUser().getId(),
                propertyOwner.getOwnershipRatio(),
                propertyOwner.getVerifiedAt(),
                propertyOwner.getCreatedAt(),
                propertyOwner.getUpdatedAt()
        );
    }
}