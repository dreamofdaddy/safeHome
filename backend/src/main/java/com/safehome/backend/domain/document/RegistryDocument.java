package com.safehome.backend.domain.document;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "registry_documents")
public class RegistryDocument {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "uploaded_by_user_id", nullable = false)
    private Long uploadedByUserId;
    @Column(name = "property_id")
    private Long propertyId;
    @Column(name = "registry_snapshot_id")
    private Long registrySnapshotId;
    @Column(name = "listing_id")
    private Long listingId;
    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;
    @Column(name = "content_type", nullable = false, length = 100)
    private String contentType;
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;
    @Column(name = "encrypted_content", columnDefinition = "bytea")
    private byte[] encryptedContent;
    @Column(name = "storage_key", length = 64)
    private String storageKey;
    @Column(name = "parser_provider", length = 30)
    private String parserProvider;
    @Column(name = "parser_model", length = 100)
    private String parserModel;
    @Column(name = "status", nullable = false, length = 30)
    private String status;
    @Column(name = "parsed_registry_property_number", length = 50)
    private String parsedRegistryPropertyNumber;
    @Column(name = "error_code", length = 100)
    private String errorCode;
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name="analysis_request_id") private java.util.UUID analysisRequestId;
    @Column(name="analysis_consented_at") private LocalDateTime analysisConsentedAt;
    public void consentToAnalysis(java.util.UUID requestId) { analysisRequestId=requestId;analysisConsentedAt=LocalDateTime.now(); }
    protected RegistryDocument() {}
    public RegistryDocument(Long userId, Long propertyId, Long listingId, String filename,
                            long size, String sha256, String storageKey) {
        this.uploadedByUserId = userId;
        this.propertyId = propertyId;
        this.listingId = listingId;
        this.originalFilename = filename;
        this.contentType = "application/pdf";
        this.sizeBytes = size;
        this.sha256 = sha256;
        this.storageKey = storageKey;
        this.status = "UPLOADED";
        this.createdAt = LocalDateTime.now();
        this.updatedAt = this.createdAt;
    }
    public Long getId() { return id; }
    public Long getUploadedByUserId() { return uploadedByUserId; }
    public Long getPropertyId() { return propertyId; }
    public Long getListingId() { return listingId; }
    public String getOriginalFilename() { return originalFilename; }
    public Long getSizeBytes() { return sizeBytes; }
    public String getSha256() { return sha256; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public byte[] getEncryptedContent() { return encryptedContent; }
    public String getStorageKey() { return storageKey; }
    public Long getRegistrySnapshotId() { return registrySnapshotId; }
    public String getErrorCode() { return errorCode; }
    public LocalDateTime getProcessedAt() { return processedAt; }
    public void startProcessing() {
        status = "PROCESSING"; errorCode = null; processedAt = null; updatedAt = LocalDateTime.now();
    }
    public void markParsed(Long propertyId, Long snapshotId,
                           String propertyNumber, String provider, String model) {
        this.propertyId = propertyId; this.registrySnapshotId = snapshotId;
        this.parsedRegistryPropertyNumber = propertyNumber;
        this.parserProvider = provider; this.parserModel = model;
        status = "PARSED"; errorCode = null; processedAt = LocalDateTime.now();
        updatedAt = processedAt;
    }
    public void fail(String code) {
        status = "FAILED"; errorCode = code; processedAt = LocalDateTime.now();
        updatedAt = processedAt;
    }
}
