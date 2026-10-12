package com.safehome.backend.domain.property.dto;

import com.safehome.backend.domain.risk.comparison.RiskTrend;
import com.safehome.backend.domain.risk.dto.RiskAnalysisResponse;

import java.math.BigDecimal;
import java.util.List;

public record PropertySafetyTrendResponse(
        Long propertyId,
        int analysisCount,
        RiskAnalysisResponse latestAnalysis,
        RiskAnalysisResponse previousAnalysis,
        BigDecimal scoreDelta,
        RiskTrend trend,
        boolean policyVersionChanged,
        List<RiskAnalysisResponse> history
) {
    public PropertySafetyTrendResponse {
        history = List.copyOf(history);
    }
}