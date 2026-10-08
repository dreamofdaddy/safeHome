package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import com.safehome.backend.domain.user.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CoOwnerIssueRiskRuleTest {

    private final CoOwnerIssueRiskRule rule =
            new CoOwnerIssueRiskRule();

    @Test
    void coOwnedPropertyWithOneLandlordProducesRisk() {

        User owner1 = mock(User.class);
        when(owner1.getId())
                .thenReturn(1L);

        User owner2 = mock(User.class);
        when(owner2.getId())
                .thenReturn(2L);

        PropertyOwner propertyOwner1 = mock(PropertyOwner.class);
        when(propertyOwner1.getUser())
                .thenReturn(owner1);
        when(propertyOwner1.getOwnershipRatio())
                .thenReturn(new BigDecimal("50.00"));

        PropertyOwner propertyOwner2 = mock(PropertyOwner.class);
        when(propertyOwner2.getUser())
                .thenReturn(owner2);
        when(propertyOwner2.getOwnershipRatio())
                .thenReturn(new BigDecimal("50.00"));

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(1L);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(),
                List.of(propertyOwner1, propertyOwner2),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertEquals(1, results.size());

        RiskRuleResult result = results.get(0);

        assertEquals(
                CommonCodes.RISK_FACTOR_TYPE_CO_OWNER_ISSUE,
                result.getFactorType()
        );
        assertEquals(
                CommonCodes.RISK_SEVERITY_HIGH,
                result.getSeverity()
        );
        assertEquals(
                "100.00",
                result.getValue()
        );
        assertTrue(
                result.getDescription()
                        .contains("공동소유 부동산")
        );
    }

    @Test
    void singleOwnerPropertyDoesNotProduceRisk() {

        User owner = mock(User.class);
        when(owner.getId())
                .thenReturn(1L);

        PropertyOwner propertyOwner = mock(PropertyOwner.class);
        when(propertyOwner.getUser())
                .thenReturn(owner);
        when(propertyOwner.getOwnershipRatio())
                .thenReturn(new BigDecimal("100.00"));

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(1L);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(),
                List.of(propertyOwner),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void landlordWhoIsNotOwnerDoesNotProduceCoOwnerRisk() {

        User owner1 = mock(User.class);
        when(owner1.getId())
                .thenReturn(1L);

        User owner2 = mock(User.class);
        when(owner2.getId())
                .thenReturn(2L);

        PropertyOwner propertyOwner1 = mock(PropertyOwner.class);
        when(propertyOwner1.getUser())
                .thenReturn(owner1);
        when(propertyOwner1.getOwnershipRatio())
                .thenReturn(new BigDecimal("50.00"));

        PropertyOwner propertyOwner2 = mock(PropertyOwner.class);
        when(propertyOwner2.getUser())
                .thenReturn(owner2);
        when(propertyOwner2.getOwnershipRatio())
                .thenReturn(new BigDecimal("50.00"));

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(3L);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(),
                List.of(propertyOwner1, propertyOwner2),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void noContractDoesNotProduceRisk() {

        User owner1 = mock(User.class);
        when(owner1.getId())
                .thenReturn(1L);

        User owner2 = mock(User.class);
        when(owner2.getId())
                .thenReturn(2L);

        PropertyOwner propertyOwner1 = mock(PropertyOwner.class);
        when(propertyOwner1.getUser())
                .thenReturn(owner1);
        when(propertyOwner1.getOwnershipRatio())
                .thenReturn(new BigDecimal("50.00"));

        PropertyOwner propertyOwner2 = mock(PropertyOwner.class);
        when(propertyOwner2.getUser())
                .thenReturn(owner2);
        when(propertyOwner2.getOwnershipRatio())
                .thenReturn(new BigDecimal("50.00"));

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(),
                List.of(propertyOwner1, propertyOwner2),
                null,
                null
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void nullLandlordUserIdDoesNotProduceRisk() {

        PropertyOwner propertyOwner1 = mock(PropertyOwner.class);
        PropertyOwner propertyOwner2 = mock(PropertyOwner.class);

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(null);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(),
                List.of(propertyOwner1, propertyOwner2),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }
}