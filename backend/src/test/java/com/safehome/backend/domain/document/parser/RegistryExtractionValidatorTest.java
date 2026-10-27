package com.safehome.backend.domain.document.parser;

import com.safehome.backend.domain.listing.Listing;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegistryExtractionValidatorTest {
    private final RegistryExtractionValidator validator = new RegistryExtractionValidator();

    @Test
    void acceptsEitherLotNumberOrRoadNameAddressForTheSameRegistry() {
        ParsedRegistryDocument parsed = parsedRegistry();

        assertThatCode(() -> validator.validate(parsed,
            listing("서울특별시 강남구 역삼동 123-4 제101호"))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(parsed,
            listing("서울특별시 강남구 테헤란로 1 101호"))).doesNotThrowAnyException();
    }

    @Test
    void combinesBaseAndDetailAddress() {
        Listing listing = Listing.pendingAnalysis(1L, "서울특별시 강남구 역삼동 123-4",
            "테스트아파트", "APARTMENT", "제101호", "JEONSE", BigDecimal.valueOf(200_000_000), null, null, null, null);
        assertThatCode(() -> validator.validate(parsedRegistry(), listing)).doesNotThrowAnyException();
    }

    @Test
    void rejectsAnAddressThatMatchesNeitherRegistryAddress() {
        assertThatThrownBy(() -> validator.validate(parsedRegistry(),
            listing("서울특별시 서초구 다른로 99 101호")))
            .isInstanceOf(RegistryParseException.class)
            .hasMessage("REGISTRY_ADDRESS_MISMATCH");
    }

    @Test
    void supportsCorporateOwnerWithoutTreatingItsNumberAsResidentBirthData() {
        var base=parsedRegistry();
        for(String number: new String[]{"000000-0000000",null}) {
            var corporate=new ParsedRegistryDocument.Owner("합성 테스트법인",number,BigDecimal.valueOf(100),null,null,"OWNER",null,null,null,"CORPORATION");
            var parsed=new ParsedRegistryDocument(base.registryPropertyNumber(),base.address(),base.roadAddress(),base.buildingName(),base.unitNumber(),base.observedAt(),List.of(corporate),base.rights());
            var owner=validator.validate(parsed,listing(base.address())).owners().getFirst();
            assertThat(owner.entityType()).isEqualTo("CORPORATION");
            assertThat(owner.maskedIdentifier()).isEqualTo(number);
        }
    }
    @Test
    void rejectsCorporateShapedIdentifiersForIndividualsAndInvalidCorporateNumbers() {
        var base=parsedRegistry();
        for(var owner:List.of(
            new ParsedRegistryDocument.Owner("합성 개인","000000-0000000",BigDecimal.valueOf(100),null,null,"OWNER",null,null,null,"PERSON"),
            new ParsedRegistryDocument.Owner("합성 법인","000-00-00000",BigDecimal.valueOf(100),null,null,"OWNER",null,null,null,"CORPORATION"))) {
            var parsed=new ParsedRegistryDocument(base.registryPropertyNumber(),base.address(),base.roadAddress(),base.buildingName(),base.unitNumber(),base.observedAt(),List.of(owner),base.rights());
            assertThatThrownBy(()->validator.validate(parsed,listing(base.address()))).hasMessage("REGISTRY_OWNER_UNCERTAIN");
        }
    }

    @Test void acceptsActiveTrustAuctionAndOtherWhilePreservingOriginalNamesAndInactiveStatus() {
        var base=parsedRegistry();
        var rights=List.of(
            new ParsedRegistryDocument.Right("TRUST",null,null,1,null,"ACTIVE","신탁"),
            new ParsedRegistryDocument.Right("AUCTION",null,null,2,null,"ACTIVE","임의경매개시결정"),
            new ParsedRegistryDocument.Right("OTHER",null,null,3,null,"ACTIVE","가처분"),
            new ParsedRegistryDocument.Right("OTHER",null,null,4,null,"INACTIVE","가등기"));
        var parsed=new ParsedRegistryDocument(base.registryPropertyNumber(),base.address(),base.roadAddress(),base.buildingName(),base.unitNumber(),base.observedAt(),base.owners(),rights);
        var verified=validator.validate(parsed,listing(base.address()));
        assertThat(verified.rights()).hasSize(4);
        assertThat(verified.rights().get(2).sourceName()).isEqualTo("가처분");
        assertThat(verified.rights().get(3).status()).isEqualTo("INACTIVE");
    }
    @Test void stillRejectsUnrecognizedTypesAndUnknownValidity() {
        var base=parsedRegistry();
        for(var right:List.of(new ParsedRegistryDocument.Right("UNSUPPORTED",null,null,1,null,"ACTIVE"),
                new ParsedRegistryDocument.Right("OTHER",null,null,1,null,"UNKNOWN"))) {
            var parsed=new ParsedRegistryDocument(base.registryPropertyNumber(),base.address(),base.roadAddress(),base.buildingName(),base.unitNumber(),base.observedAt(),base.owners(),List.of(right));
            assertThatThrownBy(()->validator.validate(parsed,listing(base.address()))).hasMessage("REGISTRY_RIGHT_UNSUPPORTED");
        }
    }

    private Listing listing(String address) {
        return Listing.pendingAnalysis(1L, address, "테스트아파트", "APARTMENT", null,
            "JEONSE", BigDecimal.valueOf(200_000_000), null, null, null, null);
    }

    private ParsedRegistryDocument parsedRegistry() {
        return new ParsedRegistryDocument("1413-2026-000001",
            "서울특별시 강남구 역삼동 123-4 제101호",
            "서울특별시 강남구 테헤란로 1 101호", "테스트아파트", "101호",
            "2026-10-15T00:00:00",
            List.of(new ParsedRegistryDocument.Owner("홍길동", "800101-*******",
                BigDecimal.valueOf(100), null)),
            List.of());
    }
}
