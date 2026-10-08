package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.domain.risk.dto.SafetyAnalysisResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Rollback
class SafetyAnalysisServiceIntegrationTest {

    @Autowired
    private SafetyAnalysisService safetyAnalysisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnLowRiskWhenNoActiveRiskDataExists() {
        jdbcTemplate.update("""
            UPDATE registry_rights
            SET status = ?
            WHERE registry_snapshot_id = ?
            """,
            CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE,
            2L
        );

        jdbcTemplate.update("""
            UPDATE contracts
            SET status = ?
            WHERE id = ?
            """,
            "DRAFT",
            3L
        );

        SafetyAnalysisResponse response =
                safetyAnalysisService.analyze(5L);

        assertThat(response.policyVersion())
                .isEqualTo("V2");

        assertThat(response.riskLevel())
                .isEqualTo(CommonCodes.RISK_LEVEL_LOW);

        assertThat(response.riskScore())
                .isEqualByComparingTo("0.00");

        assertThat(response.factors())
                .isEmpty();
    }

    @Test
    void shouldCalculateHighRiskFromMortgageAndPriorityRight() {
        jdbcTemplate.update("""
            UPDATE registry_rights
            SET status = ?
            WHERE registry_snapshot_id = ?
            """,
            CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE,
            2L
        );

        jdbcTemplate.update("""
            UPDATE contracts
            SET status = ?
            WHERE id = ?
            """,
            CommonCodes.CONTRACT_STATUS_ACTIVE,
            3L
        );

        SafetyAnalysisResponse response =
                safetyAnalysisService.analyze(5L);

        assertThat(response.policyVersion())
                .isEqualTo("V2");

        assertThat(response.riskLevel())
                .isEqualTo(CommonCodes.RISK_LEVEL_HIGH);

        assertThat(response.riskScore())
                .isEqualByComparingTo("50.00");

        assertThat(response.factors())
                .hasSize(2);

        assertThat(response.factors())
                .anySatisfy(factor -> {
                    assertThat(factor.factorType())
                            .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_MORTGAGE);
                    assertThat(factor.severity())
                            .isEqualTo(CommonCodes.RISK_SEVERITY_HIGH);
                    assertThat(factor.score())
                            .isEqualByComparingTo("30.00");
                });

