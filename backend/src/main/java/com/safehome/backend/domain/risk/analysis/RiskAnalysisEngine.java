package com.safehome.backend.domain.risk.analysis;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class RiskAnalysisEngine {

    private final List<RiskRule> rules;

    public RiskAnalysisEngine(List<RiskRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public List<RiskRuleResult> analyze(RiskAnalysisContext context) {
        List<RiskRuleResult> results = new ArrayList<>();

        for (RiskRule rule : rules) {
            List<RiskRuleResult> ruleResults = rule.evaluate(context);

            if (ruleResults != null) {
                results.addAll(ruleResults);
            }
        }

        return results;
    }
}