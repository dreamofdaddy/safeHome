package com.safehome.backend.domain.risk;

import com.safehome.backend.domain.document.parser.ParsedRegistryDocument;
import com.safehome.backend.domain.document.parser.RegistryDocumentParser;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "safehome.auth.enforce=true")
@AutoConfigureMockMvc
class ListingSafetyAnalysisIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired PropertyRepository properties;
    @Autowired RegistrySnapshotRepository snapshots;
    @Autowired RegistryRightRepository rights;
    @MockitoBean RegistryDocumentParser parser;

    @Test
    void ownerAnalyzesParsedListingAndReadsPersistedResultWithoutDuplicates() throws Exception {
        String ownerToken = signupAndLogin("owner");
        long listingId = register(ownerToken);
        String propertyNumber = "1413-2026-" + String.format("%06d",
            Integer.parseInt(UUID.randomUUID().toString().substring(0, 5), 16) % 1_000_000);
        when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(propertyNumber,
            "서울특별시 강남구 테스트로 1 제101호", null, "테스트아파트", "101호",
            "2026-10-13T10:00:00",
            List.of(new ParsedRegistryDocument.Owner("홍길동", "800101-*******",
                BigDecimal.valueOf(100), null)),
            List.of(new ParsedRegistryDocument.Right("MORTGAGE", "은행",
                BigDecimal.valueOf(10000000), null, "2020-01-01T00:00:00", "INACTIVE"))));
        when(parser.provider()).thenReturn("TEST");
        when(parser.model()).thenReturn("fixed-fixture");

        mvc.perform(post("/api/listings/{id}/registry-processing", listingId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isAccepted());
        waitForDocument(listingId, "PARSED");

        String firstBody = mvc.perform(post("/api/listings/{id}/safety-analysis", listingId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.listingStatus").value("SAFETY_ANALYZED"))
            .andExpect(jsonPath("$.data.analysis.riskLevel").value("LOW"))
            .andExpect(jsonPath("$.data.analysis.riskScore").value(0))
            .andExpect(jsonPath("$.data.factorCount").value(0))
            .andReturn().getResponse().getContentAsString();
        JsonNode first = mapper.readTree(firstBody).path("data");
        long analysisId = first.path("analysis").path("id").asLong();
        long propertyId = first.path("analysis").path("propertyId").asLong();

        mvc.perform(get("/api/listings/{id}/safety-analysis", listingId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.analysis.id").value(analysisId))
            .andExpect(jsonPath("$.data.summary").isNotEmpty());
        mvc.perform(post("/api/listings/{id}/safety-analysis", listingId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.analysis.id").value(analysisId))
            .andExpect(jsonPath("$.data.reanalysisAvailable").value(false))
            .andExpect(jsonPath("$.data.reanalysisReason").value("UP_TO_DATE"));

        assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_analyses WHERE id=?", Long.class, analysisId))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId))
            .isEqualTo(analysisId);
        assertThat(jdbc.queryForObject("SELECT status FROM listings WHERE id=?", String.class, listingId))
            .isEqualTo("SAFETY_ANALYZED");

        RegistrySnapshot changed = snapshots.save(RegistrySnapshot.create(
            properties.findById(propertyId).orElseThrow(), "UPLOADED_PDF",
            LocalDateTime.of(2026, 10, 14, 12, 0), "changed-" + UUID.randomUUID()));
        rights.save(RegistryRight.create(changed, "MORTGAGE", "새로운은행",
            BigDecimal.valueOf(50000000), 1, LocalDateTime.of(2026, 10, 14, 11, 0), "ACTIVE"));

        mvc.perform(get("/api/listings/{id}/safety-analysis", listingId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.analysis.id").value(analysisId))
            .andExpect(jsonPath("$.data.latestRegistrySnapshotId").value(changed.getId()))
            .andExpect(jsonPath("$.data.reanalysisAvailable").value(true))
            .andExpect(jsonPath("$.data.reanalysisReason").value("REGISTRY_CHANGED"));

        String secondBody = mvc.perform(post("/api/listings/{id}/safety-analysis", listingId)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.analysis.registrySnapshotId").value(changed.getId()))
            .andExpect(jsonPath("$.data.factorCount").value(1))
            .andExpect(jsonPath("$.data.analysis.factors[0].factorType").value("MORTGAGE"))
            .andExpect(jsonPath("$.data.reanalysisAvailable").value(false))
            .andReturn().getResponse().getContentAsString();
        long secondAnalysisId = mapper.readTree(secondBody).path("data").path("analysis").path("id").asLong();
        assertThat(secondAnalysisId).isNotEqualTo(analysisId);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_analyses WHERE property_id=?", Long.class, propertyId))
            .isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId))
            .isEqualTo(secondAnalysisId);

        String otherToken = signupAndLogin("other");
        mvc.perform(get("/api/listings/{id}/safety-analysis", listingId)
                .header("Authorization", "Bearer " + otherToken))
            .andExpect(status().isForbidden());
    }

    private long register(String token) throws Exception {
        String metadata = "{\"address\":\"서울특별시 강남구 테스트로 1\","+
            "\"buildingName\":\"테스트아파트\",\"buildingType\":\"APARTMENT\","+
            "\"detailAddress\":\"101호\",\"transactionType\":\"MONTHLY_RENT\","+
            "\"depositAmount\":20000000,\"monthlyRent\":650000}";
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage()); document.save(output); pdf = output.toByteArray();
        }
        String body = mvc.perform(multipart("/api/registry-documents/listings")
                .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes()))
                .file(new MockMultipartFile("registryPdf", "registry.pdf", "application/pdf", pdf))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data").path("listing").path("id").asLong();
    }

    private String signupAndLogin(String prefix) throws Exception {
        String email = prefix + "-safety-" + UUID.randomUUID() + "@example.test";
        String password = "LongLocalPassword-2026!";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password
                    + "\",\"name\":\"테스트사용자\"}"))
            .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data").path("accessToken").stringValue();
    }

    private void waitForDocument(long listingId, String expected) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            String status = jdbc.queryForObject(
                "SELECT status FROM registry_documents WHERE listing_id=?", String.class, listingId);
            if (expected.equals(status)) return;
            if ("FAILED".equals(status)) throw new AssertionError("Registry processing failed");
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for registry processing");
    }
}
