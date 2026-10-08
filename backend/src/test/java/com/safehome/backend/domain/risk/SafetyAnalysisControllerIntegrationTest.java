package com.safehome.backend.domain.risk;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.matchesRegex;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SafetyAnalysisControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

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
}