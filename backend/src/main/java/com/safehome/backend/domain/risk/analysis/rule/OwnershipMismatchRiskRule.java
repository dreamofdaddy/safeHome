package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRule;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class OwnershipMismatchRiskRule implements RiskRule {

    @Override
    public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {

        Contract contract = context.getContract();

        // 현재 계약이 없으면 임대인과 소유자 관계를 판단할 수 없음
        if (contract == null) {
            return List.of();
        }

        Long landlordUserId = contract.getLandlordUserId();

        // 임대인 정보가 없으면 현재 단계에서는 판단하지 않음
        if (landlordUserId == null) {
            return List.of();
        }

        boolean landlordIsOwner = context.getPropertyOwners()
                .stream()
                .map(PropertyOwner::getUser)
                .map(user -> user.getId())
                .anyMatch(landlordUserId::equals);

        if (landlordIsOwner) {
            return List.of();
        }

        return List.of(createResult(landlordUserId));
    }

    private RiskRuleResult createResult(Long landlordUserId) {

        return RiskRuleResult.of(
                CommonCodes.RISK_FACTOR_TYPE_OWNERSHIP_MISMATCH,
                CommonCodes.RISK_SEVERITY_HIGH,
                landlordUserId.toString(),
                "계약상 임대인이 등기상 소유자와 일치하지 않습니다. " +
                "임대인 사용자 ID: " + landlordUserId
        );
    }
}