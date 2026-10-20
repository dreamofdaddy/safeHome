package com.safehome.backend.domain.document;

import com.safehome.backend.domain.document.parser.ParsedRegistryDocument;
import com.safehome.backend.domain.document.parser.RegistryDocumentParser;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "safehome.auth.enforce=true")
@AutoConfigureMockMvc
class RegistryProcessingIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @MockitoBean RegistryDocumentParser parser;

    @Test
    void userStartsAnalysisAndParsedRegistryIsSavedWithoutSafetyResult() throws Exception {
        String token = signupAndLogin();
        long listingId = register(token);
        String number = "1413-2026-" + String.format("%06d", Math.abs(UUID.randomUUID().hashCode()) % 1000000);
        when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,
            "서울특별시 강남구 테스트로 1 제101호", null, null, "101호", "2026-10-13T10:00:00",
            List.of(new ParsedRegistryDocument.Owner("홍길동", "800101-*******", BigDecimal.valueOf(100), null)),
            List.of(new ParsedRegistryDocument.Right("MORTGAGE", "은행", BigDecimal.valueOf(10000000), 1,
                "2020-01-01T00:00:00", "INACTIVE"))));
        when(parser.provider()).thenReturn("TEST");
        when(parser.model()).thenReturn("fixed-fixture");

        mvc.perform(post("/api/listings/{id}/registry-processing", listingId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isAccepted());
        waitForStatus(listingId, "PARSED");
        JsonNode response = mapper.readTree(mvc.perform(get("/api/listings/{id}/registry-processing", listingId)
                .header("Authorization", "Bearer " + token)).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString()).path("data");
        long propertyId = response.path("propertyId").asLong();
        long snapshotId = response.path("registrySnapshotId").asLong();
        assertThat(response.path("status").stringValue()).isEqualTo("PARSED");
        assertThat(propertyId).isPositive();
        assertThat(snapshotId).isPositive();
        assertThat(jdbc.queryForObject("SELECT status FROM listings WHERE id=?", String.class, listingId))
            .isEqualTo("REGISTRY_READY");
        assertThat(jdbc.queryForObject("SELECT registry_property_number FROM properties WHERE id=?", String.class, propertyId))
            .isEqualTo(number);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM property_owners WHERE property_id=? AND registry_snapshot_id=?", Long.class, propertyId, snapshotId))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_rights WHERE registry_snapshot_id=? AND status='INACTIVE'", Long.class, snapshotId))
            .isEqualTo(1);
        assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_analyses WHERE property_id=?", Long.class, propertyId))
            .isZero();
        mvc.perform(post("/api/listings/{id}/registry-processing", listingId)
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isConflict());
    }

    private long register(String token) throws Exception {
        String metadata = "{\"address\":\"서울특별시 강남구 테스트로 1\","+
            "\"buildingType\":\"APARTMENT\",\"detailAddress\":\"101호\","+
            "\"transactionType\":\"JEONSE\",\"depositAmount\":20000000}";
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

    private String signupAndLogin() throws Exception {
        String email = "parse-" + UUID.randomUUID() + "@example.test";
        String password = "LongLocalPassword-2026!";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"name\":\"등록자\"}"))
            .andExpect(status().isCreated());
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return mapper.readTree(body).path("data").path("accessToken").stringValue();
    }

    private void waitForStatus(long listingId, String expected) throws Exception {
        for (int attempt = 0; attempt < 100; attempt++) {
            String status = jdbc.queryForObject(
                "SELECT status FROM registry_documents WHERE listing_id=?", String.class, listingId);
            if (expected.equals(status)) return;
            if ("FAILED".equals(status)) throw new AssertionError("Parser failed: " + jdbc.queryForObject(
                "SELECT error_code FROM registry_documents WHERE listing_id=?", String.class, listingId));
            Thread.sleep(100);
        }
        throw new AssertionError("Timed out waiting for registry processing");
    }
}
