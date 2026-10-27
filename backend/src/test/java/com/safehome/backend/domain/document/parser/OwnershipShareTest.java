package com.safehome.backend.domain.document.parser;

import com.safehome.backend.domain.listing.Listing;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class OwnershipShareTest {
    @Test void preservesThePrintedFractionAndCalculatesPercentWithoutMistakingNumeratorForPercent() {
        assertThat(OwnershipShare.percentage("12분의 1")).isEqualByComparingTo("8.33333333");
        assertThat(OwnershipShare.percentage("1/12")).isEqualByComparingTo("8.33333333");
        assertThat(OwnershipShare.percentage("지분 12분의 7")).isEqualByComparingTo("58.33333333");
        assertThat(OwnershipShare.percentage(null)).isNull();
    }
    @Test void refusesImpossibleFractionsRatherThanInventingTheRemainder() {
        for(String value:List.of("0분의 1","12분의 0","12분의 13"))
            assertThatThrownBy(()->OwnershipShare.percentage(value)).hasMessage("REGISTRY_OWNER_RATIO_INVALID");
    }
    @Test void validatorKeepsEachCoOwnerAndTheOriginalFractionWithoutAssigningAnUnstatedRemainder() {
        var owners=java.util.stream.IntStream.range(0,5).mapToObj(i->
            new ParsedRegistryDocument.Owner("합성공유자"+i,"800101-*******",BigDecimal.ONE,null,"12분의 1")).toList();
        var parsed=new ParsedRegistryDocument("1413-2026-000099","서울 테스트로 1",null,null,null,
            "2026-10-23T00:00:00",owners,List.of());
        var listing=Listing.pendingAnalysis(1L,"서울 테스트로 1",null,"APARTMENT",null,"JEONSE",BigDecimal.ONE,null,null,null,null);
        var verified=new RegistryExtractionValidator().validate(parsed,listing);
        assertThat(verified.owners()).hasSize(5);
        assertThat(verified.owners()).allSatisfy(o->{assertThat(o.ownershipShare()).isEqualTo("12분의 1");assertThat(o.ownershipRatio()).isEqualByComparingTo("8.33333333");});
    }
}
