package com.safehome.backend.integration.building;

import com.safehome.backend.domain.document.RegistryDocumentRepository;
import com.safehome.backend.domain.document.parser.ParsedRegistryDocument;
import com.safehome.backend.domain.document.parser.RegistryDocumentParser;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
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
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {"safehome.auth.enforce=true", "safehome.integration.enabled=true",
        "safehome.integration.token=local-test-token-012345678901234567890123"})
@AutoConfigureMockMvc
class BuildingIdentificationFlowIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @Autowired RegistryDocumentRepository documents;
    @MockitoBean RegistryDocumentParser parser;
    @MockitoBean BuildingEvidenceClient buildingClient;

    @Test
    void startsBuildingLookupAfterRegistryCommitAndKeepsRiskAnalysisSeparate() throws Exception {
        String token = signupAndLogin();
        String metadata = """
            {"address":"서울 송파구 잠실동 19","detailAddress":"160동 101호",
             "buildingType":"APARTMENT","transactionType":"JEONSE","depositAmount":20000000,
             "addressSearchEvidence":{"selectedType":"J","selectedAddress":"서울 송파구 잠실동 19",
               "jibunAddress":"서울 송파구 잠실동 19","bcode":"1171010100",
               "sigunguCode":"11710","noSelected":"N"}}
            """;
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage()); document.save(output); pdf = output.toByteArray();
        }
        String body = mvc.perform(multipart("/api/registry-documents/listings")
                .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes()))
                .file(new MockMultipartFile("registryPdf", "registry.pdf", "application/pdf", pdf))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long listingId = mapper.readTree(body).path("data").path("listing").path("id").asLong();
        String number = "1413-2026-" + String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000);
        when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,
                "서울 송파구 잠실동 19 160동 1층 101호", null, null, "101호", "2026-10-13T10:00:00",
                List.of(new ParsedRegistryDocument.Owner("홍길동", "800101-*******", BigDecimal.valueOf(100), null)),
                List.of()));
        when(parser.provider()).thenReturn("TEST");
        when(parser.model()).thenReturn("fixture");
        when(buildingClient.resolve(any(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer(invocation -> {
            BuildingQuery query = invocation.getArgument(0);
            assertThat(query.floorNumber()).isEqualTo(1);
            assertThat(query.unitNumber()).isEqualTo("101");
            assertThat(jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=?",
                    String.class, listingId)).isEqualTo("PARSED");
            BuildingUnit unit = new BuildingUnit("unit-pk", "title-pk", "11710", "10100", "0",
                    "0019", "0000", "서울 송파구 잠실동 19", null, "160동",
                    BuildingQuery.Ground.ABOVE_GROUND, 1, "1층 101호", "APARTMENT");
            return new Resolution(Resolution.Status.MATCHED, List.of(unit), null,
                    "MOLIT_BUILDING_HUB", Instant.now(), true);
        });
        mvc.perform(post("/api/listings/{id}/registry-processing", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isAccepted());
        for (int i = 0; i < 100; i++) {
            List<String> statuses = jdbc.query("""
                    SELECT b.execution_status FROM building_identifications b
                    JOIN listings l ON l.current_building_identification_id=b.id WHERE l.id=?
                    """, (rs, row) -> rs.getString(1), listingId);
            String status = statuses.isEmpty() ? null : statuses.getFirst();
            if ("COMPLETED".equals(status)) break;
            if ("CALL_FAILED".equals(status) || "INPUT_CONFLICT".equals(status)
                    || "INPUT_INCOMPLETE".equals(status)) throw new AssertionError(status);
            Thread.sleep(100);
        }
        var result = mapper.readTree(mvc.perform(get("/api/listings/{id}/building-identification", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        assertThat(result.path("executionStatus").stringValue()).isEqualTo("COMPLETED");
        assertThat(result.path("resolutionStatus").stringValue()).isEqualTo("MATCHED");
        assertThat(result.path("managementNumber").stringValue()).isEqualTo("unit-pk");
        mvc.perform(get("/api/listings/{id}/building-identification", listingId)
                .header("Authorization", "Bearer " + signupAndLogin()))
            .andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId))
                .isNull();
        assertThat(documents.findTopByListingIdOrderByIdDesc(listingId).orElseThrow().getStatus()).isEqualTo("PARSED");
    }

    @Test
    void failedLookupPreservesParsedRegistryAndAllowsUnchangedSafetyAnalysis() throws Exception {
        String token = signupAndLogin();
        String metadata = """
            {"address":"서울 송파구 잠실동 19","detailAddress":"160동 101호",
             "buildingType":"APARTMENT","transactionType":"JEONSE","depositAmount":20000000,
             "addressSearchEvidence":{"selectedType":"J","selectedAddress":"서울 송파구 잠실동 19",
               "jibunAddress":"서울 송파구 잠실동 19","bcode":"1171010100",
               "sigunguCode":"11710","noSelected":"N"}}
            """;
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage()); document.save(output); pdf = output.toByteArray();
        }
        String body = mvc.perform(multipart("/api/registry-documents/listings")
                .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes()))
                .file(new MockMultipartFile("registryPdf", "synthetic-test.pdf", "application/pdf", pdf))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long listingId = mapper.readTree(body).path("data").path("listing").path("id").asLong();
        String number = "1413-2026-" + String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000);
        when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,
                "서울 송파구 잠실동 19 160동 1층 101호", null, null, "101호", "2026-10-13T10:00:00",
                List.of(new ParsedRegistryDocument.Owner("홍길동", "800101-*******", BigDecimal.valueOf(100), null)),
                List.of()));
        when(parser.provider()).thenReturn("TEST");
        when(parser.model()).thenReturn("synthetic-fixture");
        when(buildingClient.resolve(any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(Resolution.unavailable("INTEGRATION_UNAVAILABLE"));

        mvc.perform(post("/api/listings/{id}/registry-processing", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isAccepted());
        String execution = null;
        for (int i = 0; i < 100; i++) {
            List<String> statuses = jdbc.query("""
                    SELECT b.execution_status FROM building_identifications b
                    JOIN listings l ON l.current_building_identification_id=b.id WHERE l.id=?
                    """, (rs, row) -> rs.getString(1), listingId);
            execution = statuses.isEmpty() ? null : statuses.getFirst();
            if ("CALL_FAILED".equals(execution)) break;
            Thread.sleep(100);
        }
        assertThat(execution).isEqualTo("CALL_FAILED");
        assertThat(jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=?",
                String.class, listingId)).isEqualTo("PARSED");
        var identification = mapper.readTree(mvc.perform(get("/api/listings/{id}/building-identification", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        assertThat(identification.path("resolutionStatus").isNull()).isTrue();

        var safety = mapper.readTree(mvc.perform(post("/api/listings/{id}/safety-analysis", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        long riskId = safety.path("analysis").path("id").asLong();
        assertThat(riskId).isPositive();
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId))
                .isEqualTo(riskId);
        assertThat(jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=?",
                String.class, listingId)).isEqualTo("PARSED");
        assertThat(jdbc.queryForObject("SELECT execution_status FROM building_identifications WHERE listing_id=?",
                String.class, listingId)).isEqualTo("CALL_FAILED");
    }

    private String signupAndLogin() throws Exception {
        String email = "building-" + UUID.randomUUID() + "@example.test";
        String password = "LongLocalPassword-2026!";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"등록자\"}"))
            .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data").path("accessToken").stringValue();
    }
}
