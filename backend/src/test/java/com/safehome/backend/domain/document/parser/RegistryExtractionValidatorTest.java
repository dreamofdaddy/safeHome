package com.safehome.backend.domain.document.parser;

import com.safehome.backend.domain.listing.Listing;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

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
