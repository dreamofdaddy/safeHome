package com.safehome.backend.domain.risk.policy;

import jakarta.persistence.*;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import java.sql.Types;

@Entity
@Table(name = "risk_policy_versions")
public class RiskPolicyVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "policy_version", nullable = false, unique = true, length = 50)
    private String policyVersion;

    @Column(name = "policy_name", nullable = false, length = 100)
    private String policyName;

    @Column(name = "description")
    private String description;

    @JdbcTypeCode(Types.CHAR)
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected RiskPolicyVersion() {
    }

    public Long getId() {
        return id;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public String getPolicyName() {
        return policyName;
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