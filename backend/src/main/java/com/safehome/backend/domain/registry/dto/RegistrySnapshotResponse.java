package com.safehome.backend.domain.registry.dto;

import com.safehome.backend.domain.registry.RegistrySnapshot;

import java.time.LocalDateTime;

public record RegistrySnapshotResponse(
        Long id,
        Long propertyId,
        String source,
        LocalDateTime observedAt,
        String documentHash,
        LocalDateTime createdAt
) {

    public static RegistrySnapshotResponse from(
            RegistrySnapshot snapshot
    ) {
        return new RegistrySnapshotResponse(
                snapshot.getId(),
                snapshot.getProperty().getId(),
                snapshot.getSource(),
                snapshot.getObservedAt(),
                snapshot.getDocumentHash(),
                snapshot.getCreatedAt()
        );
    }
}