        assertThat(response.factors())
                .anySatisfy(factor -> {
                    assertThat(factor.factorType())
                            .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT);
                    assertThat(factor.severity())
                            .isEqualTo(CommonCodes.RISK_SEVERITY_HIGH);
                    assertThat(factor.score())
                            .isEqualByComparingTo("20.00");
                });
    }

    @Test
    void shouldCalculateCriticalRiskFromMortgageAndSeizure() {
        jdbcTemplate.update("""
            UPDATE registry_rights
            SET
                right_type = ?,
                status = ?
            WHERE id = ?
            """,
            CommonCodes.REGISTRY_RIGHT_TYPE_PROVISIONAL_SEIZURE,
            CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE,
            2L
        );

        jdbcTemplate.update("""
            UPDATE registry_rights
            SET status = ?
            WHERE id = ?
            """,
            CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE,
            1L
        );

        jdbcTemplate.update("""
            UPDATE contracts
            SET status = ?
            WHERE id = ?
            """,
            CommonCodes.CONTRACT_STATUS_ACTIVE,
            3L
        );

        SafetyAnalysisResponse response =
                safetyAnalysisService.analyze(5L);

        assertThat(response.policyVersion())
                .isEqualTo("V2");

        assertThat(response.riskLevel())
                .isEqualTo(CommonCodes.RISK_LEVEL_CRITICAL);

        assertThat(response.riskScore())
                .isEqualByComparingTo("80.00");

        assertThat(response.factors())
                .hasSize(2);

        assertThat(response.factors())
                .anySatisfy(factor -> {
                    assertThat(factor.factorType())
                            .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_MORTGAGE);
                    assertThat(factor.score())
                            .isEqualByComparingTo("30.00");
                });

        assertThat(response.factors())
                .anySatisfy(factor -> {
                    assertThat(factor.factorType())
                            .isEqualTo(CommonCodes.RISK_FACTOR_TYPE_SEIZURE);
                    assertThat(factor.severity())
                            .isEqualTo(CommonCodes.RISK_SEVERITY_CRITICAL);
                    assertThat(factor.score())
                            .isEqualByComparingTo("50.00");
                });
    }

    @Test
    void shouldThrowBusinessExceptionWhenPropertyDoesNotExist() {
        assertThatThrownBy(() ->
                safetyAnalysisService.analyze(999999L)
        )
        .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldCalculateMediumRiskFromCoOwnerIssue() {
        // Registry 위험 요소 제거
        jdbcTemplate.update("""
            UPDATE registry_rights
            SET status = ?
            WHERE registry_snapshot_id = ?
            """,
            CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE,
            2L
        );

        // Contract 활성화
        jdbcTemplate.update("""
            UPDATE contracts
            SET status = ?
            WHERE id = ?
            """,
            CommonCodes.CONTRACT_STATUS_ACTIVE,
            3L
        );

        // 임시 공동소유자 생성
        String email = "safehome.coowner.test@example.com";

        jdbcTemplate.update("""
            INSERT INTO users (
                email,
                name
            )
            VALUES (?, ?)
            """,
            email,
            "Co Owner Test"
        );

        Long coOwnerUserId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM users
            WHERE email = ?
            """,
            Long.class,
            email
        );

        // Property #5에 두 번째 공동소유자 30% 추가
        jdbcTemplate.update("""
            INSERT INTO property_owners (
                property_id,
                user_id,
                ownership_ratio
            )
            VALUES (?, ?, ?)
            """,
            5L,
            coOwnerUserId,
            new BigDecimal("30.00")
        );

        SafetyAnalysisResponse response =
                safetyAnalysisService.analyze(5L);

        assertThat(response.policyVersion())
                .isEqualTo("V2");

        assertThat(response.riskLevel())
                .isEqualTo(CommonCodes.RISK_LEVEL_MEDIUM);

        assertThat(response.riskScore())
                .isEqualByComparingTo("25.00");

        assertThat(response.factors())
                .hasSize(1);

        assertThat(response.factors())
                .anySatisfy(factor -> {
                    assertThat(factor.factorType())
                            .isEqualTo(
                                    CommonCodes.RISK_FACTOR_TYPE_CO_OWNER_ISSUE);

                    assertThat(factor.severity())
                            .isEqualTo(CommonCodes.RISK_SEVERITY_HIGH);

                    assertThat(factor.score())
                            .isEqualByComparingTo("25.00");
                });
    }

    @Test
    void shouldCalculateMediumRiskFromOwnershipMismatch() {
        // Registry 위험 요소 제거
        jdbcTemplate.update("""
            UPDATE registry_rights
            SET status = ?
            WHERE registry_snapshot_id = ?
            """,
            CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE,
            2L
        );

        // 임시 비소유자 사용자 생성
        String email = "safehome.nonowner.test@example.com";

        jdbcTemplate.update("""
            INSERT INTO users (
                email,
                name
            )
            VALUES (?, ?)
            """,
            email,
            "Non Owner Test"
        );

        Long nonOwnerUserId = jdbcTemplate.queryForObject("""
            SELECT id
            FROM users
            WHERE email = ?
            """,
            Long.class,
            email
        );

        // 기존 계약의 임대인을 실제 소유자가 아닌 사용자로 변경
        jdbcTemplate.update("""
            UPDATE contracts
            SET
                status = ?,
                landlord_user_id = ?
            WHERE id = ?
            """,
            CommonCodes.CONTRACT_STATUS_ACTIVE,
            nonOwnerUserId,
            3L
        );

        SafetyAnalysisResponse response =
                safetyAnalysisService.analyze(5L);

        assertThat(response.policyVersion())
                .isEqualTo("V2");

        assertThat(response.riskLevel())
                .isEqualTo(CommonCodes.RISK_LEVEL_MEDIUM);

        assertThat(response.riskScore())
                .isEqualByComparingTo("25.00");

        assertThat(response.factors())
                .hasSize(1);

        assertThat(response.factors())
                .anySatisfy(factor -> {
                    assertThat(factor.factorType())
                            .isEqualTo(
                                    CommonCodes.RISK_FACTOR_TYPE_OWNERSHIP_MISMATCH);

                    assertThat(factor.severity())
                            .isEqualTo(CommonCodes.RISK_SEVERITY_HIGH);

                    assertThat(factor.score())
                            .isEqualByComparingTo("25.00");
                });
    }
}
