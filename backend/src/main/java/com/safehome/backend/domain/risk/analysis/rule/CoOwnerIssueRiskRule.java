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

        Long landlordUserId = contract.getLandlordUserId();

        // 임대인이 없는 계약은 OwnershipMismatch 등 다른 Rule에서 판단
        if (landlordUserId == null) {
            return List.of();
        }

        boolean landlordIsOwner = propertyOwners.stream()
                .map(PropertyOwner::getUser)
                .map(user -> user.getId())
                .anyMatch(landlordUserId::equals);

        // 임대인이 소유자도 아니면 OwnershipMismatch의 책임
        if (!landlordIsOwner) {
            return List.of();
        }

        BigDecimal totalOwnershipRatio = propertyOwners.stream()
                .map(PropertyOwner::getOwnershipRatio)
                .filter(ratio -> ratio != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        /*
         * 공동소유인데 현재 계약에는 임대인 1명만 존재한다.
         * 따라서 다른 공동소유자의 계약 관여/동의 여부를
         * 현재 데이터 모델로 확인할 수 없으므로 경고를 발생시킨다.
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