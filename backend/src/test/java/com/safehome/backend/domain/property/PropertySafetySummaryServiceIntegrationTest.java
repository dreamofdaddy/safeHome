package com.safehome.backend.domain.property;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.risk.SafetyAnalysisService;
import com.safehome.backend.domain.property.dto.PropertySafetySummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Rollback
class PropertySafetySummaryServiceIntegrationTest {

    @Autowired
    private PropertySafetySummaryService propertySafetySummaryService;

    @Autowired
    private SafetyAnalysisService safetyAnalysisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnHighRiskSummary() {
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

        safetyAnalysisService.analyze(5L);

        PropertySafetySummaryResponse response =
                propertySafetySummaryService.getSafetySummary(5L);

        assertThat(response.property().id())
                .isEqualTo(5L);

        assertThat(response.latestRiskAnalysis())
                .isNotNull();

        assertThat(response.latestRiskAnalysis().policyVersion())
                .isEqualTo("V2");

        assertThat(response.latestRiskAnalysis().riskLevel())
                .isEqualTo(CommonCodes.RISK_LEVEL_HIGH);

        assertThat(response.latestRiskAnalysis().riskScore())
                .isEqualByComparingTo("50.00");

        assertThat(response.riskFactors())
                .hasSize(2);

        assertThat(response.summary())
                .isEqualTo(
                        "위험 요소가 2건 확인되어 계약 전 상세 확인이 필요합니다."
                );

        assertThat(response.highestSeverity())
                .isEqualTo(CommonCodes.RISK_SEVERITY_HIGH);

        assertThat(response.riskFactorCount())
                .isEqualTo(2);

        assertThat(response.activeListings())
                .isNotEmpty();

        assertThat(response.contracts())
                .isNotEmpty();
    }

    @Test
    void shouldReturnLowRiskSummary() {
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

        safetyAnalysisService.analyze(5L);

        PropertySafetySummaryResponse response =
                propertySafetySummaryService.getSafetySummary(5L);

        assertThat(response.latestRiskAnalysis())
                .isNotNull();

        assertThat(response.latestRiskAnalysis().riskLevel())
                .isEqualTo(CommonCodes.RISK_LEVEL_LOW);

        assertThat(response.latestRiskAnalysis().riskScore())
                .isEqualByComparingTo("0.00");

        assertThat(response.riskFactors())
                .isEmpty();

        assertThat(response.summary())
                .isEqualTo(
                        "현재 분석에서 주요 위험 요소가 확인되지 않았습니다."
                );

        assertThat(response.highestSeverity())
                .isNull();

        assertThat(response.riskFactorCount())
                .isZero();
    }

    @Test
    void shouldReturnSummaryWithoutAnalysis() {
        jdbcTemplate.update("""
            DELETE FROM risk_factors
            WHERE risk_analysis_id IN (
                SELECT id
                FROM risk_analyses
                WHERE property_id = ?
            )
            """,
            5L
        );

        jdbcTemplate.update("""
            DELETE FROM risk_analyses
            WHERE property_id = ?
            """,
            5L
        );

        PropertySafetySummaryResponse response =
                propertySafetySummaryService.getSafetySummary(5L);

        assertThat(response.property().id())
                .isEqualTo(5L);

        assertThat(response.latestRiskAnalysis())
                .isNull();

        assertThat(response.riskFactors())
                .isEmpty();

        assertThat(response.summary())
                .isEqualTo(
                        "아직 안전성 분석 결과가 없습니다."
                );

        assertThat(response.highestSeverity())
                .isNull();

        assertThat(response.riskFactorCount())
                .isZero();
    }

    @Test
    void shouldThrowWhenPropertyDoesNotExist() {
        assertThatThrownBy(() ->
                propertySafetySummaryService.getSafetySummary(999999L)
        )
        .isInstanceOf(PropertyNotFoundException.class);
    }
}