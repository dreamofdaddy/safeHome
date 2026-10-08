package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SeizureRiskRuleTest {

    private final SeizureRiskRule rule =
            new SeizureRiskRule();

    @Test
    void activeSeizureProducesRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_SEIZURE);
        when(right.getHolderName())
                .thenReturn("Test Court");

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of()
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertEquals(1, results.size());

        RiskRuleResult result = results.get(0);

        assertEquals(
                CommonCodes.RISK_FACTOR_TYPE_SEIZURE,
                result.getFactorType()
        );
        assertEquals(
                CommonCodes.RISK_SEVERITY_CRITICAL,
                result.getSeverity()
        );
        assertEquals(
                CommonCodes.REGISTRY_RIGHT_TYPE_SEIZURE,
                result.getValue()
        );
        assertTrue(
                result.getDescription()
                        .contains("활성 압류")
        );
    }

    @Test
    void activeProvisionalSeizureProducesRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE);
        when(right.getRightType())
                .thenReturn(
                        CommonCodes.REGISTRY_RIGHT_TYPE_PROVISIONAL_SEIZURE
                );
        when(right.getHolderName())
                .thenReturn("Test Court");

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of()
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertEquals(1, results.size());

        RiskRuleResult result = results.get(0);

        assertEquals(
                CommonCodes.RISK_FACTOR_TYPE_SEIZURE,
                result.getFactorType()
        );
        assertEquals(
                CommonCodes.RISK_SEVERITY_CRITICAL,
                result.getSeverity()
        );
        assertEquals(
                CommonCodes.REGISTRY_RIGHT_TYPE_PROVISIONAL_SEIZURE,
                result.getValue()
        );
        assertTrue(
                result.getDescription()
                        .contains("활성 가압류")
        );
    }

    @Test
    void inactiveSeizureDoesNotProduceRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_SEIZURE);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of()
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void mortgageDoesNotProduceSeizureRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of()
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }
}