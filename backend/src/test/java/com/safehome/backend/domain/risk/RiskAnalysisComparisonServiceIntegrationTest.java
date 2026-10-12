package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.risk.comparison.RiskFactorChangeType;
import com.safehome.backend.domain.risk.comparison.RiskTrend;
import com.safehome.backend.domain.risk.dto.RiskAnalysisComparisonResponse;
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
class RiskAnalysisComparisonServiceIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private RiskAnalysisComparisonService comparisonService;

    @Autowired
    private RiskAnalysisRepository riskAnalysisRepository;

    @Autowired
    private RiskFactorRepository riskFactorRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
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
    void shouldReturnImprovedComparisonWithFactorChanges() {
        RiskAnalysis previous = saveAnalysis(
                "V1",
                CommonCodes.RISK_LEVEL_HIGH,
                "50.00",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RiskAnalysis current = saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_MEDIUM,
                "25.00",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        saveFactor(
                previous.getId(),
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH,
                "30.00",
                "300000000",
                "Mortgage exists"
        );

        saveFactor(
                previous.getId(),
                CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT,
                CommonCodes.RISK_SEVERITY_HIGH,
                "20.00",
                "1",
                "Priority right exists"
        );

        saveFactor(
                current.getId(),
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH,
                "25.00",
                "250000000",
                "Mortgage exists"
        );

        RiskAnalysisComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.propertyId())
                .isEqualTo(PROPERTY_ID);

        assertThat(response.previousAnalysis().id())
                .isEqualTo(previous.getId());

        assertThat(response.currentAnalysis().id())
                .isEqualTo(current.getId());

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("-25.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.IMPROVED);

        assertThat(response.policyVersionChanged())
                .isTrue();

        assertThat(response.factorChanges())
                .hasSize(2);

        assertThat(response.factorChanges())
                .anySatisfy(change -> {
                    assertThat(change.changeType())
                            .isEqualTo(RiskFactorChangeType.CHANGED);

                    assertThat(change.previous())
                            .isNotNull();

                    assertThat(change.current())
                            .isNotNull();

                    assertThat(change.previous().factorType())
                            .isEqualTo(
                                    CommonCodes.RISK_FACTOR_TYPE_MORTGAGE
                            );

                    assertThat(change.previous().value())
                            .isEqualTo("300000000");

                    assertThat(change.current().value())
                            .isEqualTo("250000000");
                });

        assertThat(response.factorChanges())
                .anySatisfy(change -> {
                    assertThat(change.changeType())
                            .isEqualTo(RiskFactorChangeType.REMOVED);

                    assertThat(change.previous())
                            .isNotNull();

                    assertThat(change.current())
                            .isNull();

                    assertThat(change.previous().factorType())
                            .isEqualTo(
                                    CommonCodes.RISK_FACTOR_TYPE_PRIORITY_RIGHT
                            );
                });
    }

    @Test
    void shouldReturnWorsenedWhenCurrentScoreIsHigher() {
        saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_LOW,
                "10.00",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_HIGH,
                "60.00",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        RiskAnalysisComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("50.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.WORSENED);

        assertThat(response.policyVersionChanged())
                .isFalse();
    }

    @Test
    void shouldReturnUnchangedWhenCurrentScoreIsSame() {
        saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_MEDIUM,
                "25.00",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_MEDIUM,
                "25.00",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        RiskAnalysisComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.scoreDelta())
                .isEqualByComparingTo("0.00");

        assertThat(response.trend())
                .isEqualTo(RiskTrend.UNCHANGED);

        assertThat(response.policyVersionChanged())
                .isFalse();
    }

    @Test
    void shouldUseHigherIdAsCurrentWhenAnalyzedAtIsSame() {
        LocalDateTime analyzedAt =
                LocalDateTime.of(2026, 10, 11, 10, 0);

        RiskAnalysis previous = saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_LOW,
                "10.00",
                analyzedAt
        );

        RiskAnalysis current = saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_MEDIUM,
                "20.00",
                analyzedAt
        );

        RiskAnalysisComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.previousAnalysis().id())
                .isEqualTo(previous.getId());

        assertThat(response.currentAnalysis().id())
                .isEqualTo(current.getId());

        assertThat(response.trend())
                .isEqualTo(RiskTrend.WORSENED);
    }

    @Test
    void shouldThrowWhenOnlyOneAnalysisExists() {
        saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_LOW,
                "10.00",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        assertThatThrownBy(() ->
                comparisonService.compareLatest(PROPERTY_ID)
        )
        .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldThrowWhenNoAnalysisExists() {
        assertThatThrownBy(() ->
                comparisonService.compareLatest(PROPERTY_ID)
        )
        .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldThrowWhenPropertyDoesNotExist() {
        assertThatThrownBy(() ->
                comparisonService.compareLatest(999999L)
        )
        .isInstanceOf(PropertyNotFoundException.class);
    }

    private RiskAnalysis saveAnalysis(
            String policyVersion,
            String riskLevel,
            String riskScore,
            LocalDateTime analyzedAt
    ) {
        RiskAnalysis analysis = new RiskAnalysis(
                PROPERTY_ID,
                null,
                "1.0",
                policyVersion,
                riskLevel,
                new BigDecimal(riskScore),
                analyzedAt
        );

        return riskAnalysisRepository.saveAndFlush(analysis);
    }

    private RiskFactor saveFactor(
            Long riskAnalysisId,
            String factorType,
            String severity,
            String score,
            String value,
            String description
    ) {
        RiskFactor factor = new RiskFactor(
                riskAnalysisId,
                factorType,
                severity,
                new BigDecimal(score),
                value,
                description
        );

        return riskFactorRepository.saveAndFlush(factor);
    }
}