package com.safehome.backend.domain.buildingregister;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class BuildingRegisterModels {
    private BuildingRegisterModels() {}
    public record FileRef(String name, long sizeBytes, int pages, String sha256, String storageKey) {}
    public record FileInfo(int index, String name, long sizeBytes, int pages) {}
    public record Context(String address, String detailAddress, String buildingType,
            Long registryDocumentId, Long registrySnapshotId, String registryAddress, String registryRoadAddress,
            String registryUnit, Long apiIdentificationId, String apiExecutionStatus,
            String apiResolutionStatus, String apiUnit, String apiReason) {}
    public record Evidence(int page, String kind, String text) {}
    public record Entry(int fileIndex, int page, String documentType, String issuedAt,
            String jibunAddress, String roadAddress, String buildingName, String dongName,
            String floorLabel, String unitName, String purpose, BigDecimal areaSquareMeters,
            String violationStatus, List<Evidence> evidence) {}
    public record Extraction(boolean buildingRegisterDocument, List<Entry> entries) {}
    public record Check(int entryIndex, String listingAddress, String registryAddress,
            String unit, String apiUnit, String purpose, String dong) {}
    public record Result(List<Entry> entries, List<Check> checks, boolean violationMentioned,
            String conclusion, Long registrySnapshotId, Long apiIdentificationId) {}
    public record Stored(long id, long listingId, long ownerId, String status, List<FileRef> files,
            Context context, Result result, String errorCode, int attemptCount, String provider,
            String model, OffsetDateTime createdAt, OffsetDateTime processedAt) {}
    public record Review(long id, String status, String requiredDocumentType, List<FileInfo> files, Result result,
            String errorCode, int attemptCount, String provider, String model,
            boolean stale, OffsetDateTime createdAt, OffsetDateTime processedAt) {}
    public record Summary(long id, String status, boolean stale, OffsetDateTime createdAt) {}
    public record Envelope(Review latest, List<Summary> history) {}
}
