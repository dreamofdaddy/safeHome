package com.safehome.backend.domain.document.dto;

import jakarta.validation.constraints.*;
import com.safehome.backend.domain.listing.AddressSearchEvidence;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ListingRegistrationRequest(
    @NotBlank @Size(max = 500) String address,
    @Size(max = 200) String buildingName,
    @NotBlank @Size(max = 50) String buildingType,
    @Size(max = 500) String detailAddress,
    @NotBlank String transactionType,
    BigDecimal depositAmount,
    BigDecimal monthlyRent,
    BigDecimal salePrice,
    LocalDateTime listedAt,
    LocalDateTime expiredAt,
    AddressSearchEvidence addressSearchEvidence
) {
    public ListingRegistrationRequest(String address, String buildingName, String buildingType,
            String detailAddress, String transactionType, BigDecimal depositAmount,
            BigDecimal monthlyRent, BigDecimal salePrice, LocalDateTime listedAt,
            LocalDateTime expiredAt) {
        this(address, buildingName, buildingType, detailAddress, transactionType,
                depositAmount, monthlyRent, salePrice, listedAt, expiredAt, null);
    }
}
