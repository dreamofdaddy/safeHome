package com.safehome.backend.domain.risk.policy;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

@Entity
@Table(name = "risk_level_policies")
public class RiskLevelPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_version", nullable = false, length = 50)
    private String policyVersion;

    @Column(name = "risk_level", nullable = false, length = 50)
    private String riskLevel;

    @Column(name = "min_score", nullable = false, precision = 10, scale = 2)
    private BigDecimal minScore;

    @Column(name = "max_score", precision = 10, scale = 2)
    private BigDecimal maxScore;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RiskLevelPolicy() {
    }

    public Long getId() {
        return id;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public String getRiskLevel() {
        return riskLevel;
    }

    public BigDecimal getMinScore() {
        return minScore;
    }

    public BigDecimal getMaxScore() {
        return maxScore;
    }

    public Integer getSortOrder() {
        return sortOrder;
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