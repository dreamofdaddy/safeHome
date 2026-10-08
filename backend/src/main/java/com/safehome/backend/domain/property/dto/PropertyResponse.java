package com.safehome.backend.domain.property.dto;

import com.safehome.backend.domain.property.Property;

import java.time.LocalDateTime;

public record PropertyResponse(
    Long id,
    String address,
    String roadAddress,
    String buildingName,
    String buildingType,
    String unitNumber,
    String postalCode,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {

    public static PropertyResponse from(Property property) {
        return new PropertyResponse(
            property.getId(),
            property.getAddress(),
            property.getRoadAddress(),
            property.getBuildingName(),
            property.getBuildingType(),
            property.getUnitNumber(),
            property.getPostalCode(),
            property.getCreatedAt(),
            property.getUpdatedAt()
        );
    }
}