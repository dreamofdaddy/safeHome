package com.safehome.backend.domain.contract;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "contracts")
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "listing_id")
    private Long listingId;

    @Column(name = "landlord_user_id", nullable = false)
    private Long landlordUserId;

    @Column(name = "tenant_user_id", nullable = false)
    private Long tenantUserId;

    @Column(name = "contract_type", nullable = false, length = 30)
    private String contractType;

    @Column(name = "deposit_amount", precision = 15, scale = 0)
    private BigDecimal depositAmount;

    @Column(name = "monthly_rent", precision = 15, scale = 0)
    private BigDecimal monthlyRent;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Contract() {
    }

    public Contract(
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
            LocalDateTime signedAt
    ) {
        this.propertyId = propertyId;
        this.listingId = listingId;
        this.landlordUserId = landlordUserId;
        this.tenantUserId = tenantUserId;
        this.contractType = contractType;
        this.depositAmount = depositAmount;
        this.monthlyRent = monthlyRent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.signedAt = signedAt;
    }

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();

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

    public Long getListingId() {
        return listingId;
    }

    public Long getLandlordUserId() {
        return landlordUserId;
    }

    public Long getTenantUserId() {
        return tenantUserId;
    }

    public String getContractType() {
        return contractType;
    }

    public BigDecimal getDepositAmount() {
        return depositAmount;
    }

    public BigDecimal getMonthlyRent() {
        return monthlyRent;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSignedAt() {
        return signedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String contractType,
            BigDecimal depositAmount,
            BigDecimal monthlyRent,
            LocalDate startDate,
            LocalDate endDate,
            String status,
            LocalDateTime signedAt
    ) {
        this.contractType = contractType;
        this.depositAmount = depositAmount;
        this.monthlyRent = monthlyRent;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
        this.signedAt = signedAt;
    }
}