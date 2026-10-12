package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
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
class RiskAnalysisComparisonControllerIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

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
    void comparison_should_return_200_with_latest_two_analyses()
            throws Exception {

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

        mockMvc.perform(
                get(
                        "/api/risk-analyses/property/{propertyId}/comparison",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.data.propertyId").value(PROPERTY_ID))
        .andExpect(jsonPath("$.data.previousAnalysis.id")
                .value(previous.getId()))
        .andExpect(jsonPath("$.data.currentAnalysis.id")
                .value(current.getId()))
        .andExpect(jsonPath("$.data.previousAnalysis.policyVersion")
                .value("V1"))
        .andExpect(jsonPath("$.data.currentAnalysis.policyVersion")
                .value("V2"))
        .andExpect(jsonPath("$.data.previousAnalysis.riskLevel")
                .value(CommonCodes.RISK_LEVEL_HIGH))
        .andExpect(jsonPath("$.data.currentAnalysis.riskLevel")
                .value(CommonCodes.RISK_LEVEL_MEDIUM))
        .andExpect(jsonPath("$.data.scoreDelta")
                .value(-25.0))
        .andExpect(jsonPath("$.data.trend")
                .value("IMPROVED"))
        .andExpect(jsonPath("$.data.policyVersionChanged")
                .value(true))
        .andExpect(jsonPath("$.data.factorChanges").isArray())
        .andExpect(jsonPath("$.data.factorChanges.length()")
                .value(2));
    }

    @Test
    void comparison_should_return_factor_change_details()
            throws Exception {

        RiskAnalysis previous = saveAnalysis(
                "V2",
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
                current.getId(),
                CommonCodes.RISK_FACTOR_TYPE_MORTGAGE,
                CommonCodes.RISK_SEVERITY_HIGH,
                "25.00",
                "250000000",
                "Mortgage exists"
        );

        mockMvc.perform(
                get(
                        "/api/risk-analyses/property/{propertyId}/comparison",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.factorChanges.length()")
                .value(1))
        .andExpect(jsonPath("$.data.factorChanges[0].changeType")
                .value("CHANGED"))
        .andExpect(jsonPath("$.data.factorChanges[0].previous.factorType")
                .value(CommonCodes.RISK_FACTOR_TYPE_MORTGAGE))
        .andExpect(jsonPath("$.data.factorChanges[0].previous.value")
                .value("300000000"))
        .andExpect(jsonPath("$.data.factorChanges[0].current.factorType")
                .value(CommonCodes.RISK_FACTOR_TYPE_MORTGAGE))
        .andExpect(jsonPath("$.data.factorChanges[0].current.value")
                .value("250000000"));
    }

    @Test
    void comparison_should_return_400_when_only_one_analysis_exists()
            throws Exception {

        saveAnalysis(
                "V2",
                CommonCodes.RISK_LEVEL_LOW,
                "10.00",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        mockMvc.perform(
                get(
                        "/api/risk-analyses/property/{propertyId}/comparison",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-001"));
    }

    @Test
    void comparison_should_return_400_when_no_analysis_exists()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/risk-analyses/property/{propertyId}/comparison",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-001"));
    }

    @Test
    void comparison_should_return_404_when_property_not_found()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/risk-analyses/property/{propertyId}/comparison",
                        999999L
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-002"));
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