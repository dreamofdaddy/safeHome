package com.safehome.backend.domain.property;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.property.dto.PropertySafetyTrendResponse;
import com.safehome.backend.domain.risk.RiskAnalysis;
import com.safehome.backend.domain.risk.RiskAnalysisRepository;
import com.safehome.backend.domain.risk.comparison.RiskTrend;
import com.safehome.backend.domain.risk.policy.RiskPolicyVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Rollback
class PropertySafetyTrendServiceIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private PropertySafetyTrendService propertySafetyTrendService;

    @Autowired
    private RiskAnalysisRepository riskAnalysisRepository;

    @Autowired
    private RiskPolicyVersionService riskPolicyVersionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private String activePolicyVersion;

    @BeforeEach
    void setUp() {
        activePolicyVersion =
                riskPolicyVersionService.getActivePolicyVersion();

        jdbcTemplate.update("""
            DELETE FROM risk_factors
            WHERE risk_analysis_id IN (
                SELECT id
                FROM risk_analyses
                WHERE property_id = ?
            )
            """,
            PROPERTY_ID
        );

        jdbcTemplate.update("""
            DELETE FROM risk_analyses
            WHERE property_id = ?
            """,
            PROPERTY_ID
        );
    }

    @Test
    void shouldReturnEmptyTrendWhenNoAnalysisExists() {
        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.analysisCount())
                .isZero();

        assertThat(response.latestAnalysis())
                .isNull();

        assertThat(response.previousAnalysis())
                .isNull();

        assertThat(response.scoreDelta())
                .isNull();

        assertThat(response.trend())
                .isNull();

        assertThat(response.policyVersionChanged())
                .isFalse();

        assertThat(response.history())
                .isEmpty();
    }

    @Test
    void shouldReturnLatestAnalysisWhenOnlyOneAnalysisExists() {
        RiskAnalysis analysis = saveAnalysis(
                "30.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.analysisCount())
                .isEqualTo(1);

        assertThat(response.latestAnalysis())
                .isNotNull();

        assertThat(response.latestAnalysis().id())
                .isEqualTo(analysis.getId());

        assertThat(response.previousAnalysis())
                .isNull();

        assertThat(response.scoreDelta())
                .isNull();

        assertThat(response.trend())
                .isNull();

        assertThat(response.policyVersionChanged())
                .isFalse();

        assertThat(response.history())
                .hasSize(1);
    }

    @Test
    void shouldReturnImprovedWhenLatestScoreIsLower() {
        RiskAnalysis previous = saveAnalysis(
                "70.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RiskAnalysis current = saveAnalysis(
                "40.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.analysisCount())
                .isEqualTo(2);

        assertThat(response.latestAnalysis().id())
                .isEqualTo(current.getId());

        assertThat(response.previousAnalysis().id())
                .isEqualTo(previous.getId());

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("-30.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.IMPROVED);

        assertThat(response.policyVersionChanged())
                .isFalse();

        assertThat(response.history())
                .extracting(riskAnalysis -> riskAnalysis.id())
                .containsExactly(
                        current.getId(),
                        previous.getId()
                );
    }

    @Test
    void shouldReturnWorsenedWhenLatestScoreIsHigher() {
        saveAnalysis(
                "20.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveAnalysis(
                "65.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("45.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.WORSENED);
    }

    @Test
    void shouldReturnUnchangedWhenScoresAreSame() {
        saveAnalysis(
                "35.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveAnalysis(
                "35.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("0.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.UNCHANGED);
    }

    @Test
    void shouldUseHigherIdAsLatestWhenAnalyzedAtIsSame() {
        LocalDateTime analyzedAt =
                LocalDateTime.of(2026, 10, 12, 10, 0);

        RiskAnalysis previous = saveAnalysis(
                "60.00",
                activePolicyVersion,
                analyzedAt
        );

        RiskAnalysis current = saveAnalysis(
                "30.00",
                activePolicyVersion,
                analyzedAt
        );

        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.latestAnalysis().id())
                .isEqualTo(current.getId());

        assertThat(response.previousAnalysis().id())
                .isEqualTo(previous.getId());

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("-30.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.IMPROVED);
    }

    @Test
    void shouldDetectPolicyVersionChange() {
        String previousPolicyVersion =
                jdbcTemplate.queryForObject("""
                    SELECT policy_version
                    FROM risk_policy_versions
                    WHERE policy_version <> ?
                    ORDER BY id DESC
                    LIMIT 1
                    """,
                    String.class,
                    activePolicyVersion
                );

        saveAnalysis(
                "50.00",
                previousPolicyVersion,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveAnalysis(
                "40.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.policyVersionChanged())
                .isTrue();

        assertThat(response.trend())
                .isEqualTo(RiskTrend.IMPROVED);

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("-10.00");
    }

    @Test
    void shouldReturnEntireHistoryInLatestFirstOrder() {
        RiskAnalysis oldest = saveAnalysis(
                "70.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RiskAnalysis middle = saveAnalysis(
                "50.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        RiskAnalysis latest = saveAnalysis(
                "30.00",
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        PropertySafetyTrendResponse response =
                propertySafetyTrendService.getTrend(PROPERTY_ID);

        assertThat(response.analysisCount())
                .isEqualTo(3);

        assertThat(response.history())
                .extracting(riskAnalysis -> riskAnalysis.id())
                .containsExactly(
                        latest.getId(),
                        middle.getId(),
                        oldest.getId()
                );

        /*
         * Trend는 전체 기간의 oldest -> latest가 아니라
         * 최신 두 건인 middle -> latest 기준이다.
         */
        assertThat(response.scoreDelta())
                .isEqualByComparingTo("-20.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.IMPROVED);
    }

    @Test
    void shouldThrowWhenPropertyDoesNotExist() {
        assertThatThrownBy(() ->
                propertySafetyTrendService.getTrend(999999L)
        )
        .isInstanceOf(PropertyNotFoundException.class);
    }

    private RiskAnalysis saveAnalysis(
            String riskScore,
            String policyVersion,
            LocalDateTime analyzedAt
    ) {
        RiskAnalysis analysis =
                new RiskAnalysis(
                        PROPERTY_ID,
                        null,
                        "1.0",
                        policyVersion,
                        CommonCodes.RISK_LEVEL_LOW,
                        new BigDecimal(riskScore),
                        analyzedAt
                );

        return riskAnalysisRepository.saveAndFlush(
                analysis
        );
    }
}