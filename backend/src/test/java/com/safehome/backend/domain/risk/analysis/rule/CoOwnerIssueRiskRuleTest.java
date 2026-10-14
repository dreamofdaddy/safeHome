package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoOwnerIssueRiskRuleTest {
    private final CoOwnerIssueRiskRule rule = new CoOwnerIssueRiskRule();

    @Test
    void flagsUnverifiedConsentForAnyContractOnCoOwnedProperty() {
        PropertyOwner first = mock(PropertyOwner.class);
        PropertyOwner second = mock(PropertyOwner.class);
        when(first.getOwnershipRatio()).thenReturn(new BigDecimal("50.00"));
        when(second.getOwnershipRatio()).thenReturn(new BigDecimal("50.00"));
        Contract contract = mock(Contract.class);

        var results = rule.evaluate(new RiskAnalysisContext(
                5L, 2L, List.of(), List.of(first, second), null, contract));

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getFactorType())
                .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_CO_OWNER_ISSUE);
        assertThat(results.get(0).getValue()).isEqualTo("100.00");
    }

    @Test
    void doesNotFlagSingleOwnerOrMissingContract() {
        PropertyOwner owner = mock(PropertyOwner.class);
        Contract contract = mock(Contract.class);
        assertThat(rule.evaluate(new RiskAnalysisContext(
                5L, 2L, List.of(), List.of(owner), null, contract))).isEmpty();
        assertThat(rule.evaluate(new RiskAnalysisContext(
                5L, 2L, List.of(), List.of(owner, owner), null, null))).isEmpty();
    }
}
