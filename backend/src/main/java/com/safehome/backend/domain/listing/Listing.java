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

    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "risk_analysis_id")
    private Long riskAnalysisId;

    @Column(name = "registered_by_user_id", nullable = false)
    private Long registeredByUserId;

    @Column(name = "listing_address", length = 500)
    private String listingAddress;
    @Column(name = "road_address", length = 500)
    private String roadAddress;
    @Column(name = "building_name", length = 200)
    private String buildingName;
    @Column(name = "building_type", length = 50)
    private String buildingType;
    @Column(name = "unit_number", length = 100)
    private String unitNumber;
    @Column(name = "postal_code", length = 20)
    private String postalCode;

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
            Long registeredByUserId,
            String transactionType,
            BigDecimal depositAmount,
            BigDecimal monthlyRent,
            BigDecimal salePrice,
            String status,
            LocalDateTime listedAt,
            LocalDateTime expiredAt
    ) {
        this.propertyId = propertyId;
        this.registeredByUserId = registeredByUserId;
        this.transactionType = transactionType;
        this.depositAmount = depositAmount;
        this.monthlyRent = monthlyRent;
        this.salePrice = salePrice;
        this.status = status;
        this.listedAt = listedAt;
        this.expiredAt = expiredAt;
    }

    public static Listing pendingAnalysis(Long userId, String address, String roadAddress,
            String buildingName, String buildingType, String unitNumber, String postalCode,
            String transactionType, BigDecimal depositAmount, BigDecimal monthlyRent,
            BigDecimal salePrice, LocalDateTime listedAt, LocalDateTime expiredAt) {
        Listing listing = new Listing(null, userId, transactionType, depositAmount, monthlyRent,
            salePrice, "READY_FOR_ANALYSIS", listedAt, expiredAt);
        listing.listingAddress = address;
        listing.roadAddress = roadAddress;
        listing.buildingName = buildingName;
        listing.buildingType = buildingType;
        listing.unitNumber = unitNumber;
        listing.postalCode = postalCode;
        return listing;
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

    public String getListingAddress() { return listingAddress; }
    public String getRoadAddress() { return roadAddress; }
    public String getBuildingName() { return buildingName; }
    public String getBuildingType() { return buildingType; }
    public String getUnitNumber() { return unitNumber; }
    public String getPostalCode() { return postalCode; }

    public Long getPropertyId() {
        return propertyId;
    }

    public Long getRiskAnalysisId() {
        return riskAnalysisId;
    }

    public Long getRegisteredByUserId() {
        return registeredByUserId;
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

    public void markRegistryProcessing() {
        status = "REGISTRY_PROCESSING"; updatedAt = LocalDateTime.now();
    }
    public void markRegistryParsed(Long propertyId) {
        this.propertyId = propertyId; status = "REGISTRY_READY"; updatedAt = LocalDateTime.now();
    }
    public void markRegistryFailed() {
        status = "REGISTRY_FAILED"; updatedAt = LocalDateTime.now();
    }

    public void markSafetyAnalyzed(Long riskAnalysisId) {
        this.riskAnalysisId = riskAnalysisId;
        status = "SAFETY_ANALYZED";
        updatedAt = LocalDateTime.now();
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
