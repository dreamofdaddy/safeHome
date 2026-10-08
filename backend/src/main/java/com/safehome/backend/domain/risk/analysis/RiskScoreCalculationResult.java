package com.safehome.backend.domain.risk.analysis;

import java.math.BigDecimal;
import java.util.List;

public class RiskScoreCalculationResult {

    private final List<ScoredRiskFactor> factors;
    private final BigDecimal totalScore;

    public RiskScoreCalculationResult(
            List<ScoredRiskFactor> factors,
            BigDecimal totalScore
    ) {
        this.factors = List.copyOf(factors);
        this.totalScore = totalScore;
    }

    public List<ScoredRiskFactor> getFactors() {
        return factors;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }
}