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
    LocalDateTime processedAt,
    AddressMismatch addressMismatch
) {
    public record AddressMismatch(String registeredAddress,String lotAddress,String roadAddress) {}
    public static ListingRegistryProcessingResponse from(RegistryDocument document) { return from(document,null); }
    public static ListingRegistryProcessingResponse from(RegistryDocument document,AddressMismatch mismatch) {
        return new ListingRegistryProcessingResponse(document.getListingId(), document.getId(),
            document.getStatus(), document.getErrorCode(), document.getPropertyId(),
            document.getRegistrySnapshotId(), document.getProcessedAt(), mismatch);
    }
}
