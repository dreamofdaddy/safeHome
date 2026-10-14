package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRule;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

@Component
public class CoOwnerIssueRiskRule implements RiskRule {

    @Override
    public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {

        Contract contract = context.getContract();

        // 현재 계약이 없으면 공동소유 관련 계약 상태를 판단할 수 없음
        if (contract == null) {
            return List.of();
        }

        List<PropertyOwner> propertyOwners =
                context.getPropertyOwners();

        // 단독 소유라면 공동소유 이슈 없음
        if (propertyOwners.size() <= 1) {
            return List.of();
        }

        BigDecimal totalOwnershipRatio = propertyOwners.stream()
                .map(PropertyOwner::getOwnershipRatio)
                .filter(ratio -> ratio != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        /*
         * 현재 계약 모델은 공동소유자 전원의 동의 여부를 표현하지 못한다.
         * 계약상 임대인 User ID로 등기상 소유자를 판정하지 않는다.
         *
         * ownershipRatio 합계가 100인지는 별도 데이터 품질 검증 대상이며,
         * 여기서는 CO_OWNER_ISSUE 판단과 분리한다.
         */
        return List.of(
                RiskRuleResult.of(
                        CommonCodes.RISK_FACTOR_TYPE_CO_OWNER_ISSUE,
                        CommonCodes.RISK_SEVERITY_HIGH,
                        totalOwnershipRatio.toPlainString(),
                        "공동소유 부동산이며 계약상 임대인이 공동소유자 중 일부만 확인됩니다."
                )
        );
    }
}
