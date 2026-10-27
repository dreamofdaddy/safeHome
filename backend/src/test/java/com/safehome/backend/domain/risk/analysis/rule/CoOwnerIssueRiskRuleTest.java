package com.safehome.backend.domain.risk.analysis.rule;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.contract.Contract;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.math.BigDecimal;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
class CoOwnerIssueRiskRuleTest {
  private final CoOwnerIssueRiskRule rule=new CoOwnerIssueRiskRule();
  @Test void deferredEvenWithMultipleOwnersAndContract(){
    var owner=mock(PropertyOwner.class);when(owner.getOwnershipRatio()).thenReturn(new BigDecimal("50"));
    assertThat(rule.evaluate(new RiskAnalysisContext(1L,1L,List.of(),List.of(owner,owner),null,mock(Contract.class)))).isEmpty();
  }
  @Test void deferredEvenWithIncompleteSingleOwnerShare(){
    var owner=mock(PropertyOwner.class);when(owner.getOwnershipRatio()).thenReturn(BigDecimal.ONE);
    assertThat(rule.evaluate(new RiskAnalysisContext(1L,1L,List.of(),List.of(owner),null,null))).isEmpty();
  }
}
