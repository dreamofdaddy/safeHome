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
    @Autowired BuildingIdentificationStore store;
    @Autowired BuildingIdentificationService service;
    @MockitoBean RegistryDocumentParser parser;
    @MockitoBean BuildingEvidenceClient buildingClient;

    @Test
    void registryCommitDoesNotCallBuildingHubAndManualLookupKeepsRiskAnalysisSeparate() throws Exception {
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
        for (int i=0; i<100; i++) {
            if ("PARSED".equals(documents.findTopByListingIdOrderByIdDesc(listingId).orElseThrow().getStatus())) break;
            Thread.sleep(100);
        }
        assertThat(documents.findTopByListingIdOrderByIdDesc(listingId).orElseThrow().getStatus()).isEqualTo("PARSED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_identifications WHERE listing_id=?",Integer.class,listingId)).isZero();
        org.mockito.Mockito.verifyNoInteractions(buildingClient);
        mvc.perform(post("/api/listings/{id}/building-identification/lookup", listingId)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"detailAddress\":\"160동 지상 1층 101호\"}")).andExpect(status().isAccepted());
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
        for (int i=0; i<100; i++) {
            if ("PARSED".equals(documents.findTopByListingIdOrderByIdDesc(listingId).orElseThrow().getStatus())) break;
            Thread.sleep(100);
        }
        assertThat(documents.findTopByListingIdOrderByIdDesc(listingId).orElseThrow().getStatus()).isEqualTo("PARSED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_identifications WHERE listing_id=?",Integer.class,listingId)).isZero();
        org.mockito.Mockito.verifyNoInteractions(buildingClient);
        mvc.perform(post("/api/listings/{id}/building-identification/lookup", listingId)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content("{\"detailAddress\":\"160동 지상 1층 101호\"}")).andExpect(status().isAccepted());
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

        long failedId = jdbc.queryForObject("SELECT current_building_identification_id FROM listings WHERE id=?", Long.class, listingId);
        mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId))
                .andExpect(status().isUnauthorized());
        String otherToken = signupAndLogin();
        mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId)
                .header("Authorization", "Bearer " + otherToken)).andExpect(status().isForbidden());
        var started = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(buildingClient.resolve(any(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer(invocation -> {
            started.countDown();
            assertThat(release.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            return new Resolution(Resolution.Status.UNVERIFIABLE, List.of(),
                    "CANDIDATE_FLOOR_NUMBER_UNAVAILABLE", "MOLIT_BUILDING_HUB", Instant.now(), true);
        });
        try {
            mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId)
                    .header("Authorization", "Bearer " + token)).andExpect(status().isAccepted())
                    .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.executionStatus").value("PENDING"));
            assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId)
                    .header("Authorization", "Bearer " + token)).andExpect(status().isConflict());
        } finally { release.countDown(); }
        for (int i = 0; i < 100; i++) {
            String current = jdbc.queryForObject("SELECT b.execution_status FROM listings l JOIN building_identifications b ON b.id=l.current_building_identification_id WHERE l.id=?", String.class, listingId);
            if ("COMPLETED".equals(current)) break;
            Thread.sleep(100);
        }
        long nextId = jdbc.queryForObject("SELECT current_building_identification_id FROM listings WHERE id=?", Long.class, listingId);
        assertThat(nextId).isNotEqualTo(failedId);
        assertThat(jdbc.queryForObject("SELECT execution_status FROM building_identifications WHERE id=?", String.class, failedId)).isEqualTo("CALL_FAILED");
        assertThat(jdbc.queryForObject("SELECT execution_status FROM building_identifications WHERE id=?", String.class, nextId)).isEqualTo("COMPLETED");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM building_identifications WHERE listing_id=?", Long.class, listingId)).isEqualTo(2);
        assertThat(jdbc.queryForObject("SELECT a.query_json=b.query_json AND a.input_json=b.input_json AND (a.registry_snapshot_id IS NOT DISTINCT FROM b.registry_snapshot_id) FROM building_identifications a CROSS JOIN building_identifications b WHERE a.id=? AND b.id=?", Boolean.class, failedId, nextId)).isTrue();
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId)).isEqualTo(riskId);
        assertThat(documents.findTopByListingIdOrderByIdDesc(listingId).orElseThrow().getStatus()).isEqualTo("PARSED");
        when(buildingClient.resolve(any(), org.mockito.ArgumentMatchers.anyLong())).thenReturn(new Resolution(
                Resolution.Status.MATCHED, List.of(new BuildingUnit("estimated-unit", "estimated-parent",
                "11710", "10100", "0", "0019", "0000", "synthetic", null, "160동",
                BuildingQuery.Ground.ABOVE_GROUND, null, "101호", "synthetic")),
                "FLOOR_INFERRED_FROM_UNIT_NUMBER", "MOLIT_BUILDING_HUB", Instant.now(), true));
        mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isAccepted());
        for (int i = 0; i < 100; i++) {
            String current = jdbc.queryForObject("SELECT b.resolution_status FROM listings l JOIN building_identifications b ON b.id=l.current_building_identification_id WHERE l.id=?", String.class, listingId);
            if ("MATCHED".equals(current)) break;
            Thread.sleep(100);
        }
        mvc.perform(get("/api/listings/{id}/building-identification", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.reason").value("FLOOR_INFERRED_FROM_UNIT_NUMBER"));
        assertThat(jdbc.queryForObject("SELECT reason FROM building_identifications WHERE id=?", String.class, nextId)).isEqualTo("CANDIDATE_FLOOR_NUMBER_UNAVAILABLE");
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId)).isEqualTo(riskId);
        for (String resolutionStatus : List.of("MATCHED", "UNVERIFIABLE", "AMBIGUOUS", "NOT_FOUND")) {
            long previousId = jdbc.queryForObject("SELECT current_building_identification_id FROM listings WHERE id=?", Long.class, listingId);
            jdbc.update("UPDATE building_identifications SET resolution_status=?,reason='FLOOR_INFERENCE_CONFLICT' WHERE id=?", resolutionStatus, previousId);
            mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId)
                    .header("Authorization", "Bearer " + otherToken)).andExpect(status().isForbidden());
            mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId)
                    .header("Authorization", "Bearer " + token)).andExpect(status().isAccepted());
            for (int i = 0; i < 100; i++) {
                String current = jdbc.queryForObject("SELECT b.execution_status FROM listings l JOIN building_identifications b ON b.id=l.current_building_identification_id WHERE l.id=?", String.class, listingId);
                if ("COMPLETED".equals(current)) break;
                Thread.sleep(100);
            }
            assertThat(jdbc.queryForObject("SELECT current_building_identification_id FROM listings WHERE id=?", Long.class, listingId)).isNotEqualTo(previousId);
            assertThat(jdbc.queryForObject("SELECT resolution_status FROM building_identifications WHERE id=?", String.class, previousId)).isEqualTo(resolutionStatus);
            assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?", Long.class, listingId)).isEqualTo(riskId);
        }
        long pausedId = jdbc.queryForObject("SELECT current_building_identification_id FROM listings WHERE id=?", Long.class, listingId);
        jdbc.update("UPDATE building_identifications SET execution_status='DISABLED',attempt_count=0 WHERE id=?", pausedId);
        assertThat(store.recover(true)).doesNotContain(pausedId);
        assertThat(jdbc.queryForObject("SELECT execution_status FROM building_identifications WHERE id=?", String.class, pausedId)).isEqualTo("DISABLED");
        mvc.perform(post("/api/listings/{id}/building-identification/retry", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isAccepted());
        for (int i = 0; i < 100; i++) {
            String current = jdbc.queryForObject("SELECT b.execution_status FROM listings l JOIN building_identifications b ON b.id=l.current_building_identification_id WHERE l.id=?", String.class, listingId);
            if ("COMPLETED".equals(current)) break;
            Thread.sleep(100);
        }
        assertThat(jdbc.queryForObject("SELECT current_building_identification_id FROM listings WHERE id=?", Long.class, listingId)).isNotEqualTo(pausedId);
        org.mockito.Mockito.verify(parser, org.mockito.Mockito.times(1)).parse(any());
    }

    @Test
    void manualLookupNeedsNoDocumentsAndPreservesListingWhileAcceptingQueryOnlyEvidence() throws Exception {
        String token = signupAndLogin();
        String metadata = """
            {"address":"서울 송파구 잠실동 19","detailAddress":"160동 101호",
             "buildingType":"APARTMENT","transactionType":"JEONSE","depositAmount":20000000,
             "addressSearchEvidence":{"selectedType":"J","selectedAddress":"서울 송파구 잠실동 19",
               "jibunAddress":"서울 송파구 잠실동 19","bcode":"1171010100",
               "sigunguCode":"11710","noSelected":"N"}}
            """;
        String created = mvc.perform(post("/api/listings").header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long id = mapper.readTree(created).path("data").path("id").asLong();
        jdbc.update("UPDATE listings SET address_search_type=NULL WHERE id=?", id);
        mvc.perform(post("/api/listings/{id}/building-identification/lookup", id)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isAccepted())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.executionStatus").value("INPUT_INCOMPLETE"));
        org.mockito.Mockito.verifyNoInteractions(parser, buildingClient);
        String other = signupAndLogin();
        mvc.perform(post("/api/listings/{id}/building-identification/lookup", id)
                .header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
        String request = """
            {"detailAddress":"160동 지상 1층 101호","addressSearchEvidence":{
              "selectedType":"J","selectedAddress":"서울 송파구 잠실동 19","jibunAddress":"서울 송파구 잠실동 19",
              "bcode":"1171010100","sigunguCode":"11710","noSelected":"N"}}
            """;
        mvc.perform(post("/api/listings/{id}/building-identification/lookup", id)
                .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON)
                .content(request.replace("잠실동 19", "잠실동 20"))).andExpect(status().isBadRequest());
        var started = new java.util.concurrent.CountDownLatch(1);
        var release = new java.util.concurrent.CountDownLatch(1);
        when(buildingClient.resolve(any(), org.mockito.ArgumentMatchers.anyLong())).thenAnswer(invocation -> {
            BuildingQuery query = invocation.getArgument(0);
            assertThat(query.floorNumber()).isEqualTo(1);
            assertThat(query.unitNumber()).isEqualTo("101");
            started.countDown();
            assertThat(release.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            return new Resolution(Resolution.Status.NOT_FOUND, List.of(), null, "MOCK", Instant.now(), true);
        });
        try {
            mvc.perform(post("/api/listings/{id}/building-identification/lookup", id)
                    .header("Authorization", "Bearer " + token).contentType(MediaType.APPLICATION_JSON).content(request))
                    .andExpect(status().isAccepted());
            assertThat(started.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/listings/{id}/building-identification/lookup", id)
                    .header("Authorization", "Bearer " + token)).andExpect(status().isConflict());
        } finally { release.countDown(); }
        String execution = null;
        for (int i=0; i<100; i++) {
            execution = jdbc.queryForObject("SELECT b.execution_status FROM listings l JOIN building_identifications b ON b.id=l.current_building_identification_id WHERE l.id=?", String.class, id);
            if ("COMPLETED".equals(execution)) break;
            Thread.sleep(100);
        }
        assertThat(execution).isEqualTo("COMPLETED");
        mvc.perform(get("/api/listings/{id}/building-identification", id).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath("$.data.registrySnapshotId").doesNotExist());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_documents WHERE listing_id=?", Integer.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT detail_address FROM listings WHERE id=?", String.class, id)).isEqualTo("160동 101호");
        assertThat(jdbc.queryForObject("SELECT address_search_type FROM listings WHERE id=?", String.class, id)).isNull();
        assertThat(jdbc.queryForObject("SELECT input_json FROM building_identifications WHERE id=(SELECT current_building_identification_id FROM listings WHERE id=?)", String.class, id)).contains("160동 지상 1층 101호", "LISTING");
        org.mockito.Mockito.verifyNoInteractions(parser);
        for (String interrupted : List.of("PENDING","RUNNING")) {
            jdbc.update("UPDATE building_identifications SET execution_status=? WHERE id=(SELECT current_building_identification_id FROM listings WHERE id=?)",interrupted,id);
            service.resumeInterrupted();
            assertThat(store.current(id).executionStatus()).isEqualTo("CALL_FAILED");
            assertThat(store.current(id).reason()).isEqualTo("REQUEST_INTERRUPTED");
            org.mockito.Mockito.verify(buildingClient,org.mockito.Mockito.times(1)).resolve(any(),org.mockito.ArgumentMatchers.anyLong());
        }
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
