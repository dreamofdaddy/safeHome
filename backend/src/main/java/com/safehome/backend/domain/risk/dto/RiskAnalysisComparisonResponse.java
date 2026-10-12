package com.safehome.backend.domain.risk.dto;

import com.safehome.backend.domain.risk.comparison.RiskTrend;

import java.math.BigDecimal;
import java.util.List;

public record RiskAnalysisComparisonResponse(
        Long propertyId,
        RiskAnalysisResponse previousAnalysis,
        RiskAnalysisResponse currentAnalysis,
        BigDecimal scoreDelta,
        RiskTrend trend,
        boolean policyVersionChanged,
        List<RiskFactorComparisonResponse> factorChanges
) {
}