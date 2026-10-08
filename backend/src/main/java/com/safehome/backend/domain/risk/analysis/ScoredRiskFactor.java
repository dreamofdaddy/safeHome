package com.safehome.backend.domain.risk.analysis;

import java.math.BigDecimal;

public class ScoredRiskFactor {

    private final String factorType;
    private final String severity;
    private final BigDecimal score;
    private final String value;
    private final String description;

    public ScoredRiskFactor(
            String factorType,
            String severity,
            BigDecimal score,
            String value,
            String description
    ) {
        this.factorType = factorType;
        this.severity = severity;
        this.score = score;
        this.value = value;
        this.description = description;
    }

    public String getFactorType() {
        return factorType;
    }

    public String getSeverity() {
        return severity;
    }

    public BigDecimal getScore() {
        return score;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}