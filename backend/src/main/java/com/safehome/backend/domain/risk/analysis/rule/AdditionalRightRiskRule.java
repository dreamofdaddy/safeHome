package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.*;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class AdditionalRightRiskRule implements RiskRule {
    @Override public List<RiskRuleResult> evaluate(RiskAnalysisContext context) {
        return context.getRegistryRights().stream()
            .filter(r -> "ACTIVE".equals(r.getStatus()))
            .filter(r -> Set.of("TRUST","AUCTION","OTHER").contains(r.getRightType()))
            .map(this::result).toList();
    }
    private RiskRuleResult result(RegistryRight right) {
        String type = right.getRightType();
        String source = right.getSourceName();
        // Older/uncertain extraction may retain OTHER; explicit source labels still trigger the severe rule.
        if ("OTHER".equals(type) && source != null) {
            if (source.contains("신탁")) type = "TRUST";
            else if (source.contains("경매")) type = "AUCTION";
        }
        return switch (type) {
            case "TRUST" -> RiskRuleResult.of("TRUST","CRITICAL","TRUST",
                "현재 유효한 신탁 등기가 분석되었습니다. 신탁 관련 계약 권한과 조건을 확인해야 합니다.");
            case "AUCTION" -> RiskRuleResult.of("AUCTION","CRITICAL","AUCTION",
                "현재 유효한 경매 관련 등기가 분석되었습니다. 경매 진행 상태를 확인해야 합니다.");
            default -> RiskRuleResult.of("OTHER","HIGH", source == null || source.isBlank() ? "원문 권리명 미확인" : source,
                "현재 유효한 기타 등기 권리가 분석되었습니다. 원문 권리명과 내용을 확인해야 합니다.");
        };
    }
}
