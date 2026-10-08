package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRule;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MortgageRiskRule implements RiskRule {

    @Override
    public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {
        return context.getRegistryRights()
                .stream()
                .filter(this::isActiveMortgage)
                .map(this::createResult)
                .toList();
    }

    private boolean isActiveMortgage(RegistryRight right) {
        return CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE.equals(right.getRightType())
                && CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE.equals(right.getStatus());
    }

    private RiskRuleResult createResult(RegistryRight right) {
        String value = right.getAmount() == null
                ? null
                : right.getAmount().toPlainString();

        return RiskRuleResult.of(
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH,
                value,
                "활성 근저당권이 존재합니다."
        );
    }
}