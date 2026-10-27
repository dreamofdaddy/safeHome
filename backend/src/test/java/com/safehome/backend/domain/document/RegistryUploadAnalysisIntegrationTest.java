package com.safehome.backend.domain.document;
import com.safehome.backend.domain.document.parser.*;
import org.apache.pdfbox.pdmodel.*;
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
import java.util.*;
import java.math.BigDecimal;
import java.io.ByteArrayOutputStream;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties="safehome.auth.enforce=true")
@AutoConfigureMockMvc
class RegistryUploadAnalysisIntegrationTest {
 @Autowired MockMvc mvc;@Autowired ObjectMapper mapper;@Autowired JdbcTemplate jdbc;
 @MockitoBean RegistryDocumentParser parser;
 private static final String METADATA="""
 {"address":"서울특별시 강남구 테스트로 1","detailAddress":"101호","buildingType":"APARTMENT","transactionType":"JEONSE","depositAmount":200000000,
 "addressSearchEvidence":{"selectedType":"J","selectedAddress":"서울특별시 강남구 테스트로 1","jibunAddress":"서울특별시 강남구 테스트로 1","bcode":"1168010100","sigunguCode":"11680","noSelected":"N"}}
 """;
 private long create(String token)throws Exception {
   return mapper.readTree(mvc.perform(post("/api/listings").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(METADATA))
     .andExpect(status().isCreated()).andExpect(jsonPath("$.data.propertyId").doesNotExist()).andReturn().getResponse().getContentAsString()).path("data").path("id").asLong();
 }
 private MockMultipartFile pdf()throws Exception {try(var doc=new PDDocument();var out=new ByteArrayOutputStream()){doc.addPage(new PDPage());doc.save(out);return new MockMultipartFile("registryPdf","synthetic.pdf","application/pdf",out.toByteArray());}}
 private void waitStatus(long id,String status)throws Exception {for(int i=0;i<120;i++){String current=jdbc.queryForObject("SELECT status FROM registry_documents WHERE listing_id=? ORDER BY id DESC LIMIT 1",String.class,id);if(status.equals(current))return;Thread.sleep(100);}throw new AssertionError("analysis did not reach expected status");}
 @Test void activeTrustAuctionAndOtherPersistAndReachRiskAnalysisWithoutDiscardingUnknownRights()throws Exception {
   String token=signupAndLogin("extra-rights");long id=create(token);
   String number="1413-2026-"+String.format("%06d",Integer.parseInt(UUID.randomUUID().toString().substring(0,5),16)%1000000);
   when(parser.provider()).thenReturn("TEST");when(parser.model()).thenReturn("synthetic");
   when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,"서울특별시 강남구 테스트로 1 101호",null,null,"101호","2026-10-15T00:00:00",
     List.of(new ParsedRegistryDocument.Owner("합성소유자","800101-*******",BigDecimal.valueOf(100),null)),
     List.of(new ParsedRegistryDocument.Right("TRUST",null,null,3,null,"ACTIVE","신탁"),
       new ParsedRegistryDocument.Right("AUCTION",null,null,2,null,"ACTIVE","임의경매개시결정"),
       new ParsedRegistryDocument.Right("OTHER",null,null,1,null,"ACTIVE","가처분"),
       new ParsedRegistryDocument.Right("AUCTION",null,null,4,null,"INACTIVE","강제경매개시결정"))));
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());
   waitStatus(id,"PARSED");
   long snapshot=jdbc.queryForObject("SELECT registry_snapshot_id FROM registry_documents WHERE listing_id=?",Long.class,id);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_rights WHERE registry_snapshot_id=?",Integer.class,snapshot)).isEqualTo(4);
   assertThat(jdbc.queryForObject("SELECT source_name FROM registry_rights WHERE registry_snapshot_id=? AND right_type='OTHER'",String.class,snapshot)).isEqualTo("가처분");
   mvc.perform(get("/api/listings/{id}/registry-facts",id).header("Authorization","Bearer "+token)).andExpect(status().isOk())
      .andExpect(jsonPath("$.data.rights.length()").value(3))
      .andExpect(jsonPath("$.data.rights[0].priority").value(1)).andExpect(jsonPath("$.data.rights[0].sourceName").value("가처분"))
      .andExpect(jsonPath("$.data.rights[1].priority").value(2)).andExpect(jsonPath("$.data.rights[2].priority").value(3));
   mvc.perform(post("/api/listings/{id}/safety-analysis",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.factorCount").value(3));
   var factors=jdbc.queryForList("SELECT f.factor_type,f.severity,f.score FROM risk_factors f JOIN risk_analyses a ON a.id=f.risk_analysis_id WHERE a.registry_snapshot_id=? ORDER BY f.factor_type",snapshot);
   assertThat(factors).hasSize(3);
   assertThat(factors.stream().map(f->f.get("factor_type")).toList()).containsExactly("AUCTION","OTHER","TRUST");
   assertThat(factors.stream().map(f->f.get("severity")).toList()).containsExactly("CRITICAL","HIGH","CRITICAL");
   assertThat(jdbc.queryForObject("SELECT risk_score FROM risk_analyses WHERE registry_snapshot_id=?",BigDecimal.class,snapshot)).isEqualByComparingTo("100");
   assertThat(jdbc.queryForObject("SELECT count(*) FROM building_identifications WHERE listing_id=?",Integer.class,id)).isZero();
   verify(parser,times(1)).parse(any());
 }
 @Test void corporateOwnerIsStoredSeparatelyAndIdentifiersNeverAppearInFacts()throws Exception {
   String token=signupAndLogin("corporate");long id=create(token);
   String number="1413-2026-"+String.format("%06d",Math.abs(UUID.randomUUID().hashCode())%1000000);
   when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,"서울특별시 강남구 테스트로 1 101호",null,null,"101호","2026-10-15T00:00:00",
     List.of(new ParsedRegistryDocument.Owner("합성 테스트법인","000000-0000000",BigDecimal.valueOf(100),null,"전부","OWNER","1",null,null,"CORPORATION")),List.of()));
   when(parser.provider()).thenReturn("TEST");when(parser.model()).thenReturn("synthetic");
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());waitStatus(id,"PARSED");
   assertThat(jdbc.queryForObject("SELECT count(*) FROM property_owners o JOIN registry_documents d ON d.registry_snapshot_id=o.registry_snapshot_id WHERE d.listing_id=? AND o.owner_entity_type='CORPORATION' AND o.owner_rrn_encrypted IS NULL AND o.owner_rrn_birth6_hash IS NULL AND o.corporate_registration_encrypted IS NOT NULL",Integer.class,id)).isEqualTo(1);
   var body=mvc.perform(get("/api/listings/{id}/registry-facts",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.owners[0].entityType").value("CORPORATION")).andReturn().getResponse().getContentAsString();
   assertThat(body).doesNotContain("000000-0000000","maskedIdentifier","corporateRegistrationEncrypted");
   mvc.perform(post("/api/listings/{id}/safety-analysis",id).header("Authorization","Bearer "+token)).andExpect(status().isOk());
   assertThat(jdbc.queryForObject("SELECT count(*) FROM building_identifications WHERE listing_id=?",Integer.class,id)).isZero();
 }
 @Test void createsWithoutPdfAndRejectsInvalidOrUnauthorizedUploadsWithoutChangingState()throws Exception {
   String token=signupAndLogin("separate-create");long id=create(token);
   assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_documents WHERE listing_id=?",Integer.class,id)).isZero();
   mvc.perform(post("/api/listings/{id}/safety-analysis",id).header("Authorization","Bearer "+token)).andExpect(status().isConflict());
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","false").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
   String other=signupAndLogin("other-upload");
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+other)).andExpect(status().isForbidden());
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(new MockMultipartFile("registryPdf","bad.pdf","application/pdf","not pdf".getBytes())).param("consent","true").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
   assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_documents WHERE listing_id=?",Integer.class,id)).isZero();
   assertThat(jdbc.queryForObject("SELECT status FROM listings WHERE id=?",String.class,id)).isEqualTo("READY_FOR_ANALYSIS");
   verifyNoInteractions(parser);
 }
 @Test void analyzesOncePreservesSourceDetailsAndKeepsHistoryWhenReplacementFails()throws Exception {
   String token=signupAndLogin("new-upload");long id=create(token);String request=UUID.randomUUID().toString();
   String number="1413-2026-"+String.format("%06d",Integer.parseInt(UUID.randomUUID().toString().substring(0,5),16)%1000000);
   when(parser.provider()).thenReturn("TEST");when(parser.model()).thenReturn("fixture");
   when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,"서울특별시 강남구 테스트로 1 101호",null,null,"101호","2026-10-13T10:00:00",
     List.of(new ParsedRegistryDocument.Owner("합성소유자","800101-*******",null,null,null,"OWNER","3-1","합성 소유자 주소",new BigDecimal("123456789")),
       new ParsedRegistryDocument.Owner("합성공유자","810101-*******",null,null,"12분의 1","CO_OWNER","5","합성 공유자 주소",null)),
     List.of(new ParsedRegistryDocument.Right("LEASEHOLD","합성권리자",new BigDecimal("50000000"),5,"2020-01-01T00:00:00","ACTIVE"))));
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",request).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());waitStatus(id,"PARSED");
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",request).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());
   assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_documents WHERE listing_id=?",Integer.class,id)).isEqualTo(1);
   verify(parser,times(1)).parse(any());
   mvc.perform(get("/api/listings/{id}/registry-facts",id).header("Authorization","Bearer "+token)).andExpect(status().isOk())
    .andExpect(jsonPath("$.data.owners[0].priorityNumber").value("3-1")).andExpect(jsonPath("$.data.owners[0].ownerAddress").value("합성 소유자 주소"))
    .andExpect(jsonPath("$.data.owners[0].acquisitionAmount").value(123456789)).andExpect(jsonPath("$.data.owners[1].ownershipType").value("CO_OWNER"))
    .andExpect(jsonPath("$.data.owners[1].ownershipShare").value("12분의 1")).andExpect(jsonPath("$.data.owners[1].maskedIdentifier").doesNotExist());
   mvc.perform(post("/api/listings/{id}/safety-analysis",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.factorCount").value(1));
   when(parser.parse(any())).thenReturn(new ParsedRegistryDocument(number,"서울특별시 강남구 다른길 2 999호",null,null,"999호","2026-10-13T10:00:00",List.of(),List.of()));
   mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());waitStatus(id,"FAILED");
   mvc.perform(get("/api/listings/{id}/registry-processing",id).header("Authorization","Bearer "+token)).andExpect(status().isOk())
    .andExpect(jsonPath("$.data.addressMismatch.registeredAddress").value("서울특별시 강남구 테스트로 1 101호"))
    .andExpect(jsonPath("$.data.addressMismatch.lotAddress").value("서울특별시 강남구 다른길 2 999호"))
    .andExpect(jsonPath("$.data.addressMismatch.directory").doesNotExist());
   String deniedToken=signupAndLogin("private-evidence");
   mvc.perform(get("/api/listings/{id}/registry-processing",id).header("Authorization","Bearer "+deniedToken)).andExpect(status().isForbidden());
   mvc.perform(get("/api/listings/{id}/registry-facts",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.owners.length()").value(2));
   mvc.perform(get("/api/listings/{id}/safety-analysis",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.factorCount").value(1));
   assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_documents WHERE listing_id=?",Integer.class,id)).isEqualTo(2);
   var history=mapper.readTree(mvc.perform(get("/api/listings/{id}/registry-documents",id).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.length()").value(2)).andReturn().getResponse().getContentAsString()).path("data");
   long earlier=history.get(1).path("id").asLong();
   mvc.perform(get("/api/listings/{id}/registry-documents",id).header("Authorization","Bearer "+deniedToken)).andExpect(status().isForbidden());
   mvc.perform(get("/api/listings/{id}/registry-facts",id).param("documentId",Long.toString(earlier)).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("$.data.documentId").value(earlier));
   mvc.perform(get("/api/registry-documents/{id}/file",earlier).header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
   mvc.perform(get("/api/registry-documents/{id}/file",earlier).header("Authorization","Bearer "+deniedToken)).andExpect(status().isForbidden());

   mvc.perform(post("/api/listings/{id}/safety-analysis",id).header("Authorization","Bearer "+token)).andExpect(status().isConflict());
 }
 @Test void serializesActiveUploadsAndTreatsRepeatedRequestAsTheSameJob()throws Exception {
   String token=signupAndLogin("active-upload");long id=create(token);String request=UUID.randomUUID().toString();
   var entered=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
   when(parser.parse(any())).thenAnswer(invocation->{entered.countDown();release.await(20,java.util.concurrent.TimeUnit.SECONDS);throw new RegistryParseException("PARSER_TIMEOUT");});
   when(parser.provider()).thenReturn("TEST");when(parser.model()).thenReturn("fixture");
   try {
     mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId","bad-uuid").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
     mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",request).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());
     assertThat(entered.await(10,java.util.concurrent.TimeUnit.SECONDS)).isTrue();
     mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",request).header("Authorization","Bearer "+token)).andExpect(status().isAccepted());
     mvc.perform(multipart("/api/listings/{id}/registry-analysis",id).file(pdf()).param("consent","true").param("requestId",UUID.randomUUID().toString()).header("Authorization","Bearer "+token)).andExpect(status().isConflict());
     assertThat(jdbc.queryForObject("SELECT count(*) FROM registry_documents WHERE listing_id=?",Integer.class,id)).isEqualTo(1);
   } finally {release.countDown();}
   waitStatus(id,"FAILED");verify(parser,times(1)).parse(any());
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

}
