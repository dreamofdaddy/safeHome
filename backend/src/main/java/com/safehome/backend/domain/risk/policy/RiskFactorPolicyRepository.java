package com.safehome.backend.domain.risk.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskFactorPolicyRepository
        extends JpaRepository<RiskFactorPolicy, Long> {

    Optional<RiskFactorPolicy>
    findFirstByPolicyVersionAndFactorTypeAndSeverityAndUseYnOrderByIdDesc(
            String policyVersion,
            String factorType,
            String severity,
            String useYn
    );
}