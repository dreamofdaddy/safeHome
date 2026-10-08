package com.safehome.backend.domain.listing.dto;

import com.safehome.backend.domain.listing.Listing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingResponse(
        Long id,
        Long propertyId,
        Long sellerUserId,
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
                listing.getSellerUserId(),
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