package com.safehome.backend.domain.registry.dto;

import com.safehome.backend.domain.registry.comparison.RegistryRightChangeType;

public record RegistryRightComparisonResponse(
        RegistryRightChangeType changeType,
        RegistryRightSnapshotResponse previous,
        RegistryRightSnapshotResponse current
) {
}