package com.safehome.backend.domain.contract.dto;

import com.safehome.backend.domain.contract.Contract;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContractResponse(
        Long id,
        Long propertyId,
        Long listingId,
        Long landlordUserId,
        Long tenantUserId,
        String contractType,
        BigDecimal depositAmount,
        BigDecimal monthlyRent,
        LocalDate startDate,
        LocalDate endDate,
        String status,
        LocalDateTime signedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ContractResponse from(Contract contract) {
        return new ContractResponse(
                contract.getId(),
                contract.getPropertyId(),
                contract.getListingId(),
                contract.getLandlordUserId(),
                contract.getTenantUserId(),
                contract.getContractType(),
                contract.getDepositAmount(),
                contract.getMonthlyRent(),
                contract.getStartDate(),
                contract.getEndDate(),
                contract.getStatus(),
                contract.getSignedAt(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }
}