package com.safehome.backend.domain.risk.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface RiskLevelPolicyRepository
        extends JpaRepository<RiskLevelPolicy, Long> {

    @Query("""
        SELECT p
        FROM RiskLevelPolicy p
        WHERE p.policyVersion = :policyVersion
          AND p.useYn = 'Y'
          AND p.minScore <= :score
          AND (p.maxScore IS NULL OR p.maxScore >= :score)
        ORDER BY p.minScore DESC
        """)
    List<RiskLevelPolicy> findApplicablePolicies(
            @Param("policyVersion") String policyVersion,
            @Param("score") BigDecimal score
    );

    List<RiskLevelPolicy> findByPolicyVersionAndUseYnOrderBySortOrderAsc(
            String policyVersion,
            String useYn
    );
}