package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import static org.assertj.core.api.Assertions.*;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.safehome.backend.domain.document.parser.RegistryParseException;

class BuildingRegisterEvaluatorTest {
    private final BuildingRegisterEvaluator evaluator=new BuildingRegisterEvaluator();
    private final Context context=new Context("서울 테스트로 1","제2층 제202호","APARTMENT",1L,2L,
            "서울 테스트로 1 2층 202호",null,"제202호",3L,"COMPLETED","MATCHED","202호",null);
    private final List<FileRef> files=List.of(new FileRef("synthetic.pdf",100,2,"synthetic","private"));
    private Entry entry(String unit,String purpose,String violation,List<Evidence> notes) {
        return new Entry(1,1,"EXCLUSIVE","2026-01-01",null,"서울 테스트로 1",null,null,"2층",unit,purpose,
                new BigDecimal("59.9"),violation,notes);
    }
    @Test void comparesDecoratedUnitAndApiWithoutCallingAnUnmarkedDocumentSafe() {
        var result=evaluator.evaluate(context,new Extraction(true,List.of(entry("제0202호","아파트","NO_MARK_VISIBLE",List.of()))),files);
        assertThat(result.checks().getFirst().unit()).isEqualTo("MATCHED");
        assertThat(result.checks().getFirst().apiUnit()).isEqualTo("MATCHED");
        assertThat(result.violationMentioned()).isFalse();assertThat(result.conclusion()).isEqualTo("DOCUMENTS_COMPARED");
    }
    @Test void sameRoomNumberInAnotherDongDoesNotCountAsMatching() {
        var c=new Context(context.address(),"101동 202호",context.buildingType(),1L,2L,
                context.registryAddress(),null,"202호",3L,"COMPLETED","MATCHED","202호",null);
        var e=entry("202호","아파트","NO_MARK_VISIBLE",List.of());
        var wrongDong=new Entry(e.fileIndex(),e.page(),e.documentType(),e.issuedAt(),e.jibunAddress(),e.roadAddress(),null,
                "102동",e.floorLabel(),e.unitName(),e.purpose(),e.areaSquareMeters(),e.violationStatus(),e.evidence());
        var r=evaluator.evaluate(c,new Extraction(true,List.of(wrongDong)),files);
        assertThat(r.checks().getFirst().dong()).isEqualTo("DIFFERENT");
        assertThat(r.conclusion()).isEqualTo("REVIEW_REQUIRED");
    }
    @Test void retainsMismatchAndViolationEvidenceWithoutInferringIllegalUse() {
        var result=evaluator.evaluate(context,new Extraction(true,List.of(entry("203호","근린생활시설","MARKED",
                List.of(new Evidence(2,"VIOLATION","위반건축물 표시"),new Evidence(2,"USE_CHANGE","과거 용도변경 기재"))))),files);
        assertThat(result.violationMentioned()).isTrue();assertThat(result.conclusion()).isEqualTo("VIOLATION_MENTIONED");
        assertThat(result.checks().getFirst().unit()).isEqualTo("DIFFERENT");
        assertThat(result.checks().getFirst().purpose()).isEqualTo("REVIEW_REQUIRED");
        assertThat(result.entries().getFirst().evidence()).hasSize(2);
    }
    @Test void anUnsupportedViolationClaimIsNotAcceptedWithoutEvidence() {
        var r=evaluator.evaluate(context,new Extraction(true,List.of(entry("202호","아파트","MARKED",List.of()))),files);
        assertThat(r.entries().getFirst().violationStatus()).isEqualTo("UNKNOWN");
        assertThat(r.violationMentioned()).isFalse();assertThat(r.conclusion()).isEqualTo("REVIEW_REQUIRED");
    }
    @Test void refusesWrongDocumentAndInventedEvidencePageAndMasksFullIdentifiers() {
        assertThatThrownBy(()->evaluator.evaluate(context,new Extraction(false,List.of()),files)).isInstanceOf(RegistryParseException.class);
        assertThatThrownBy(()->evaluator.evaluate(context,new Extraction(true,List.of(entry("202호","아파트","UNKNOWN",List.of(new Evidence(3,"OTHER","outside"))))),files))
                .isInstanceOf(RegistryParseException.class);
        var r=evaluator.evaluate(context,new Extraction(true,List.of(entry("202호","아파트","UNKNOWN",List.of(new Evidence(1,"OTHER","800101-1234567"))))),files);
        assertThat(r.entries().getFirst().evidence().getFirst().text()).isEqualTo("[개인정보 생략]");
    }
    private Entry asType(Entry e,String type) {
        return new Entry(e.fileIndex(),e.page(),type,e.issuedAt(),e.jibunAddress(),e.roadAddress(),e.buildingName(),e.dongName(),e.floorLabel(),e.unitName(),e.purpose(),e.areaSquareMeters(),e.violationStatus(),e.evidence());
    }
    @Test void titleOrSummaryCannotSubstituteForAnExclusiveRegister() {
        var e=entry("202호","아파트","NO_MARK_VISIBLE",List.of());
        for(String type:List.of("TITLE","SUMMARY","GENERAL"))
            assertThatThrownBy(()->evaluator.evaluate(context,new Extraction(true,List.of(asType(e,type))),files))
                .isInstanceOf(RegistryParseException.class).hasMessage("EXCLUSIVE_REGISTER_REQUIRED");
    }
    @Test void detachedAndMultiFamilyHomesRequireTheGeneralRegister() {
        var e=entry("202호","단독주택","NO_MARK_VISIBLE",List.of());
        for(String type:List.of("HOUSE","DETACHED_HOUSE","MULTI_FAMILY")) {
            var c=new Context(context.address(),context.detailAddress(),type,1L,2L,context.registryAddress(),null,"202호",null,null,null,null,null);
            assertThatThrownBy(()->evaluator.evaluate(c,new Extraction(true,List.of(e)),files))
                .isInstanceOf(RegistryParseException.class).hasMessage("GENERAL_REGISTER_REQUIRED");
            assertThat(evaluator.evaluate(c,new Extraction(true,List.of(asType(e,"GENERAL"))),files).entries()).hasSize(1);
        }
    }
    @Test void exclusiveMarkAndPartialReleaseNotesSurviveAnUnmarkedSupplement() {
        var e=entry("202호","아파트","MARKED",List.of(new Evidence(1,"VIOLATION","위반건축물 표시"),new Evidence(2,"VIOLATION","일부 해제: A부분 시정, B부분 미시정")));
        var r=evaluator.evaluate(context,new Extraction(true,List.of(asType(entry("202호","아파트","NO_MARK_VISIBLE",List.of()),"TITLE"),e)),files);
        assertThat(r.violationMentioned()).isTrue();assertThat(r.conclusion()).isEqualTo("VIOLATION_MENTIONED");
        assertThat(r.entries().get(1).violationStatus()).isEqualTo("MARKED");
        assertThat(r.entries().get(1).evidence().get(1).text()).contains("일부 해제","미시정");
    }

}
