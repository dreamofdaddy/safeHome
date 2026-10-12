package com.safehome.backend.domain.registry.dto;

import java.util.List;

public record RegistrySnapshotComparisonResponse(
        Long propertyId,
        RegistrySnapshotResponse previousSnapshot,
        RegistrySnapshotResponse currentSnapshot,
        boolean sourceChanged,
        boolean documentChanged,
        boolean rightsChanged,
        List<RegistryRightComparisonResponse> rightChanges
) {
}