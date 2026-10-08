package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRule;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class PriorityRightRiskRule implements RiskRule {

    @Override
    public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {

        Contract contract = context.getContract();

        // 현재 계약이 없으면 선순위 권리를 판단할 수 없음
        if (contract == null) {
            return List.of();
        }

        LocalDateTime contractReferenceDate =
                resolveContractReferenceDate(contract);

        return context.getRegistryRights()
                .stream()
                .filter(this::isActive)
                .filter(this::isPriorityRight)
                .filter(right ->
                        isRegisteredBeforeContract(
                                right,
                                contractReferenceDate
                        )
                )
                .map(this::createResult)
                .toList();
    }

    private boolean isActive(RegistryRight right) {
        return CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE.equals(
                right.getStatus()
        );
    }

    private boolean isPriorityRight(RegistryRight right) {
        /*
         * 현재 MVP에서는 LEASEHOLD를
         * 별도의 선순위 권리 대상으로 판단한다.
         *
         * MORTGAGE는 MortgageRiskRule에서 별도로 처리하므로
         * 여기서는 중복 위험을 발생시키지 않는다.
         */
        return CommonCodes.REGISTRY_RIGHT_TYPE_LEASEHOLD.equals(
                right.getRightType()
        );
    }

    private boolean isRegisteredBeforeContract(
            RegistryRight right,
            LocalDateTime contractReferenceDate
    ) {
        if (right.getRegisteredAt() == null) {
            return false;
        }

        return right.getRegisteredAt()
                .isBefore(contractReferenceDate);
    }

    private LocalDateTime resolveContractReferenceDate(
            Contract contract
    ) {
        // 실제 계약 체결 시각이 있으면 우선 사용
        if (contract.getSignedAt() != null) {
            return contract.getSignedAt();
        }

        // signedAt이 없으면 계약 시작일 00:00을 사용
        return contract.getStartDate().atStartOfDay();
    }

    private RiskRuleResult createResult(RegistryRight right) {

        String value = right.getPriority() == null
                ? null
                : right.getPriority().toString();

        String description =
                "현재 계약보다 먼저 등기된 선순위 권리가 존재합니다. " +
                "권리자: " + right.getHolderName();

        return RiskRuleResult.of(
                CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT,
                CommonCodes.RISK_SEVERITY_HIGH,
                value,
                description
        );
    }
}