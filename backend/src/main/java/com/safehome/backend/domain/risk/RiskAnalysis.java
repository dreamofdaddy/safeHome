package com.safehome.backend.domain.risk;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "risk_analyses")
public class RiskAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "registry_snapshot_id")
    private Long registrySnapshotId;

    @Column(name = "analysis_version", nullable = false, length = 50)
    private String analysisVersion;

    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    @Column(name = "risk_level", nullable = false, length = 30)
    private String riskLevel;

    @Column(name = "risk_score", precision = 5, scale = 2)
    private BigDecimal riskScore;

    @Column(name = "analyzed_at", nullable = false)
    private LocalDateTime analyzedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected RiskAnalysis() {
    }

    public RiskAnalysis(
            Long propertyId,
            Long registrySnapshotId,
            String analysisVersion,
            String policyVersion,
            String riskLevel,
            BigDecimal riskScore,
            LocalDateTime analyzedAt
    ) {
        this.propertyId = propertyId;
        this.registrySnapshotId = registrySnapshotId;
        this.analysisVersion = analysisVersion;
        this.policyVersion = policyVersion;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.analyzedAt = analyzedAt;
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

    public Long getPropertyId() {
        return propertyId;
    }

    public Long getRegistrySnapshotId() {
        return registrySnapshotId;
    }

    public String getAnalysisVersion() {
        return analysisVersion;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void update(
            Long registrySnapshotId,
            String analysisVersion,
            String riskLevel,
            BigDecimal riskScore,
            LocalDateTime analyzedAt
    ) {
        this.registrySnapshotId = registrySnapshotId;
        this.analysisVersion = analysisVersion;
        this.riskLevel = riskLevel;
        this.riskScore = riskScore;
        this.analyzedAt = analyzedAt;
    }
}