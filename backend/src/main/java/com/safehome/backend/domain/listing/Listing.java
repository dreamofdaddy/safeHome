package com.safehome.backend.domain.listing;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "listings")
@org.hibernate.annotations.SQLDelete(sql="UPDATE listings SET deleted_at=CURRENT_TIMESTAMP, updated_at=CURRENT_TIMESTAMP WHERE id=?")
@org.hibernate.annotations.SQLRestriction("deleted_at IS NULL")
public class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id")
    private Long propertyId;

    @Column(name = "risk_analysis_id")
    private Long riskAnalysisId;

    @Column(name = "current_building_identification_id")
    private Long currentBuildingIdentificationId;

    @Column(name = "registered_by_user_id", nullable = false)
    private Long registeredByUserId;

    @Column(name = "address", nullable = false, length = 500)
    private String address;
    @Column(name = "building_name", length = 200)
    private String buildingName;
    @Column(name = "building_type", length = 50)
    private String buildingType;
    @Column(name = "detail_address", length = 500)
    private String detailAddress;
    @Column(name = "address_search_type", length = 1)
    private String addressSearchType;
    @Column(name = "address_search_selected_address", length = 500)
    private String addressSearchSelectedAddress;
    @Column(name = "address_search_jibun_address", length = 500)
    private String addressSearchJibunAddress;
    @Column(name = "address_search_bcode", length = 10)
    private String addressSearchBcode;
    @Column(name = "address_search_sigungu_code", length = 5)
    private String addressSearchSigunguCode;
    @Column(name = "address_search_no_selected", length = 1)
    private String addressSearchNoSelected;

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

    public static Listing pendingAnalysis(Long userId, String address,
            String buildingName, String buildingType, String detailAddress,
            String transactionType, BigDecimal depositAmount, BigDecimal monthlyRent,
            BigDecimal salePrice, LocalDateTime listedAt, LocalDateTime expiredAt) {
        Listing listing = new Listing(null, userId, transactionType, depositAmount, monthlyRent,
            salePrice, "READY_FOR_ANALYSIS", listedAt, expiredAt);
        listing.address = address;
        listing.buildingName = buildingName;
        listing.buildingType = buildingType;
        listing.detailAddress = detailAddress;
        return listing;
    }

    public void setAddressSearchEvidence(AddressSearchEvidence evidence) {
        addressSearchType = evidence == null ? null : evidence.selectedType();
        addressSearchSelectedAddress = evidence == null ? null : evidence.selectedAddress();
        addressSearchJibunAddress = evidence == null ? null : evidence.jibunAddress();
        addressSearchBcode = evidence == null ? null : evidence.bcode();
        addressSearchSigunguCode = evidence == null ? null : evidence.sigunguCode();
        addressSearchNoSelected = evidence == null ? null : evidence.noSelected();
    }

    public AddressSearchEvidence getAddressSearchEvidence() {
        return addressSearchType == null ? null : new AddressSearchEvidence(addressSearchType,
                addressSearchSelectedAddress, addressSearchJibunAddress, addressSearchBcode,
                addressSearchSigunguCode, addressSearchNoSelected);
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

    public String getAddress() { return address; }
    public String getFullAddress() {
        return address + (detailAddress == null || detailAddress.isBlank() ? "" : " " + detailAddress.trim());
    }
    public void reviseTerms(com.safehome.backend.domain.document.dto.ListingRegistrationRequest request) {
        transactionType=request.transactionType();depositAmount=request.depositAmount();monthlyRent=request.monthlyRent();salePrice=request.salePrice();
        if(request.listedAt()!=null)listedAt=request.listedAt();
        if(request.expiredAt()!=null)expiredAt=request.expiredAt();updatedAt=LocalDateTime.now();
    }
    public void revise(com.safehome.backend.domain.document.dto.ListingRegistrationRequest request) {
        if (!address.equals(request.address().trim())
                || !java.util.Objects.equals(detailAddress,
                    request.detailAddress() == null ? null : request.detailAddress().trim())) {
            currentBuildingIdentificationId = null;
        }
        address = request.address().trim();
        detailAddress = request.detailAddress() == null ? null : request.detailAddress().trim();
        buildingName = request.buildingName(); buildingType = request.buildingType();
        transactionType = request.transactionType(); depositAmount = request.depositAmount();
        monthlyRent = request.monthlyRent(); salePrice = request.salePrice();
        if (request.listedAt() != null) listedAt = request.listedAt();
        expiredAt = request.expiredAt(); updatedAt = LocalDateTime.now();
    }
    public String getBuildingName() { return buildingName; }
    public String getBuildingType() { return buildingType; }
    public String getDetailAddress() { return detailAddress; }

    public Long getPropertyId() {
        return propertyId;
    }

    public Long getRiskAnalysisId() {
        return riskAnalysisId;
    }

    public Long getCurrentBuildingIdentificationId() { return currentBuildingIdentificationId; }
    public void setCurrentBuildingIdentificationId(Long id) { currentBuildingIdentificationId = id; }

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

    public void requireRegistryReview() { status="READY_FOR_ANALYSIS"; currentBuildingIdentificationId=null; updatedAt=LocalDateTime.now(); }

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

}
