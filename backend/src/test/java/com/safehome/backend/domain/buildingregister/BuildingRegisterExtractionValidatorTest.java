package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import static org.assertj.core.api.Assertions.*;
import com.safehome.backend.domain.document.parser.RegistryParseException;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class BuildingRegisterExtractionValidatorTest {
    private Entry entry(int file,String type,int page,List<Evidence> notes) {
        return new Entry(file,page,type,null,null,"테스트로 1",null,null,"19층","1901호","아파트",new BigDecimal("94.59"),"NO_MARK_VISIBLE",notes);
    }
    @Test void allSuppliedFilesMustBeRepresentedIncludingOptionalTitle() {
        var extraction=new Extraction(true,List.of(entry(2,"EXCLUSIVE",1,List.of())));
        assertThatThrownBy(()->BuildingRegisterExtractionValidator.validateCoverage(extraction,2))
            .isInstanceOf(RegistryParseException.class).hasMessage("PARSER_INCOMPLETE");
    }
    @Test void aTitleFileCannotBeCitedAsAnExclusiveFileEvenWithCompleteCoverage() {
        var extraction=new Extraction(true,List.of(entry(1,"EXCLUSIVE",1,List.of()),entry(2,"TITLE",1,List.of())));
        var sources=List.of(new BuildingRegisterExtractionValidator.Source(Set.of("TITLE"),2),new BuildingRegisterExtractionValidator.Source(Set.of("EXCLUSIVE"),2));
        assertThatThrownBy(()->BuildingRegisterExtractionValidator.validate(extraction,sources))
            .isInstanceOf(RegistryParseException.class).hasMessage("PARSER_SOURCE_MISMATCH");
    }
    @Test void preservesIndependentScopesAndChecksEvidencePageWithinItsOriginalFile() {
        var sources=List.of(new BuildingRegisterExtractionValidator.Source(Set.of("TITLE"),4),new BuildingRegisterExtractionValidator.Source(Set.of("EXCLUSIVE"),3));
        var valid=new Extraction(true,List.of(entry(1,"TITLE",1,List.of()),entry(2,"EXCLUSIVE",1,List.of(new Evidence(3,"VIOLATION","일부 해제, 나머지 미시정")))));
        assertThatCode(()->BuildingRegisterExtractionValidator.validate(valid,sources)).doesNotThrowAnyException();
        var invalid=new Extraction(true,List.of(entry(1,"TITLE",1,List.of()),entry(2,"EXCLUSIVE",1,List.of(new Evidence(4,"VIOLATION","wrong file page")))));
        assertThatThrownBy(()->BuildingRegisterExtractionValidator.validate(invalid,sources)).hasMessage("PARSER_INVALID_OUTPUT");
    }
    @Test void certificatesCombinedInOnePdfAreAllowed() {
        var extraction=new Extraction(true,List.of(entry(1,"TITLE",1,List.of()),entry(1,"EXCLUSIVE",3,List.of())));
        assertThatCode(()->BuildingRegisterExtractionValidator.validate(extraction,List.of(new BuildingRegisterExtractionValidator.Source(Set.of("TITLE","EXCLUSIVE"),4)))).doesNotThrowAnyException();
    }
    @Test void duplexRowsRetainBothFloorsAndAreasWithoutInventingAnotherUnit() {
        var first=entry(1,"EXCLUSIVE",1,List.of());
        var second=new Entry(1,1,"EXCLUSIVE",null,null,"테스트로 1",null,null,"20층","20층 제1901호","아파트",new BigDecimal("1.67"),"NO_MARK_VISIBLE",List.of());
        var context=new Context("테스트로 1","1901호","APARTMENT",null,null,null,null,"1901호",null,null,null,null,null);
        var result=new BuildingRegisterEvaluator().evaluate(context,new Extraction(true,List.of(first,second)),List.of(new FileRef("synthetic.pdf",100,1,"hash","key")));
        assertThat(result.entries()).hasSize(2);assertThat(result.entries().get(0).areaSquareMeters()).isEqualByComparingTo("94.59");
        assertThat(result.entries().get(1).areaSquareMeters()).isEqualByComparingTo("1.67");assertThat(result.conclusion()).isEqualTo("DOCUMENTS_COMPARED");
    }
}
