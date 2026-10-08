package com.safehome.backend.domain.listing.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class ListingUpdateRequest {

    @NotNull
    @Size(max = 30)
    private String transactionType;

    private BigDecimal depositAmount;

    private BigDecimal monthlyRent;

    private BigDecimal salePrice;

    @NotNull
    @Size(max = 30)
    private String status;

    @NotNull
    private LocalDateTime listedAt;

    private LocalDateTime expiredAt;

    public ListingUpdateRequest() {
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
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

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getListedAt() {
        return listedAt;
    }

    public void setListedAt(LocalDateTime listedAt) {
        this.listedAt = listedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public void setExpiredAt(LocalDateTime expiredAt) {
        this.expiredAt = expiredAt;
    }
}