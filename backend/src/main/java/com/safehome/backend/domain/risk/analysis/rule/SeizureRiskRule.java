package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRule;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SeizureRiskRule implements RiskRule {

    @Override
    public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {
        return context.getRegistryRights()
                .stream()
                .filter(this::isActiveSeizure)
                .map(this::createResult)
                .toList();
    }

    private boolean isActiveSeizure(RegistryRight right) {

        if (!CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE.equals(
                right.getStatus())) {
            return false;
        }

        return CommonCodes.REGISTRY_RIGHT_TYPE_SEIZURE.equals(
                right.getRightType()
        ) || CommonCodes.REGISTRY_RIGHT_TYPE_PROVISIONAL_SEIZURE.equals(
                right.getRightType()
        );
    }

    private RiskRuleResult createResult(RegistryRight right) {

        return RiskRuleResult.of(
                CommonCodes.RISK_FACTOR_TYPE_SEIZURE,
                CommonCodes.RISK_SEVERITY_CRITICAL,
                right.getRightType(),
                createDescription(right)
        );
    }

    private String createDescription(RegistryRight right) {

        if (CommonCodes.REGISTRY_RIGHT_TYPE_PROVISIONAL_SEIZURE.equals(
                right.getRightType())) {

            return "활성 가압류가 존재합니다. " +
                   "권리자: " + right.getHolderName();
        }

        return "활성 압류가 존재합니다. " +
               "권리자: " + right.getHolderName();
    }
}