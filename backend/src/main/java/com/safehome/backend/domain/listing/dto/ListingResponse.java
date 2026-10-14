package com.safehome.backend.domain.listing.dto;

import com.safehome.backend.domain.listing.Listing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingResponse(
        Long id,
        Long propertyId,
        Long riskAnalysisId,
        Long registeredByUserId,
        String listingAddress,
        String roadAddress,
        String buildingName,
        String buildingType,
        String unitNumber,
        String postalCode,
        String transactionType,
        BigDecimal depositAmount,
        BigDecimal monthlyRent,
        BigDecimal salePrice,
        String status,
        LocalDateTime listedAt,
        LocalDateTime expiredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ListingResponse from(Listing listing) {
        return new ListingResponse(
                listing.getId(),
                listing.getPropertyId(),
                listing.getRiskAnalysisId(),
                listing.getRegisteredByUserId(),
                listing.getListingAddress(),
                listing.getRoadAddress(),
                listing.getBuildingName(),
                listing.getBuildingType(),
                listing.getUnitNumber(),
                listing.getPostalCode(),
                listing.getTransactionType(),
                listing.getDepositAmount(),
                listing.getMonthlyRent(),
                listing.getSalePrice(),
                listing.getStatus(),
                listing.getListedAt(),
                listing.getExpiredAt(),
                listing.getCreatedAt(),
                listing.getUpdatedAt()
        );
    }
}
