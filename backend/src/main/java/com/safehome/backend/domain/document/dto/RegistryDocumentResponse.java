package com.safehome.backend.domain.document.dto;

import com.safehome.backend.domain.document.RegistryDocument;
import java.time.LocalDateTime;

public record RegistryDocumentResponse(Long id, Long propertyId, Long listingId,
    String originalFilename, Long sizeBytes, String sha256, String status,
    Long registrySnapshotId, String errorCode,
    LocalDateTime processedAt, LocalDateTime createdAt) {
    public static RegistryDocumentResponse from(RegistryDocument d) {
        return new RegistryDocumentResponse(d.getId(), d.getPropertyId(), d.getListingId(),
            d.getOriginalFilename(), d.getSizeBytes(), d.getSha256(), d.getStatus(),
            d.getRegistrySnapshotId(), d.getErrorCode(),
            d.getProcessedAt(), d.getCreatedAt());
    }
}
