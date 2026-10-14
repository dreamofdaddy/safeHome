package com.safehome.backend.domain.document;

import com.safehome.backend.domain.property.PropertyRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "safehome.auth.enforce=true")
@AutoConfigureMockMvc
@Transactional
class ListingRegistrationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired RegistryDocumentRepository documents;
    @Autowired PrivatePdfStorage storage;
    @Autowired PropertyRepository properties;
    @Autowired JdbcTemplate jdbc;

    @Test
    void signupLoginThenStoreListingAndPrivatePdfWithoutCreatingProperty() throws Exception {
        String email = "listing-" + UUID.randomUUID() + "@example.test";
        String token = signupAndLogin(email);
        long beforeProperties = properties.count();
        byte[] pdf = onePagePdf();
        String metadata = "{\"address\":\"서울특별시 강남구 테스트로 1\","
            + "\"buildingType\":\"APARTMENT\",\"unitNumber\":\"101호\","
            + "\"transactionType\":\"JEONSE\",\"depositAmount\":200000000}";
        MockMultipartFile json = new MockMultipartFile("metadata", "", "application/json", metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockMultipartFile file = new MockMultipartFile("registryPdf", "registry.pdf", "application/pdf", pdf);

        mvc.perform(multipart("/api/registry-documents/listings")
                .file(json).file(file).header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.message").value("등기부 업로드 완료"))
            .andExpect(jsonPath("$.data.listing.status").value("READY_FOR_ANALYSIS"))
            .andExpect(jsonPath("$.data.listing.propertyId").doesNotExist())
            .andExpect(jsonPath("$.data.document.status").value("UPLOADED"));
        assertThat(properties.count()).isEqualTo(beforeProperties);
        var doc = documents.findAll().stream().filter(d -> d.getOriginalFilename().equals("registry.pdf"))
            .reduce((first, second) -> second).orElseThrow();
        assertThat(doc.getPropertyId()).isNull();
        assertThat(doc.getListingId()).isNotNull();
        assertThat(doc.getEncryptedContent()).isNull();
        assertThat(doc.getStorageKey()).matches("[0-9]{4}-[0-9]{2}-[0-9]{2}/[0-9a-f-]{36}\\.pdf");
        assertThat(storage.read(doc.getStorageKey())).isEqualTo(pdf);
        assertThat(jdbc.queryForObject("SELECT property_id FROM listings WHERE id=?", Long.class, doc.getListingId())).isNull();
        assertThat(jdbc.queryForObject("SELECT status FROM listings WHERE id=?", String.class, doc.getListingId()))
            .isEqualTo("READY_FOR_ANALYSIS");
        mvc.perform(get("/api/registry-documents/" + doc.getId() + "/file")
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isOk()).andExpect(content().bytes(pdf));
        String other = signupAndLogin("other-" + UUID.randomUUID() + "@example.test");
        mvc.perform(get("/api/registry-documents/" + doc.getId() + "/file")
                .header("Authorization", "Bearer " + other))
            .andExpect(status().isForbidden());

        mvc.perform(get("/api/listings/mine").header("Authorization", "Bearer " + token))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].id").value(doc.getListingId()));
        mvc.perform(get("/api/listings/mine").header("Authorization", "Bearer " + other))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void rejectsInvalidPdfWithoutCreatingListing() throws Exception {
        String token = signupAndLogin("badpdf-" + UUID.randomUUID() + "@example.test");
        Long before = jdbc.queryForObject("SELECT count(*) FROM listings", Long.class);
        String metadata = "{\"address\":\"서울 테스트\",\"buildingType\":\"APARTMENT\","
            + "\"transactionType\":\"JEONSE\",\"depositAmount\":200000000}";
        mvc.perform(multipart("/api/registry-documents/listings")
                .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes()))
                .file(new MockMultipartFile("registryPdf", "fake.pdf", "application/pdf", "not a pdf".getBytes()))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listings", Long.class)).isEqualTo(before);
    }

    private String signupAndLogin(String email) throws Exception {
        String password = "LongLocalPassword-2026!";
        mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password
                    + "\",\"name\":\"등록자\"}"))
            .andExpect(status().isCreated());
        var result = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}"))
            .andExpect(status().isOk()).andReturn();
        return result.getResponse().getContentAsString()
            .replaceFirst(".*\\\"accessToken\\\":\\\"([^\\\"]+)\\\".*", "$1");
    }

    private byte[] onePagePdf() throws Exception {
        try (PDDocument pdf = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            pdf.addPage(new PDPage());
            pdf.save(out);
            return out.toByteArray();
        }
    }
}
