package com.safehome.backend.domain.risk.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record SafetyAnalysisResponse(
        Long id,
        Long propertyId,
        Long registrySnapshotId,
        String analysisVersion,
        String policyVersion,
        String riskLevel,
        BigDecimal riskScore,
        LocalDateTime analyzedAt,
        List<RiskFactorResponse> factors
) {
}