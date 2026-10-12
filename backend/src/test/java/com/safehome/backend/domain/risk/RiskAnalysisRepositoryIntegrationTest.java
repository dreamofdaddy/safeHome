package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Rollback
class RiskAnalysisRepositoryIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private RiskAnalysisRepository riskAnalysisRepository;

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
    void shouldOrderByAnalyzedAtDescAndIdDesc() {
        LocalDateTime earlier =
                LocalDateTime.of(2026, 10, 8, 10, 0);

        LocalDateTime latest =
                LocalDateTime.of(2026, 10, 8, 11, 0);

        RiskAnalysis first = saveAnalysis(
                earlier,
                "10.00"
        );

        RiskAnalysis second = saveAnalysis(
                latest,
                "20.00"
        );

        RiskAnalysis third = saveAnalysis(
                latest,
                "30.00"
        );

        List<RiskAnalysis> analyses =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        );

        assertThat(analyses)
                .extracting(RiskAnalysis::getId)
                .containsExactly(
                        third.getId(),
                        second.getId(),
                        first.getId()
                );
    }

    @Test
    void shouldReturnHigherIdWhenAnalyzedAtIsSame() {
        LocalDateTime analyzedAt =
                LocalDateTime.of(2026, 10, 8, 11, 0);

        RiskAnalysis first = saveAnalysis(
                analyzedAt,
                "20.00"
        );

        RiskAnalysis second = saveAnalysis(
                analyzedAt,
                "30.00"
        );

        RiskAnalysis latest =
                riskAnalysisRepository
                        .findTopByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .orElseThrow();

        assertThat(latest.getId())
                .isEqualTo(second.getId());

        assertThat(latest.getId())
                .isGreaterThan(first.getId());
    }

    private RiskAnalysis saveAnalysis(
            LocalDateTime analyzedAt,
            String riskScore
    ) {
        RiskAnalysis riskAnalysis = new RiskAnalysis(
                PROPERTY_ID,
                null,
                "1.0",
                "V2",
                CommonCodes.RISK_LEVEL_LOW,
                new BigDecimal(riskScore),
                analyzedAt
        );

        return riskAnalysisRepository.saveAndFlush(riskAnalysis);
    }
}