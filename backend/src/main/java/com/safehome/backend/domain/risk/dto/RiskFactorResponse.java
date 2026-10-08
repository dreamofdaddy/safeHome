package com.safehome.backend.domain.risk.dto;

import com.safehome.backend.domain.risk.RiskFactor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record RiskFactorResponse(
        Long id,
        Long riskAnalysisId,
        String factorType,
        String severity,
        BigDecimal score,
        String value,
        String description,
        LocalDateTime createdAt
) {
    public static RiskFactorResponse from(RiskFactor riskFactor) {
        return new RiskFactorResponse(
                riskFactor.getId(),
                riskFactor.getRiskAnalysisId(),
                riskFactor.getFactorType(),
                riskFactor.getSeverity(),
                riskFactor.getScore(),
                riskFactor.getValue(),
                riskFactor.getDescription(),
                riskFactor.getCreatedAt()
        );
    }
}