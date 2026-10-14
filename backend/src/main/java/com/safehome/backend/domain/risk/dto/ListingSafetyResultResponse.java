package com.safehome.backend.domain.risk.dto;

import com.safehome.backend.domain.risk.reanalysis.SafetyReanalysisReason;

public record ListingSafetyResultResponse(
    Long listingId,
    String listingStatus,
    SafetyAnalysisResponse analysis,
    String summary,
    String highestSeverity,
    int factorCount,
    Long latestRegistrySnapshotId,
    boolean reanalysisAvailable,
    SafetyReanalysisReason reanalysisReason
) {}
