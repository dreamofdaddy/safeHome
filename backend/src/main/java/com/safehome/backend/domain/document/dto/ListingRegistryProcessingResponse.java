package com.safehome.backend.domain.document.dto;

import com.safehome.backend.domain.document.RegistryDocument;
import java.time.LocalDateTime;

public record ListingRegistryProcessingResponse(
    Long listingId,
    Long documentId,
    String status,
    String errorCode,
    Long propertyId,
    Long registrySnapshotId,
    LocalDateTime processedAt
) {
    public static ListingRegistryProcessingResponse from(RegistryDocument document) {
        return new ListingRegistryProcessingResponse(document.getListingId(), document.getId(),
            document.getStatus(), document.getErrorCode(), document.getPropertyId(),
            document.getRegistrySnapshotId(), document.getProcessedAt());
    }
}
