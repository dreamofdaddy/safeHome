package com.safehome.backend.domain.risk.analysis.rule;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class AdditionalRightRiskRuleTest {
    private RegistryRight right(String type,String status,String source) {
        var right=RegistryRight.create(null,type,null,null,1,null,status);
        right.preserveSourceName(source);return right;
    }
    @Test void reportsSevereTrustAuctionAndActiveOtherWithoutScoringInactiveRights() {
        var result=new AdditionalRightRiskRule().evaluate(new RiskAnalysisContext(1L,1L,List.of(
            right("TRUST","ACTIVE","신탁"),right("AUCTION","ACTIVE","강제경매개시결정"),
            right("OTHER","ACTIVE","가처분"),right("OTHER","INACTIVE","가처분"),
            right("TRUST","INACTIVE","신탁"),right("AUCTION","INACTIVE","경매")),List.of()));
        assertThat(result).extracting(r->r.getFactorType()).containsExactly("TRUST","AUCTION","OTHER");
        assertThat(result).extracting(r->r.getSeverity()).containsExactly("CRITICAL","CRITICAL","HIGH");
        assertThat(result.get(2).getValue()).isEqualTo("가처분");
    }
    @Test void preservesConservativeHandlingWhenAnExplicitTrustOrAuctionLabelWasClassifiedOther() {
        var result=new AdditionalRightRiskRule().evaluate(new RiskAnalysisContext(1L,1L,List.of(
            right("OTHER","ACTIVE","신탁"),right("OTHER","ACTIVE","임의경매개시결정"),right("OTHER","ACTIVE",null)),List.of()));
        assertThat(result).extracting(r->r.getFactorType()).containsExactly("TRUST","AUCTION","OTHER");
        assertThat(result.get(2).getValue()).isEqualTo("원문 권리명 미확인");
    }
}
