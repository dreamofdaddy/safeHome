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
    @Autowired jakarta.persistence.EntityManager entityManager;

    @Test
    void savesValidatedPostcodeEvidenceAndClearsItOnAddressEdit() throws Exception {
        String token = signupAndLogin("postcode-" + UUID.randomUUID() + "@example.test");
        String filename = UUID.randomUUID() + ".pdf";
        String metadata = """
            {"address":"서울 송파구 잠실동 19","detailAddress":"160동 101호",
             "buildingType":"APARTMENT","transactionType":"JEONSE","depositAmount":200000000,
             "addressSearchEvidence":{"selectedType":"J","selectedAddress":"서울 송파구 잠실동 19",
               "jibunAddress":"서울 송파구 잠실동 19","bcode":"1171010100",
               "sigunguCode":"11710","noSelected":"N"}}
            """;
        mvc.perform(multipart("/api/registry-documents/listings")
                .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("registryPdf", filename, "application/pdf", onePagePdf()))
                .header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.listing.addressSearchEvidence.bcode").value("1171010100"));
        var doc = documents.findAll().stream().filter(d -> filename.equals(d.getOriginalFilename())).findFirst().orElseThrow();
        Long id = doc.getListingId();
        entityManager.flush(); entityManager.clear();
        assertThat(jdbc.queryForObject("SELECT address_search_bcode FROM listings WHERE id=?", String.class, id))
                .isEqualTo("1171010100");
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(metadata.replace("\"11710\",\"noSelected\"", "\"11530\",\"noSelected\"")))
            .andExpect(status().isBadRequest());
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(metadata.replace("\"address\":\"서울 송파구 잠실동 19\"",
                        "\"address\":\"서울 송파구 잠실동 20\"")))
            .andExpect(status().isBadRequest());
        String manualEdit = """
            {"address":"서울 송파구 잠실동 20","detailAddress":"160동 101호",
             "buildingType":"APARTMENT","transactionType":"JEONSE","depositAmount":200000000}
            """;
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(manualEdit))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.addressSearchEvidence").value(org.hamcrest.Matchers.nullValue()));
        entityManager.flush(); entityManager.clear();
        assertThat(jdbc.queryForObject("SELECT address_search_bcode FROM listings WHERE id=?", String.class, id))
                .isNull();
    }

    @Test
    void signupLoginThenStoreListingAndPrivatePdfWithoutCreatingProperty() throws Exception {
        String email = "listing-" + UUID.randomUUID() + "@example.test";
        String token = signupAndLogin(email);
        long beforeProperties = properties.count();
        byte[] pdf = onePagePdf();
        String metadata = "{\"address\":\"서울특별시 강남구 테스트로 1\","
            + "\"buildingType\":\"APARTMENT\",\"detailAddress\":\"101호\","
            + "\"transactionType\":\"JEONSE\",\"depositAmount\":200000000}";
        MockMultipartFile json = new MockMultipartFile("metadata", "", "application/json", metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        MockMultipartFile file = new MockMultipartFile("registryPdf", "registry.pdf", "application/pdf", pdf);

        mvc.perform(multipart("/api/registry-documents/listings")
                .file(json).file(file).header("Authorization", "Bearer " + token))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.message").value("등기부 업로드 완료"))
            .andExpect(jsonPath("$.data.listing.status").value("READY_FOR_ANALYSIS"))
            .andExpect(jsonPath("$.data.listing.address").value("서울특별시 강남구 테스트로 1"))
            .andExpect(jsonPath("$.data.listing.roadAddress").doesNotExist())
            .andExpect(jsonPath("$.data.listing.postalCode").doesNotExist())
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

    @Test
    void ownerCanReviseFailedListingAndDeleteItsDocumentButNotProcessingOrParsedListing() throws Exception {
        String token = signupAndLogin("edit-" + UUID.randomUUID() + "@example.test");
        String other = signupAndLogin("edit-other-" + UUID.randomUUID() + "@example.test");
        String filename = UUID.randomUUID() + ".pdf";
        String metadata = """
            {"address":"서울특별시 강남구 테스트로 1","detailAddress":"테스트아파트 101동 제202호",
             "buildingName":"테스트아파트","buildingType":"APARTMENT",
             "transactionType":"JEONSE","depositAmount":200000000}
            """;
        mvc.perform(multipart("/api/registry-documents/listings")
            .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .file(new MockMultipartFile("registryPdf", filename, "application/pdf", onePagePdf()))
            .header("Authorization", "Bearer " + token)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.listing.detailAddress").value("테스트아파트 101동 제202호"));
        var doc = documents.findAll().stream().filter(d -> filename.equals(d.getOriginalFilename())).findFirst().orElseThrow();
        Long id = doc.getListingId();
        String corrected = metadata.replace("제202호", "제203호");
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + other)
            .contentType(MediaType.APPLICATION_JSON).content(corrected)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/listings/" + id).header("Authorization", "Bearer " + other)).andExpect(status().isForbidden());
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(corrected)).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.detailAddress").value("테스트아파트 101동 제203호"));
        for (String state : new String[] {"REGISTRY_PROCESSING", "REGISTRY_READY", "SAFETY_ANALYZED"}) {
            entityManager.flush(); entityManager.clear();
            jdbc.update("UPDATE listings SET status=? WHERE id=?", state, id);
            mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isConflict());
            mvc.perform(delete("/api/listings/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isConflict());
        }
        jdbc.update("UPDATE listings SET status='REGISTRY_FAILED' WHERE id=?", id);
        entityManager.clear();
        mvc.perform(put("/api/listings/" + id).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(metadata)).andExpect(status().isOk());
        mvc.perform(delete("/api/listings/" + id).header("Authorization", "Bearer " + token)).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM listings WHERE id=?", Long.class, id)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_documents WHERE listing_id=?", Long.class, id)).isZero();
    }

    @Test
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void committedDeletionAlsoRemovesPrivatePdf() throws Exception {
        String token = signupAndLogin("delete-file-" + UUID.randomUUID() + "@example.test");
        String filename = UUID.randomUUID() + ".pdf";
        String metadata = """
            {"address":"서울 테스트로 1","detailAddress":"101호","buildingType":"APARTMENT",
             "transactionType":"JEONSE","depositAmount":200000000}
            """;
        mvc.perform(multipart("/api/registry-documents/listings")
            .file(new MockMultipartFile("metadata", "", "application/json", metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
            .file(new MockMultipartFile("registryPdf", filename, "application/pdf", onePagePdf()))
            .header("Authorization", "Bearer " + token)).andExpect(status().isCreated());
        var doc = documents.findAll().stream().filter(d -> filename.equals(d.getOriginalFilename())).findFirst().orElseThrow();
        assertThat(storage.read(doc.getStorageKey())).isNotEmpty();
        mvc.perform(delete("/api/listings/" + doc.getListingId()).header("Authorization", "Bearer " + token))
            .andExpect(status().isNoContent());
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> storage.read(doc.getStorageKey()))
            .isInstanceOf(IllegalStateException.class);
        assertThat(documents.findById(doc.getId())).isEmpty();
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
