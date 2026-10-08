package com.safehome.backend.domain.risk.analysis;

public class RiskRuleResult {

    private final String factorType;
    private final String severity;
    private final String value;
    private final String description;

    private RiskRuleResult(
            String factorType,
            String severity,
            String value,
            String description
    ) {
        this.factorType = factorType;
        this.severity = severity;
        this.value = value;
        this.description = description;
    }

    public static RiskRuleResult of(
            String factorType,
            String severity,
            String value,
            String description
    ) {
        return new RiskRuleResult(
                factorType,
                severity,
                value,
                description
        );
    }

    public String getFactorType() {
        return factorType;
    }

    public String getSeverity() {
        return severity;
    }

    public String getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}