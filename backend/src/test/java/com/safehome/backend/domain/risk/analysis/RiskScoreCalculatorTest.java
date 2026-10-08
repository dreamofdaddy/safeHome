package com.safehome.backend.domain.risk.analysis;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.risk.policy.RiskFactorPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskScoreCalculatorTest {

    @Mock
    private RiskFactorPolicyService policyService;

    private RiskScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RiskScoreCalculator(policyService);
    }

    @Test
    void shouldCalculateScoreUsingPolicy() {
        RiskRuleResult result = RiskRuleResult.of(
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH,
                "300000000",
                "활성 근저당권이 존재합니다."
        );

        when(policyService.getScore(
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH
        )).thenReturn(new BigDecimal("30.00"));

        RiskScoreCalculationResult calculation =
                calculator.calculate(List.of(result));

        assertThat(calculation.getTotalScore())
                .isEqualByComparingTo("30.00");

        assertThat(calculation.getFactors())
                .hasSize(1);

        assertThat(calculation.getFactors().get(0).getScore())
                .isEqualByComparingTo("30.00");

        assertThat(calculation.getFactors().get(0).getFactorType())
                .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_MORTGAGE);
    }

    @Test
    void shouldSumMultipleRiskFactors() {
        RiskRuleResult mortgage = RiskRuleResult.of(
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH,
                "300000000",
                "활성 근저당권이 존재합니다."
        );

        RiskRuleResult priorityRight = RiskRuleResult.of(
                CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT,
                CommonCodes.RISK_SEVERITY_HIGH,
                "1",
                "우선순위 권리가 존재합니다."
        );

        when(policyService.getScore(
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH
        )).thenReturn(new BigDecimal("30.00"));

        when(policyService.getScore(
                "PRIORITY_RIGHT",
                CommonCodes.RISK_SEVERITY_HIGH
        )).thenReturn(new BigDecimal("20.00"));

        RiskScoreCalculationResult calculation =
                calculator.calculate(
                        List.of(mortgage, priorityRight)
                );

        assertThat(calculation.getTotalScore())
                .isEqualByComparingTo("50.00");

        assertThat(calculation.getFactors())
                .hasSize(2);
    }

    @Test
    void shouldReturnZeroWhenNoRiskFactorsExist() {
        RiskScoreCalculationResult calculation =
                calculator.calculate(List.of());

        assertThat(calculation.getTotalScore())
                .isEqualByComparingTo("0");

        assertThat(calculation.getFactors())
                .isEmpty();
    }
}