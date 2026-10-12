package com.safehome.backend.domain.property;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.risk.RiskAnalysis;
import com.safehome.backend.domain.risk.RiskAnalysisRepository;
import com.safehome.backend.domain.risk.policy.RiskPolicyVersionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class PropertySafetyTrendControllerIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

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
    void trend_shouldReturnEmptyHistoryWhenNoAnalysisExists()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/properties/{propertyId}/safety-trend",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.propertyId")
                .value(PROPERTY_ID))
        .andExpect(jsonPath("$.data.analysisCount")
                .value(0))
        .andExpect(jsonPath("$.data.history.length()")
                .value(0));
    }

    @Test
    void trend_shouldReturnImprovedWhenLatestScoreIsLower()
            throws Exception {

        RiskAnalysis previous = saveAnalysis(
                "70.00",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RiskAnalysis current = saveAnalysis(
                "40.00",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        mockMvc.perform(
                get(
                        "/api/properties/{propertyId}/safety-trend",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.analysisCount")
                .value(2))
        .andExpect(jsonPath("$.data.latestAnalysis.id")
                .value(current.getId()))
        .andExpect(jsonPath("$.data.previousAnalysis.id")
                .value(previous.getId()))
        .andExpect(jsonPath("$.data.scoreDelta")
                .value(-30.00))
        .andExpect(jsonPath("$.data.trend")
                .value("IMPROVED"))
        .andExpect(jsonPath("$.data.policyVersionChanged")
                .value(false))
        .andExpect(jsonPath("$.data.history.length()")
                .value(2));
    }

    @Test
    void trend_shouldReturnWorsenedWhenLatestScoreIsHigher()
            throws Exception {

        saveAnalysis(
                "20.00",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveAnalysis(
                "65.00",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        mockMvc.perform(
                get(
                        "/api/properties/{propertyId}/safety-trend",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.scoreDelta")
                .value(45.00))
        .andExpect(jsonPath("$.data.trend")
                .value("WORSENED"));
    }

    @Test
    void trend_shouldReturn404WhenPropertyDoesNotExist()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/properties/{propertyId}/safety-trend",
                        999999L
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success")
                .value(false))
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-002"));
    }

    private RiskAnalysis saveAnalysis(
            String riskScore,
            LocalDateTime analyzedAt
    ) {
        RiskAnalysis analysis =
                new RiskAnalysis(
                        PROPERTY_ID,
                        null,
                        "1.0",
                        activePolicyVersion,
                        CommonCodes.RISK_LEVEL_LOW,
                        new BigDecimal(riskScore),
                        analyzedAt
                );

        return riskAnalysisRepository.saveAndFlush(
                analysis
        );
    }
}