package com.safehome.backend.domain.risk.dto;

import com.safehome.backend.domain.risk.comparison.RiskFactorChangeType;

public record RiskFactorComparisonResponse(
        RiskFactorChangeType changeType,
        RiskFactorResponse previous,
        RiskFactorResponse current
) {
}