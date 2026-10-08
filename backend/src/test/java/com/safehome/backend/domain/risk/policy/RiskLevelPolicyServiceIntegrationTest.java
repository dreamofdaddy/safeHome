package com.safehome.backend.domain.risk.policy;

import com.safehome.backend.common.code.CommonCodes;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class RiskLevelPolicyServiceIntegrationTest {

    @Autowired
    private RiskLevelPolicyService policyService;

    @Test
    void zeroScoreShouldBeLow() {
        assertThat(
                policyService.getRiskLevel(new BigDecimal("0.00"))
        ).isEqualTo(CommonCodes.RISK_LEVEL_LOW);
    }

    @Test
    void scoreBelowTwentyShouldBeLow() {
        assertThat(
                policyService.getRiskLevel(new BigDecimal("19.99"))
        ).isEqualTo(CommonCodes.RISK_LEVEL_LOW);
    }

    @Test
    void scoreOfTwentyShouldBeMedium() {
        assertThat(
                policyService.getRiskLevel(new BigDecimal("20.00"))
        ).isEqualTo(CommonCodes.RISK_LEVEL_MEDIUM);
    }

    @Test
    void scoreBelowFortyShouldBeMedium() {
        assertThat(
                policyService.getRiskLevel(new BigDecimal("39.99"))
        ).isEqualTo(CommonCodes.RISK_LEVEL_MEDIUM);
    }

    @Test
    void scoreOfFortyShouldBeHigh() {
        assertThat(
                policyService.getRiskLevel(new BigDecimal("40.00"))
        ).isEqualTo(CommonCodes.RISK_LEVEL_HIGH);
    }

    @Test
    void scoreBelowSeventyShouldBeHigh() {
        assertThat(
                policyService.getRiskLevel(new BigDecimal("69.99"))
        ).isEqualTo(CommonCodes.RISK_LEVEL_HIGH);
    }

    @Test
    void scoreOfSeventyShouldBeCritical() {
        assertThat(
                policyService.getRiskLevel(new BigDecimal("70.00"))
        ).isEqualTo(CommonCodes.RISK_LEVEL_CRITICAL);
    }
}