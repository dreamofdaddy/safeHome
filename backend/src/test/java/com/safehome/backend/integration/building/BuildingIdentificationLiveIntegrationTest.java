package com.safehome.backend.integration.building;

import com.safehome.backend.domain.document.parser.ParsedRegistryDocument;
import com.safehome.backend.domain.document.parser.RegistryDocumentParser;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
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

/** Opt-in verification against a running Integration Server, in the dedicated test database. */
@SpringBootTest(properties = {"safehome.auth.enforce=true", "safehome.integration.enabled=true"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "SAFEHOME_LIVE_INTEGRATION_TEST", matches = "true")
class BuildingIdentificationLiveIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ObjectMapper mapper;
    @MockitoBean RegistryDocumentParser parser;

    @Test
    void committedRegistryTriggersLiveLookupAndPersistsSeparateResult() throws Exception {
        String database = jdbc.queryForObject("select current_database()", String.class);
        assertThat(database).isEqualTo("safehome_stage6_live_test");

        String email = "building-live-" + UUID.randomUUID() + "@example.test";
        String password = "LongLocalPassword-2026!";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password
                        + "\",\"name\":\"검증 사용자\"}"))
                .andExpect(status().isCreated());
        String login = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        String token = mapper.readTree(login).path("data").path("accessToken").stringValue();

        String metadata = """
            {"address":"서울 구로구 개봉동 355-18","detailAddress":"1층 101호",
             "buildingType":"VILLA","transactionType":"JEONSE","depositAmount":20000000,
             "addressSearchEvidence":{"selectedType":"J","selectedAddress":"서울 구로구 개봉동 355-18",
               "jibunAddress":"서울 구로구 개봉동 355-18","bcode":"1153010700",
               "sigunguCode":"11530","noSelected":"N"}}
            """;
        byte[] pdf;
        try (PDDocument document = new PDDocument(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            document.addPage(new PDPage()); document.save(output); pdf = output.toByteArray();
        }
        String created = mvc.perform(multipart("/api/registry-documents/listings")
                .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes()))
                .file(new MockMultipartFile("registryPdf", "synthetic-test.pdf", "application/pdf", pdf))
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long listingId = mapper.readTree(created).path("data").path("listing").path("id").asLong();
        String number = "1413-2026-" + String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000);
        when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,
                "서울 구로구 개봉동 355-18 1층 101호", null, null, "101호", "2026-10-13T10:00:00",
                List.of(new ParsedRegistryDocument.Owner("홍길동", "800101-*******", BigDecimal.valueOf(100), null)),
                List.of()));
        when(parser.provider()).thenReturn("TEST");
        when(parser.model()).thenReturn("synthetic-fixture");

        mvc.perform(post("/api/listings/{id}/registry-processing", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isAccepted());
        for (int i=0; i<100; i++) {
            String doc=jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=?",String.class,listingId);
            if("PARSED".equals(doc))break;
            Thread.sleep(100);
        }
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_identifications WHERE listing_id=?",Integer.class,listingId)).isZero();
        mvc.perform(post("/api/listings/{id}/building-identification/lookup", listingId)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"detailAddress\":\"160동 지상 1층 101호\"}")).andExpect(status().isAccepted());
        String execution = null;
        for (int i = 0; i < 150; i++) {
            List<String> states = jdbc.query("""
                    SELECT b.execution_status FROM building_identifications b
                    JOIN listings l ON l.current_building_identification_id=b.id WHERE l.id=?
                    """, (rs, row) -> rs.getString(1), listingId);
            execution = states.isEmpty() ? null : states.getFirst();
            if ("COMPLETED".equals(execution) || "CALL_FAILED".equals(execution)
                    || "INPUT_INCOMPLETE".equals(execution) || "INPUT_CONFLICT".equals(execution)) break;
            Thread.sleep(1000);
        }
        assertThat(execution).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=?",
                String.class, listingId)).isEqualTo("PARSED");
        assertThat(jdbc.queryForObject("SELECT attempt_count FROM building_identifications WHERE listing_id=?",
                Integer.class, listingId)).isEqualTo(1);
        var building = mapper.readTree(mvc.perform(get("/api/listings/{id}/building-identification", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString()).path("data");
        assertThat(building.path("executionStatus").stringValue()).isEqualTo("COMPLETED");
        assertThat(building.path("source").stringValue()).isEqualTo("MOLIT_BUILDING_HUB");
        assertThat(building.path("resolutionStatus").stringValue()).isEqualTo("MATCHED");
        assertThat(building.path("managementNumber").stringValue()).isEqualTo("1018152541");
        assertThat(building.path("parentManagementNumber").stringValue()).isEqualTo("1018112940");
        assertThat(jdbc.queryForObject("SELECT management_number FROM building_identifications WHERE listing_id=?",
                String.class, listingId)).isEqualTo("1018152541");
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId))
                .isNull();
        System.out.println("LIVE_BUILDING_FLOW listingId=" + listingId + " result="
                + building.path("resolutionStatus").stringValue());
    }
}
