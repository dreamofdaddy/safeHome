package com.safehome.backend.domain.risk.analysis;

import java.util.List;

public interface RiskRule {

    List<RiskRuleResult> evaluate(RiskAnalysisContext context);

}