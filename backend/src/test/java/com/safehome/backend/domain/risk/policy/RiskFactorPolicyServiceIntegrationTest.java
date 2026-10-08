package com.safehome.backend.domain.risk.policy;

import com.safehome.backend.common.code.CommonCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RiskFactorPolicyServiceIntegrationTest {

    @Autowired
    private RiskFactorPolicyService policyService;

    @Test
    void shouldLoadMortgageHighScoreFromDatabase() {
        BigDecimal score = policyService.getScore(
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH
        );

        assertThat(score)
                .isEqualByComparingTo("30.00");
    }

    @Test
    void shouldLoadMortgageHighPolicyFromDatabase() {
        RiskFactorPolicy policy = policyService.findActive(
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH
        );

        assertThat(policy.getPolicyVersion())
                .isEqualTo("V2");

        assertThat(policy.getFactorType())
                .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_MORTGAGE);

        assertThat(policy.getSeverity())
                .isEqualTo(CommonCodes.RISK_SEVERITY_HIGH);

        assertThat(policy.getScore())
                .isEqualByComparingTo("30.00");

        assertThat(policy.getUseYn())
                .isEqualTo("Y");
    }
}