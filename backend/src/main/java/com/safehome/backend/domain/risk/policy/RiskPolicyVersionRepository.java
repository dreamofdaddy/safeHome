package com.safehome.backend.domain.risk.policy;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RiskPolicyVersionRepository
        extends JpaRepository<RiskPolicyVersion, Long> {

    Optional<RiskPolicyVersion> findByPolicyVersion(
            String policyVersion
    );

    Optional<RiskPolicyVersion> findFirstByUseYnOrderByIdDesc(
            String useYn
    );
}