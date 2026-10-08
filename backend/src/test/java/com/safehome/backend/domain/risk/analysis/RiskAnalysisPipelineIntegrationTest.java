package com.safehome.backend.domain.risk.analysis;

import com.safehome.backend.common.code.CommonCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RiskAnalysisPipelineIntegrationTest {

    @Autowired
    private RiskScoreCalculator riskScoreCalculator;

    @Autowired
    private RiskLevelCalculator riskLevelCalculator;

    @Test
    void shouldCalculateMortgageScoreAndRiskLevelUsingDatabasePolicy() {

        RiskRule testRule = context -> List.of(
                RiskRuleResult.of(
                        CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                        CommonCodes.RISK_SEVERITY_HIGH,
                        "300000000",
                        "활성 근저당권이 존재합니다."
                )
        );

        RiskAnalysisEngine engine =
                new RiskAnalysisEngine(List.of(testRule));

        RiskAnalysisContext context =
                new RiskAnalysisContext(
                        5L,
                        2L,
                        List.of(),
                        List.of()
                );

        List<RiskRuleResult> results =
                engine.analyze(context);

        RiskScoreCalculationResult scoreResult =
                riskScoreCalculator.calculate(results);

        String riskLevel =
                riskLevelCalculator.calculate(
                        scoreResult.getTotalScore()
                );

        assertThat(results)
                .hasSize(1);

        assertThat(results.get(0).getFactorType())
                .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_MORTGAGE);

        assertThat(scoreResult.getFactors())
                .hasSize(1);

        assertThat(scoreResult.getFactors().get(0).getScore())
                .isEqualByComparingTo(new BigDecimal("30.00"));

        assertThat(scoreResult.getTotalScore())
                .isEqualByComparingTo(new BigDecimal("30.00"));

        assertThat(riskLevel)
                .isEqualTo(CommonCodes.RISK_LEVEL_MEDIUM);
    }

    @Test
    void shouldSumMultipleRiskFactorsUsingDatabasePolicies() {

        RiskRule testRule = context -> List.of(
                RiskRuleResult.of(
                        CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                        CommonCodes.RISK_SEVERITY_HIGH,
                        "300000000",
                        "활성 근저당권이 존재합니다."
                ),
                RiskRuleResult.of(
                        CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT,
                        CommonCodes.RISK_SEVERITY_HIGH,
                        "1",
                        "우선순위 권리가 존재합니다."
                )
        );

        RiskAnalysisEngine engine =
                new RiskAnalysisEngine(List.of(testRule));

        RiskAnalysisContext context =
                new RiskAnalysisContext(
                        5L,
                        2L,
                        List.of(),
                        List.of()
                );

        List<RiskRuleResult> results =
                engine.analyze(context);

        RiskScoreCalculationResult scoreResult =
                riskScoreCalculator.calculate(results);

        String riskLevel =
                riskLevelCalculator.calculate(
                        scoreResult.getTotalScore()
                );

        assertThat(scoreResult.getFactors())
                .hasSize(2);

        assertThat(scoreResult.getTotalScore())
                .isEqualByComparingTo(new BigDecimal("50.00"));

        assertThat(riskLevel)
                .isEqualTo(CommonCodes.RISK_LEVEL_HIGH);
    }
}