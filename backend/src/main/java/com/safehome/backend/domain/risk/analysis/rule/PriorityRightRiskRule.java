package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.*;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class PriorityRightRiskRule implements RiskRule {
    @Override public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {
        return context.getRegistryRights().stream()
            .filter(r->CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE.equals(r.getStatus()))
            .filter(r->CommonCodes.REGISTRY_RIGHT_TYPE_LEASEHOLD.equals(r.getRightType()))
            .map(r->result(r,context.getContract())).toList();
    }
    private RiskRuleResult result(RegistryRight right, Contract contract) {
        LocalDateTime reference=contract==null?null:contract.getSignedAt()!=null?contract.getSignedAt()
            :contract.getStartDate()==null?null:contract.getStartDate().atStartOfDay();
        String priority=reference==null || right.getRegisteredAt()==null
            ?"계약 기준일 또는 권리 등록일 정보가 없어 계약과의 선후관계는 확인되지 않았습니다."
            :right.getRegisteredAt().isBefore(reference)
                ?"현재 계약보다 먼저 등기된 선순위 권리가 존재합니다."
                :"현재 계약 기준일보다 먼저 등기된 것으로 확인된 권리는 아니지만 존속 중인 전세권의 정리가 필요합니다.";
        String amount=right.getAmount()==null?"":" 전세금: "+right.getAmount().toPlainString()+"원.";
        String date=right.getRegisteredAt()==null?"":" 등기일: "+right.getRegisteredAt().toLocalDate()+".";
        return RiskRuleResult.of(CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT,CommonCodes.RISK_SEVERITY_HIGH,
            right.getPriority()==null?null:right.getPriority().toString(),
            "유효한 전세권이 등기되어 있습니다."+amount+date+" "+priority+
            " 전세권 등기만으로 실제 거주 임차인이나 전입·확정일자에 따른 우선순위를 확정할 수 없습니다.");
    }
}
