package com.safehome.backend.domain.risk.analysis;

import com.safehome.backend.domain.risk.policy.RiskFactorPolicyService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class RiskScoreCalculator {

    private final RiskFactorPolicyService policyService;

    public RiskScoreCalculator(
            RiskFactorPolicyService policyService
    ) {
        this.policyService = policyService;
    }

    public RiskScoreCalculationResult calculate(
            List<RiskRuleResult> results
    ) {
        List<ScoredRiskFactor> factors = results.stream()
                .map(result -> score(result))
                .toList();

        BigDecimal totalScore = factors.stream()
                .map(factor -> factor.getScore())
                .reduce(
                        BigDecimal.ZERO,
                        BigDecimal::add
                );

        return new RiskScoreCalculationResult(
                factors,
                totalScore
        );
    }

    private ScoredRiskFactor score(
            RiskRuleResult result
    ) {
        BigDecimal score = policyService.getScore(
                result.getFactorType(),
                result.getSeverity()
        );

        return new ScoredRiskFactor(
                result.getFactorType(),
                result.getSeverity(),
                score,
                result.getValue(),
                result.getDescription()
        );
    }
}