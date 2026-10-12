package com.safehome.backend.domain.risk.dto;

import com.safehome.backend.domain.risk.reanalysis.SafetyReanalysisReason;

public record SafetyReanalysisDecisionResponse(
        Long propertyId,
        boolean required,
        SafetyReanalysisReason reason,
        Long latestSnapshotId,
        Long lastAnalyzedSnapshotId,
        String activePolicyVersion,
        String lastPolicyVersion
) {
}