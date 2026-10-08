package com.safehome.backend.domain.risk.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RiskAnalysisUpdateRequest {

    private Long registrySnapshotId;

    @NotBlank
    private String analysisVersion;

    @NotBlank
    private String riskLevel;

    @DecimalMin("0.0")
    @DecimalMax("100.0")
    @Digits(integer = 3, fraction = 2)
    private BigDecimal riskScore;

    @NotNull
    private LocalDateTime analyzedAt;

    public RiskAnalysisUpdateRequest() {
    }

    public Long getRegistrySnapshotId() {
        return registrySnapshotId;
    }

    public void setRegistrySnapshotId(Long registrySnapshotId) {
        this.registrySnapshotId = registrySnapshotId;
    }

    public String getAnalysisVersion() {
        return analysisVersion;
    }

    public void setAnalysisVersion(String analysisVersion) {
        this.analysisVersion = analysisVersion;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public void setRiskLevel(String riskLevel) {
        this.riskLevel = riskLevel;
    }

    public BigDecimal getRiskScore() {
        return riskScore;
    }

    public void setRiskScore(BigDecimal riskScore) {
        this.riskScore = riskScore;
    }

    public LocalDateTime getAnalyzedAt() {
        return analyzedAt;
    }

    public void setAnalyzedAt(LocalDateTime analyzedAt) {
        this.analyzedAt = analyzedAt;
    }
}