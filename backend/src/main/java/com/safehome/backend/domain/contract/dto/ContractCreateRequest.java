package com.safehome.backend.domain.contract.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class ContractCreateRequest {

    @NotNull
    private Long propertyId;

    private Long listingId;

    @NotNull
    private Long landlordUserId;

    @NotNull
    private Long tenantUserId;

    @NotNull
    @Size(max = 30)
    private String contractType;

    private BigDecimal depositAmount;

    private BigDecimal monthlyRent;

    @NotNull
    private LocalDate startDate;

    @NotNull
    private LocalDate endDate;

    @Size(max = 30)
    private String status;

    private LocalDateTime signedAt;

    public ContractCreateRequest() {
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public void setPropertyId(Long propertyId) {
        this.propertyId = propertyId;
    }

    public Long getListingId() {
        return listingId;
    }

    public void setListingId(Long listingId) {
        this.listingId = listingId;
    }

    public Long getLandlordUserId() {
        return landlordUserId;
    }

    public void setLandlordUserId(Long landlordUserId) {
        this.landlordUserId = landlordUserId;
    }

    public Long getTenantUserId() {
        return tenantUserId;
    }

    public void setTenantUserId(Long tenantUserId) {
        this.tenantUserId = tenantUserId;
    }

    public String getContractType() {
        return contractType;
    }

    public void setContractType(String contractType) {
        this.contractType = contractType;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public void setDepositAmount(BigDecimal depositAmount) {
        this.depositAmount = depositAmount;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public void setMonthlyRent(BigDecimal monthlyRent) {
        this.monthlyRent = monthlyRent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public void setSignedAt(LocalDateTime signedAt) {
        this.signedAt = signedAt;
    }
}