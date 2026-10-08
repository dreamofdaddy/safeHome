package com.safehome.backend.domain.risk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesRegex;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SafetyAnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SafetyAnalysisService safetyAnalysisService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void analyze_should_return_201_created() throws Exception {

        Long propertyId = 5L;

        mockMvc.perform(
                        post(
                                "/api/properties/{propertyId}/safety-analysis",
                                propertyId
                        )
                )
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        matchesRegex("/api/risk-analyses/\\d+")
                ))
                        .andExpect(jsonPath("$.success").value(true))
                        .andExpect(jsonPath("$.data").exists())
                        .andExpect(jsonPath("$.data.id").isNumber())
                        .andExpect(jsonPath("$.data.propertyId").value(propertyId))
                        .andExpect(jsonPath("$.data.registrySnapshotId").isNumber())
                        .andExpect(jsonPath("$.data.analysisVersion").value("1.0"))
                        .andExpect(jsonPath("$.data.policyVersion").isString())
                        .andExpect(jsonPath("$.data.riskLevel").isString())
                        .andExpect(jsonPath("$.data.riskScore").isNumber())
                        .andExpect(jsonPath("$.data.analyzedAt").isString())
                        .andExpect(jsonPath("$.data.factors").isArray());
    }

    @Test
    void analyze_should_return_404_when_property_not_found()
            throws Exception {

        Long propertyId = 999999L;

        mockMvc.perform(
                post(
                        "/api/properties/{propertyId}/safety-analysis",
                        propertyId
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.data").doesNotExist())
        .andExpect(jsonPath("$.error").exists())
        .andExpect(jsonPath("$.error.code").value("COMMON-002"))
        .andExpect(jsonPath("$.error.message")
                .value("요청한 리소스를 찾을 수 없습니다."));
    }

    @Test
    void result_should_return_saved_analysis_and_factors() throws Exception {
        jdbcTemplate.update(
                "UPDATE registry_rights SET status = 'ACTIVE' WHERE registry_snapshot_id = ?",
                2L
        );
        var analysis = safetyAnalysisService.analyze(5L);
        assertThat(analysis.factors()).isNotEmpty();

        mockMvc.perform(get("/api/risk-analyses/{id}/result", analysis.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(analysis.id()))
                .andExpect(jsonPath("$.data.propertyId").value(5))
                .andExpect(jsonPath("$.data.policyVersion").value(analysis.policyVersion()))
                .andExpect(jsonPath("$.data.riskScore").value(analysis.riskScore().doubleValue()))
                .andExpect(jsonPath("$.data.factors.length()").value(analysis.factors().size()))
                .andExpect(jsonPath("$.data.factors[0].id").value(analysis.factors().getFirst().id()))
                .andExpect(jsonPath("$.data.factors[0].factorType")
                        .value(analysis.factors().getFirst().factorType()));
    }

    @Test
    void result_should_return_404_when_analysis_not_found() throws Exception {
        mockMvc.perform(get("/api/risk-analyses/{id}/result", 999999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("COMMON-002"));
    }
}
