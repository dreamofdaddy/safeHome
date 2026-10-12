package com.safehome.backend.domain.risk.dto;

import com.safehome.backend.domain.risk.reanalysis.SafetyReanalysisReason;

public record SafetyReanalysisExecutionResponse(
        Long propertyId,
        boolean performed,
        SafetyReanalysisReason reason,
        SafetyAnalysisResponse analysis
) {
}