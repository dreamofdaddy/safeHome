package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.safehome.backend.domain.document.parser.*;
import com.safehome.backend.integration.building.BuildingEvidenceClient;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.apache.pdfbox.pdmodel.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcPrint;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(properties={"safehome.auth.enforce=true","safehome.integration.enabled=false"})
@AutoConfigureMockMvc(print=MockMvcPrint.NONE)
class BuildingRegisterFlowTest {
    static final Path STORAGE=Path.of(System.getProperty("java.io.tmpdir"),"safehome-building-review-"+UUID.randomUUID());
    @DynamicPropertySource static void properties(DynamicPropertyRegistry registry){registry.add("safehome.document.storage-root",STORAGE::toString);}
    @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;@Autowired BuildingRegisterStore store;
    @MockitoBean BuildingRegisterParser buildingParser;
    @MockitoBean RegistryDocumentParser registryParser;
    @MockitoBean BuildingEvidenceClient evidenceClient;
    record Fixture(String token,long listingId){}
    private byte[] pdf() throws Exception {
        try(var document=new PDDocument();var output=new ByteArrayOutputStream()){
            document.addPage(new PDPage());document.save(output);return output.toByteArray();
        }
    }
    private String token() throws Exception {
        String email="building-pdf-"+UUID.randomUUID()+"@example.test",password="SyntheticPassword-2026!";
        mvc.perform(post("/api/auth/signup").contentType("application/json").content(mapper.writeValueAsString(Map.of("email",email,"password",password,"name","합성 테스트")))).andExpect(status().isCreated());
        var response=mvc.perform(post("/api/auth/login").contentType("application/json").content(mapper.writeValueAsString(Map.of("email",email,"password",password)))).andExpect(status().isOk()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).path("data").path("accessToken").asString();
    }
    private Fixture fixture(boolean parsed) throws Exception {
        String token=token();
        String metadata="""
            {"address":"서울 테스트로 1","detailAddress":"2층 202호","buildingType":"APARTMENT","transactionType":"JEONSE","depositAmount":10000000}
            """;
        var response=mvc.perform(multipart("/api/registry-documents/listings")
                .file(new MockMultipartFile("metadata","","application/json",metadata.getBytes(java.nio.charset.StandardCharsets.UTF_8)))
                .file(new MockMultipartFile("registryPdf","registry.pdf","application/pdf",pdf()))
                .header("Authorization","Bearer "+token)).andExpect(status().isCreated()).andReturn();
        long id=mapper.readTree(response.getResponse().getContentAsString()).path("data").path("listing").path("id").asLong();
        if(parsed) {
            String number="1413-2026-"+String.format("%06d",Math.abs(UUID.randomUUID().hashCode())%1000000);
            when(registryParser.parse(any())).thenReturn(new ParsedRegistryDocument(number,"서울 테스트로 1 2층 202호",null,null,"202호","2026-01-01T00:00:00",
                    List.of(new ParsedRegistryDocument.Owner("합성 소유자","800101-*******",BigDecimal.valueOf(100),null)),List.of()));
            when(registryParser.provider()).thenReturn("TEST");when(registryParser.model()).thenReturn("synthetic");
            mvc.perform(post("/api/listings/{id}/registry-processing",id).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());
            for(int n=0;n<100;n++) {if("PARSED".equals(jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=?",String.class,id)))break;Thread.sleep(100);}
            assertThat(jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=?",String.class,id)).isEqualTo("PARSED");
            mvc.perform(post("/api/listings/{id}/safety-analysis",id).header("Authorization","Bearer "+token)).andExpect(status().isOk());
        }
        return new Fixture(token,id);
    }
    private long upload(Fixture f) throws Exception {
        var response=mvc.perform(multipart("/api/listings/{id}/building-register",f.listingId())
                .file(new MockMultipartFile("files","building.pdf","application/pdf",pdf()))
                .header("Authorization","Bearer "+f.token())).andExpect(status().isCreated()).andReturn();
        return mapper.readTree(response.getResponse().getContentAsString()).path("data").path("id").asLong();
    }
    private Extraction extracted() {
        return new Extraction(true,List.of(new Entry(1,1,"EXCLUSIVE","2026-01-01",null,"서울 테스트로 1",null,null,"2층","202호","아파트",BigDecimal.valueOf(59),"MARKED",List.of(new Evidence(1,"VIOLATION","위반건축물 표시")))));
    }
    private void waitFor(long id,String target) throws Exception {
        for(int n=0;n<100;n++){if(target.equals(store.get(id).status()))return;Thread.sleep(100);}
        assertThat(store.get(id).status()).isEqualTo(target);
    }
    @Test void uploadRequiresOwnerAndConsentAndPreservesRegistryAndRiskResults() throws Exception {
        var f=fixture(true);String other=token();Long risk=jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?",Long.class,f.listingId());
        Long snapshot=jdbc.queryForObject("SELECT registry_snapshot_id FROM registry_documents WHERE listing_id=?",Long.class,f.listingId());
        long id=upload(f);verifyNoInteractions(buildingParser);
        mvc.perform(get("/api/listings/{id}/building-register",f.listingId()).header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
        mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),id).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":false}")).andExpect(status().isBadRequest());
        var started=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(buildingParser.parse(any())).thenAnswer(inv->{started.countDown();assertThat(release.await(10,TimeUnit.SECONDS)).isTrue();return extracted();});
        when(buildingParser.provider()).thenReturn("TEST");when(buildingParser.model()).thenReturn("synthetic");
        try {
            mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),id).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isAccepted());
            assertThat(started.await(10,TimeUnit.SECONDS)).isTrue();
            mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),id).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isConflict());
        } finally {release.countDown();}
        waitFor(id,"PARSED");
        var response=mvc.perform(get("/api/listings/{id}/building-register",f.listingId()).header("Authorization","Bearer "+f.token())).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        var data=mapper.readTree(response).path("data").path("latest");assertThat(data.path("result").path("violationMentioned").asBoolean()).isTrue();
        assertThat(data.path("requiredDocumentType").asString()).isEqualTo("EXCLUSIVE");
        assertThat(response).doesNotContain("storageKey","synthetic-key");
        mvc.perform(get("/api/listings/{id}/building-register/{review}/files/1",f.listingId(),id).header("Authorization","Bearer "+f.token())).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        mvc.perform(get("/api/listings/{id}/building-register/{review}/files/1",f.listingId(),id).header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
        assertThat(jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?",Long.class,f.listingId())).isEqualTo(risk);
        assertThat(jdbc.queryForObject("SELECT registry_snapshot_id FROM registry_documents WHERE listing_id=?",Long.class,f.listingId())).isEqualTo(snapshot);
        assertThat(jdbc.queryForObject("SELECT status FROM listings WHERE id=?",String.class,f.listingId())).isEqualTo("SAFETY_ANALYZED");
        assertThat(jdbc.queryForObject("SELECT consented_at IS NOT NULL FROM building_register_reviews WHERE id=?",Boolean.class,id)).isTrue();
        verify(registryParser,times(1)).parse(any());verify(buildingParser,times(1)).parse(any());verifyNoInteractions(evidenceClient);
    }
    @Test void violationEntersSafetyOnlyOnManualRunWithoutChangingExistingResultFlags() throws Exception {
        var f=fixture(true);
        Long original=jdbc.queryForObject("SELECT risk_analysis_id FROM listings WHERE id=?",Long.class,f.listingId());
        when(buildingParser.parse(any())).thenReturn(extracted());
        when(buildingParser.provider()).thenReturn("TEST");when(buildingParser.model()).thenReturn("synthetic");
        long marked=upload(f);
        mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),marked).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isAccepted());
        waitFor(marked,"PARSED");
        mvc.perform(get("/api/listings/{id}/safety-analysis",f.listingId()).header("Authorization","Bearer "+f.token()))
            .andExpect(jsonPath("$.data.analysis.id").value(original)).andExpect(jsonPath("$.data.reanalysisAvailable").value(false));
        jdbc.update("UPDATE building_identifications SET reason='TEST_REFRESH' WHERE id=(SELECT current_building_identification_id FROM listings WHERE id=?)",f.listingId());
        mvc.perform(get("/api/listings/{id}/building-register",f.listingId()).header("Authorization","Bearer "+f.token()))
            .andExpect(jsonPath("$.data.latest.stale").value(false));
        var response=mapper.readTree(mvc.perform(post("/api/listings/{id}/safety-analysis",f.listingId()).header("Authorization","Bearer "+f.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.highestSeverity").value("HIGH"))
            .andExpect(jsonPath("$.data.analysis.riskScore").value(50)).andExpect(jsonPath("$.data.factorCount").value(1))
            .andExpect(jsonPath("$.data.analysis.factors[0].factorType").value("BUILDING_VIOLATION"))
            .andExpect(jsonPath("$.data.analysis.factors[0].severity").value("HIGH"))
            .andExpect(jsonPath("$.data.reanalysisAvailable").value(false)).andReturn().getResponse().getContentAsString()).path("data");
        assertThat(response.path("analysis").path("id").asLong()).isNotEqualTo(original);
        long failed=upload(f);when(buildingParser.parse(any())).thenThrow(new RegistryParseException("PARSER_INVALID_OUTPUT"));
        mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),failed).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isAccepted());waitFor(failed,"FAILED");
        mvc.perform(post("/api/listings/{id}/safety-analysis",f.listingId()).header("Authorization","Bearer "+f.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.analysis.riskScore").value(50));
        var e=extracted().entries().getFirst(); reset(buildingParser);
        when(buildingParser.parse(any())).thenReturn(new Extraction(true,List.of(new Entry(e.fileIndex(),e.page(),e.documentType(),e.issuedAt(),e.jibunAddress(),e.roadAddress(),e.buildingName(),e.dongName(),e.floorLabel(),e.unitName(),e.purpose(),e.areaSquareMeters(),"NO_MARK_VISIBLE",List.of()))));
        long clean=upload(f);
        mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),clean).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isAccepted());waitFor(clean,"PARSED");
        mvc.perform(get("/api/listings/{id}/safety-analysis",f.listingId()).header("Authorization","Bearer "+f.token()))
            .andExpect(jsonPath("$.data.analysis.riskScore").value(50)).andExpect(jsonPath("$.data.reanalysisAvailable").value(false));
        mvc.perform(post("/api/listings/{id}/safety-analysis",f.listingId()).header("Authorization","Bearer "+f.token()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.analysis.riskScore").value(0)).andExpect(jsonPath("$.data.factorCount").value(0));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM risk_analyses WHERE id=?",Integer.class,original)).isEqualTo(1);
        verifyNoInteractions(evidenceClient);
    }

    @Test void replacementKeepsHistoryAndOldResponseCannotBecomeTheLatest() throws Exception {
        var f=fixture(false);long old=upload(f);var started=new CountDownLatch(1);var release=new CountDownLatch(1);
        when(buildingParser.parse(any())).thenAnswer(inv->{started.countDown();assertThat(release.await(10,TimeUnit.SECONDS)).isTrue();return extracted();});
        when(buildingParser.provider()).thenReturn("TEST");when(buildingParser.model()).thenReturn("synthetic");
        long latest;
        try {
            mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),old).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isAccepted());
            assertThat(started.await(10,TimeUnit.SECONDS)).isTrue();latest=upload(f);
        } finally {release.countDown();}
        waitFor(old,"SUPERSEDED");assertThat(store.history(f.listingId())).hasSize(2);assertThat(store.get(latest).status()).isEqualTo("UPLOADED");
        mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),old).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isConflict());
        store.start(latest);store.recover();assertThat(store.get(latest).errorCode()).isEqualTo("INTERRUPTED");
        verify(buildingParser,times(1)).parse(any());
    }
    @Test void rejectsNonPdfAndRetainsPrivateBuildingFilesWhenAListingIsHidden() throws Exception {
        var f=fixture(false);long before=countFiles();
        mvc.perform(multipart("/api/listings/{id}/building-register",f.listingId()).file(new MockMultipartFile("files","fake.pdf","application/pdf","not pdf".getBytes())).header("Authorization","Bearer "+f.token())).andExpect(status().isBadRequest());
        assertThat(store.history(f.listingId())).isEmpty();assertThat(countFiles()).isEqualTo(before);upload(f);long retained=countFiles();
        mvc.perform(delete("/api/listings/{id}",f.listingId()).header("Authorization","Bearer "+f.token())).andExpect(status().isNoContent());
        assertThat(countFiles()).isEqualTo(retained);
        mvc.perform(get("/api/listings/{id}/building-register",f.listingId()).header("Authorization","Bearer "+f.token())).andExpect(status().isNotFound());verifyNoInteractions(buildingParser);
    }
    @Test void missingExclusiveFailsAndRequiresNewFilesWithoutRepeatingThePaidCall() throws Exception {
        var f=fixture(false);long id=upload(f);var e=extracted().entries().getFirst(); reset(buildingParser);
        when(buildingParser.parse(any())).thenReturn(new Extraction(true,List.of(new Entry(1,1,"TITLE",e.issuedAt(),e.jibunAddress(),e.roadAddress(),e.buildingName(),e.dongName(),e.floorLabel(),null,e.purpose(),e.areaSquareMeters(),"NO_MARK_VISIBLE",List.of()))));
        mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),id).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isAccepted());
        waitFor(id,"FAILED");assertThat(store.get(id).errorCode()).isEqualTo("EXCLUSIVE_REGISTER_REQUIRED");
        mvc.perform(post("/api/listings/{id}/building-register/{review}/processing",f.listingId(),id).header("Authorization","Bearer "+f.token()).contentType("application/json").content("{\"consent\":true}")).andExpect(status().isConflict());
        assertThat(store.get(id).attemptCount()).isEqualTo(1);
        verify(buildingParser,times(1)).parse(any());verifyNoInteractions(registryParser,evidenceClient);
        long replacement=upload(f);assertThat(replacement).isGreaterThan(id);assertThat(store.get(replacement).status()).isEqualTo("UPLOADED");
    }
    @Test void combinedUploadAnalysisRequiresConsentAndIsIdempotentWithoutReparsingRegistry() throws Exception {
        var f=fixture(false);String request=UUID.randomUUID().toString();String other=token();
        mvc.perform(multipart("/api/listings/{id}/building-register/analysis",f.listingId()).file(new MockMultipartFile("files","building.pdf","application/pdf",pdf())).param("consent","false").param("requestId",request).header("Authorization","Bearer "+f.token())).andExpect(status().isBadRequest());
        mvc.perform(multipart("/api/listings/{id}/building-register/analysis",f.listingId()).file(new MockMultipartFile("files","building.pdf","application/pdf",pdf())).param("consent","true").param("requestId",request).header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
        assertThat(store.history(f.listingId())).isEmpty();
        when(buildingParser.parse(any())).thenReturn(extracted());when(buildingParser.provider()).thenReturn("TEST");when(buildingParser.model()).thenReturn("fixture");
        var response=mvc.perform(multipart("/api/listings/{id}/building-register/analysis",f.listingId()).file(new MockMultipartFile("files","building.pdf","application/pdf",pdf())).param("consent","true").param("requestId",request).header("Authorization","Bearer "+f.token())).andExpect(status().isAccepted()).andReturn();
        long id=mapper.readTree(response.getResponse().getContentAsString()).path("data").path("id").asLong();waitFor(id,"PARSED");
        mvc.perform(multipart("/api/listings/{id}/building-register/analysis",f.listingId()).file(new MockMultipartFile("files","building.pdf","application/pdf",pdf())).param("consent","true").param("requestId",request).header("Authorization","Bearer "+f.token())).andExpect(status().isAccepted());
        assertThat(store.history(f.listingId())).hasSize(1);verify(buildingParser,times(1)).parse(any());verifyNoInteractions(registryParser,evidenceClient);
        when(buildingParser.parse(any())).thenThrow(new RegistryParseException("PARSER_INVALID_OUTPUT"));
        mvc.perform(multipart("/api/listings/{id}/building-register/analysis",f.listingId()).file(new MockMultipartFile("files","replacement.pdf","application/pdf",pdf())).param("consent","true").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+f.token())).andExpect(status().isAccepted());
        var latest=store.history(f.listingId()).getFirst();waitFor(latest.id(),"FAILED");
        assertThat(store.get(id).status()).isEqualTo("PARSED");assertThat(store.get(id).result()).isNotNull();
        mvc.perform(get("/api/listings/{id}/building-register/{reviewId}/files/{index}",f.listingId(),id,1).header("Authorization","Bearer "+f.token())).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
    }
    private long countFiles() throws Exception {if(!Files.exists(STORAGE))return 0;try(var paths=Files.walk(STORAGE)){return paths.filter(Files::isRegularFile).count();}}
}
