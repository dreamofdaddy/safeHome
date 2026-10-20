package com.safehome.backend.domain.document.parser;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class RegistryAddressMatcherTest {
    @Test void acceptsEquivalentDetailNotation() {
        assertThat(RegistryAddressMatcher.same("서울특별시 테스트동 1-148 안전아파트 101동 202호",
            "서울특별시 테스트동 1-148 안전아파트 제101동 제2층 제202호")).isTrue();
        assertThat(RegistryAddressMatcher.same("서울 테스트로 1 (안전주택) 제2층 제202호",
            "서울 테스트로 1 안전주택 2층 202호")).isTrue();
        assertThat(RegistryAddressMatcher.same("서울 테스트로 1", "서울 테스트로 1")).isTrue();
    }
    @Test void rejectsDifferentLotsUnitsAndExplicitFloors() {
        assertThat(RegistryAddressMatcher.same("서울 테스트동 1-148", "서울 테스트동 11-48")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트로 1 101동 202호", "서울 테스트로 1 101동 제203호")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트로 1 101동 202호", "서울 테스트로 1 102동 제202호")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트로 1 지하2층 202호", "서울 테스트로 1 제2층 제202호")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트로 1", "서울 테스트로 1 202호")).isFalse();
    }
}
