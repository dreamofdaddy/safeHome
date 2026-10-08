package com.safehome.backend.domain.risk.policy;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;

import java.sql.Types;

@Entity
@Table(name = "risk_factor_policies")
public class RiskFactorPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "factor_type", nullable = false, length = 50)
    private String factorType;

    @Column(name = "severity", nullable = false, length = 50)
    private String severity;

    @Column(name = "score", nullable = false, precision = 10, scale = 2)
    private BigDecimal score;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RiskFactorPolicy() {
    }

    public Long getId() {
        return id;
    }

    public String getPolicyVersion() {
        return policyVersion;
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

    public String getDescription() {
        return description;
    }

    public String getUseYn() {
        return useYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}