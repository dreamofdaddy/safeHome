package com.safehome.backend.integration.building;

import java.time.Instant;

public record BuildingIdentificationResponse(Long id, Long registrySnapshotId,
        String executionStatus, Resolution.Status resolutionStatus, String reason,
        Integer candidateCount, String managementNumber, String parentManagementNumber,
        String unitName, String source, Instant queriedAt) {
    public static BuildingIdentificationResponse notRequested() {
        return new BuildingIdentificationResponse(null, null, "NOT_REQUESTED", null,
                null, null, null, null, null, null, null);
    }
}
