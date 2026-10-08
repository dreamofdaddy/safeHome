package com.safehome.backend.domain.risk.analysis.rule;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.risk.analysis.RiskAnalysisContext;
import com.safehome.backend.domain.risk.analysis.RiskRuleResult;
import com.safehome.backend.domain.user.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OwnershipMismatchRiskRuleTest {

    private final OwnershipMismatchRiskRule rule =
            new OwnershipMismatchRiskRule();

    @Test
    void landlordWhoIsPropertyOwnerDoesNotProduceRisk() {

        Long landlordUserId = 1L;

        User ownerUser = mock(User.class);
        when(ownerUser.getId())
                .thenReturn(landlordUserId);

        PropertyOwner propertyOwner = mock(PropertyOwner.class);
        when(propertyOwner.getUser())
                .thenReturn(ownerUser);

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(landlordUserId);

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
    void landlordWhoIsNotPropertyOwnerProducesRisk() {

        Long landlordUserId = 3L;

        User ownerUser = mock(User.class);
        when(ownerUser.getId())
                .thenReturn(1L);

        PropertyOwner propertyOwner = mock(PropertyOwner.class);
        when(propertyOwner.getUser())
                .thenReturn(ownerUser);

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(landlordUserId);

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

        assertEquals(1, results.size());

        RiskRuleResult result = results.get(0);

        assertEquals(
                CommonCodes.RISK_FACTOR_TYPE_OWNERSHIP_MISMATCH,
                result.getFactorType()
        );
        assertEquals(
                CommonCodes.RISK_SEVERITY_HIGH,
                result.getSeverity()
        );
        assertEquals(
                "3",
                result.getValue()
        );
        assertTrue(
                result.getDescription()
                        .contains("임대인이 등기상 소유자와 일치하지 않습니다")
        );
    }

    @Test
    void landlordWhoIsOneOfCoOwnersDoesNotProduceRisk() {

        Long landlordUserId = 2L;

        User owner1 = mock(User.class);
        when(owner1.getId())
                .thenReturn(1L);

        User owner2 = mock(User.class);
        when(owner2.getId())
                .thenReturn(landlordUserId);

        PropertyOwner propertyOwner1 = mock(PropertyOwner.class);
        when(propertyOwner1.getUser())
                .thenReturn(owner1);

        PropertyOwner propertyOwner2 = mock(PropertyOwner.class);
        when(propertyOwner2.getUser())
                .thenReturn(owner2);

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(landlordUserId);

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

        User ownerUser = mock(User.class);
        when(ownerUser.getId())
                .thenReturn(1L);

        PropertyOwner propertyOwner = mock(PropertyOwner.class);
        when(propertyOwner.getUser())
                .thenReturn(ownerUser);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(),
                List.of(propertyOwner),
                null,
                null
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }

    @Test
    void nullLandlordUserIdDoesNotProduceRisk() {

        Contract contract = mock(Contract.class);
        when(contract.getLandlordUserId())
                .thenReturn(null);

        RiskAnalysisContext context = new RiskAnalysisContext(
                5L,
                2L,
                List.of(),
                List.of(),
                null,
                contract
        );

        List<RiskRuleResult> results =
                rule.evaluate(context);

        assertTrue(results.isEmpty());
    }
}