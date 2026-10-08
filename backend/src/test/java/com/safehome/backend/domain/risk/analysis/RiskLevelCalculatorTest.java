package com.safehome.backend.domain.risk.analysis;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.risk.policy.RiskLevelPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskLevelCalculatorTest {

    @Mock
    private RiskLevelPolicyService policyService;

    private RiskLevelCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskLevelCalculator(policyService);
    }

    @Test
    void shouldReturnLow() {
        when(policyService.getRiskLevel(
                new BigDecimal("10.00")
        )).thenReturn("LOW");

        String riskLevel =
                calculator.calculate(new BigDecimal("10.00"));

        assertThat(riskLevel)
                .isEqualTo(CommonCodes.RISK_LEVEL_LOW);
    }

    @Test
    void shouldReturnMedium() {
        when(policyService.getRiskLevel(
                new BigDecimal("30.00")
        )).thenReturn("MEDIUM");

        String riskLevel =
                calculator.calculate(new BigDecimal("30.00"));

        assertThat(riskLevel)
                .isEqualTo(CommonCodes.RISK_LEVEL_MEDIUM);
    }

    @Test
    void shouldReturnHigh() {
        when(policyService.getRiskLevel(
                new BigDecimal("50.00")
        )).thenReturn("HIGH");

        String riskLevel =
                calculator.calculate(new BigDecimal("50.00"));

        assertThat(riskLevel)
                .isEqualTo(CommonCodes.RISK_LEVEL_HIGH);
    }

    @Test
    void shouldReturnCritical() {
        when(policyService.getRiskLevel(
                new BigDecimal("80.00")
        )).thenReturn("CRITICAL");

        String riskLevel =
                calculator.calculate(new BigDecimal("80.00"));

        assertThat(riskLevel)
                .isEqualTo(CommonCodes.RISK_LEVEL_CRITICAL);
    }
}