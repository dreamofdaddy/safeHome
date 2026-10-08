package com.safehome.backend.domain.listing;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "seller_user_id", nullable = false)
    private Long sellerUserId;

    @Column(name = "transaction_type", nullable = false, length = 30)
    private String transactionType;

    @Column(name = "deposit_amount", precision = 15, scale = 0)
    private BigDecimal depositAmount;

    @Column(name = "monthly_rent", precision = 15, scale = 0)
    private BigDecimal monthlyRent;

    @Column(name = "sale_price", precision = 15, scale = 0)
    private BigDecimal salePrice;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "listed_at", nullable = false)
    private LocalDateTime listedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Listing() {
    }

    public Listing(
            Long propertyId,
            Long sellerUserId,
            String transactionType,
            BigDecimal depositAmount,
            BigDecimal monthlyRent,
            BigDecimal salePrice,
            String status,
            LocalDateTime listedAt,
            LocalDateTime expiredAt
    ) {
        this.propertyId = propertyId;
        this.sellerUserId = sellerUserId;
        this.transactionType = transactionType;
        this.depositAmount = depositAmount;
        this.monthlyRent = monthlyRent;
        this.salePrice = salePrice;
        this.status = status;
        this.listedAt = listedAt;
        this.expiredAt = expiredAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

        if (listedAt == null) {
            listedAt = now;
        }

        if (createdAt == null) {
            createdAt = now;
        }

        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public Long getSellerUserId() {
        return sellerUserId;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getListedAt() {
        return listedAt;
    }

    public LocalDateTime getExpiredAt() {
        return expiredAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String transactionType,
            BigDecimal depositAmount,
            BigDecimal monthlyRent,
            BigDecimal salePrice,
            String status,
            LocalDateTime listedAt,
            LocalDateTime expiredAt
    ) {
        this.transactionType = transactionType;
        this.depositAmount = depositAmount;
        this.monthlyRent = monthlyRent;
        this.salePrice = salePrice;
        this.status = status;
        this.listedAt = listedAt;
        this.expiredAt = expiredAt;
    }
}