package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.domain.risk.analysis.*;
import java.util.List;
import org.springframework.stereotype.Component;

/** Deferred: investigate current-vs-transferred ownership history before enabling this assessment. */
@Component
public class CoOwnerIssueRiskRule implements RiskRule {
    @Override public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {
        return List.of();
    }
}
