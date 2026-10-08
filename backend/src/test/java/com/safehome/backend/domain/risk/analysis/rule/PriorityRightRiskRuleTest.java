package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PriorityRightRiskRuleTest {

    private final PriorityRightRiskRule rule =
            new PriorityRightRiskRule();

    @Test
    void activeLeaseholdRegisteredBeforeContractProducesRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_LEASEHOLD);
        when(right.getRegisteredAt())
                .thenReturn(LocalDateTime.of(2026, 9, 20, 9, 0));
        when(right.getPriority())
                .thenReturn(2);
        when(right.getHolderName())
                .thenReturn("Test Tenant");

        Contract contract = mock(Contract.class);

        when(contract.getSignedAt())
                .thenReturn(LocalDateTime.of(2026, 10, 1, 10, 0));
        when(contract.getStartDate())
                .thenReturn(LocalDate.of(2026, 10, 1));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of(),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertEquals(1, results.size());

        RiskRuleResult result = results.get(0);

        assertEquals(
                CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT,
                result.getFactorType()
        );
        assertEquals(
                CommonCodes.RISK_SEVERITY_HIGH,
                result.getSeverity()
        );
        assertEquals(
                "2",
                result.getValue()
        );
        assertTrue(
                result.getDescription()
                        .contains("선순위 권리가 존재합니다")
        );
    }

    @Test
    void leaseholdRegisteredAfterContractDoesNotProduceRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_LEASEHOLD);
        when(right.getRegisteredAt())
                .thenReturn(LocalDateTime.of(2026, 10, 5, 9, 0));

        Contract contract = mock(Contract.class);

        when(contract.getSignedAt())
                .thenReturn(LocalDateTime.of(2026, 10, 1, 10, 0));
        when(contract.getStartDate())
                .thenReturn(LocalDate.of(2026, 10, 1));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of(),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void inactiveLeaseholdDoesNotProduceRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_LEASEHOLD);
        when(right.getRegisteredAt())
                .thenReturn(LocalDateTime.of(2026, 9, 20, 9, 0));

        Contract contract = mock(Contract.class);

        when(contract.getSignedAt())
                .thenReturn(LocalDateTime.of(2026, 10, 1, 10, 0));
        when(contract.getStartDate())
                .thenReturn(LocalDate.of(2026, 10, 1));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of(),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void mortgageDoesNotProducePriorityRightRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE);
        when(right.getRegisteredAt())
                .thenReturn(LocalDateTime.of(2026, 9, 1, 9, 0));
        when(right.getPriority())
                .thenReturn(1);
        when(right.getHolderName())
                .thenReturn("Test Bank");
        when(right.getAmount())
                .thenReturn(new BigDecimal("300000000"));

        Contract contract = mock(Contract.class);

        when(contract.getSignedAt())
                .thenReturn(LocalDateTime.of(2026, 10, 1, 10, 0));
        when(contract.getStartDate())
                .thenReturn(LocalDate.of(2026, 10, 1));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of(),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void noContractDoesNotProduceRisk() {

        RegistryRight right = mock(RegistryRight.class);

        when(right.getStatus())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE);
        when(right.getRightType())
                .thenReturn(CommonCodes.REGISTRY_RIGHT_TYPE_LEASEHOLD);
        when(right.getRegisteredAt())
                .thenReturn(LocalDateTime.of(2026, 9, 20, 9, 0));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(right),
                List.of(),
                null,
                null
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }
}