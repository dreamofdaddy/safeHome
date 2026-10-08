package com.safehome.backend.domain.risk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_factors")
public class RiskFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "risk_analysis_id", nullable = false)
    private Long riskAnalysisId;

    @Column(name = "factor_type", nullable = false, length = 50)
    private String factorType;

    @Column(name = "severity", nullable = false, length = 30)
    private String severity;

    @Column(name = "score", precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "value", length = 500)
    private String value;

    @Column(name = "description")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RiskFactor() {
    }

    public RiskFactor(
            Long riskAnalysisId,
            String factorType,
            String severity,
            BigDecimal score,
            String value,
            String description
    ) {
        this.riskAnalysisId = riskAnalysisId;
        this.factorType = factorType;
        this.severity = severity;
        this.score = score;
        this.value = value;
        this.description = description;
    }

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Long getRiskAnalysisId() {
        return riskAnalysisId;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(
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
}