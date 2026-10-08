package com.safehome.backend.domain.risk.analysis;

import com.safehome.backend.domain.risk.policy.RiskLevelPolicyService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class RiskLevelCalculator {

    private final RiskLevelPolicyService policyService;

    public RiskLevelCalculator(
            RiskLevelPolicyService policyService
    ) {
        this.policyService = policyService;
    }

    public String calculate(BigDecimal totalScore) {
        return policyService.getRiskLevel(totalScore);
    }
}