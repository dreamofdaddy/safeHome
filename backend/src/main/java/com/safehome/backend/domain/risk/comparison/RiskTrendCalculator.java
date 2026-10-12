package com.safehome.backend.domain.risk.comparison;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RiskTrendCalculator {

    public BigDecimal calculateScoreDelta(
            BigDecimal previousScore,
            BigDecimal currentScore
    ) {
        BigDecimal previous =
                previousScore == null
                        ? BigDecimal.ZERO
                        : previousScore;

        BigDecimal current =
                currentScore == null
                        ? BigDecimal.ZERO
                        : currentScore;

        return current.subtract(previous);
    }

    public RiskTrend calculateTrend(
            BigDecimal previousScore,
            BigDecimal currentScore
    ) {
        BigDecimal previous =
                previousScore == null
                        ? BigDecimal.ZERO
                        : previousScore;

        BigDecimal current =
                currentScore == null
                        ? BigDecimal.ZERO
                        : currentScore;

        int comparison =
                current.compareTo(previous);

        if (comparison < 0) {
            return RiskTrend.IMPROVED;
        }

        if (comparison > 0) {
            return RiskTrend.WORSENED;
        }

        return RiskTrend.UNCHANGED;
    }
}