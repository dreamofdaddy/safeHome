package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MortgageRiskRuleTest {

    private final MortgageRiskRule rule = new MortgageRiskRule();

    @Test
    void activeMortgage_createsRiskFactor() {
        RegistryRight right = mock(RegistryRight.class);

        when(right.getRightType()).thenReturn("MORTGAGE");
        when(right.getStatus()).thenReturn("ACTIVE");
        when(right.getAmount()).thenReturn(new BigDecimal("300000000"));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of()
        );

        List<RiskRuleResult> results = rule.evaluate(context);

        assertEquals(1, results.size());

        RiskRuleResult result = results.get(0);

        assertEquals("MORTGAGE", result.getFactorType());
        assertEquals("HIGH", result.getSeverity());
        assertEquals("300000000", result.getValue());
        assertEquals(
                "활성 근저당권이 존재합니다.",
                result.getDescription()
        );
    }

    @Test
    void inactiveMortgage_doesNotCreateRiskFactor() {
        RegistryRight right = mock(RegistryRight.class);

        when(right.getRightType()).thenReturn("MORTGAGE");
        when(right.getStatus()).thenReturn("INACTIVE");
        when(right.getAmount()).thenReturn(new BigDecimal("300000000"));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of()
        );

        List<RiskRuleResult> results = rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void nonMortgageRight_doesNotCreateRiskFactor() {
        RegistryRight right = mock(RegistryRight.class);

        when(right.getRightType()).thenReturn("LEASEHOLD");
        when(right.getStatus()).thenReturn("ACTIVE");
        when(right.getAmount()).thenReturn(new BigDecimal("200000000"));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of()
        );

        List<RiskRuleResult> results = rule.evaluate(context);

        assertTrue(results.isEmpty());
    }
}