package com.safehome.backend.domain.risk.dto;

import com.safehome.backend.domain.risk.RiskAnalysis;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskAnalysisResponse(
        Long id,
        Long propertyId,
        Long registrySnapshotId,
        String analysisVersion,
        String policyVersion,
        String riskLevel,
        BigDecimal riskScore,
        LocalDateTime analyzedAt,
        LocalDateTime createdAt
) {
    public static RiskAnalysisResponse from(RiskAnalysis riskAnalysis) {
        return new RiskAnalysisResponse(
                riskAnalysis.getId(),
                riskAnalysis.getPropertyId(),
                riskAnalysis.getRegistrySnapshotId(),
                riskAnalysis.getAnalysisVersion(),
                riskAnalysis.getPolicyVersion(),
                riskAnalysis.getRiskLevel(),
                riskAnalysis.getRiskScore(),
                riskAnalysis.getAnalyzedAt(),
                riskAnalysis.getCreatedAt()
        );
    }
}