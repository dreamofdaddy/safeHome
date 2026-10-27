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
    @Test void acceptsAdministrativeNameAliasesOnlyAtTheLeadingToken() {
        assertThat(RegistryAddressMatcher.same("서울 테스트구 테스트동 1 테스트빌 2층 202호",
            "서울특별시 테스트구 테스트동 1 테스트빌 제2층 제202호")).isTrue();
        assertThat(RegistryAddressMatcher.same("경기 테스트시 테스트로 1 202호",
            "경기도 테스트시 테스트로 1 제2층 제202호")).isTrue();
        assertThat(RegistryAddressMatcher.same("강원도 테스트시 테스트로 1 202호",
            "강원특별자치도 테스트시 테스트로 1 202호")).isTrue();
        assertThat(RegistryAddressMatcher.same("전북 테스트시 테스트로 1 202호",
            "전라북도 테스트시 테스트로 1 202호")).isTrue();
    }
    @Test void regionalAliasesNeverAuthorizeDifferentBuildingsOrProvinces() {
        assertThat(RegistryAddressMatcher.same("서울 테스트구 테스트동 1 테스트제빌 2층 202호",
            "서울특별시 테스트구 테스트동 1 테스트쉐빌 제2층 제202호")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트구 테스트동 1 202호",
            "부산광역시 테스트구 테스트동 1 202호")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트구 테스트동 1 202호",
            "서울특별시 테스트구 테스트동 2 202호")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트구 테스트동 1 202호",
            "서울특별시 테스트구 테스트동 1 203호")).isFalse();
    }
    @Test void neverReplacesCityLikeWordsInsideBuildingOrStreetNames() {
        assertThat(RegistryAddressMatcher.same("서울동 테스트로 1 202호",
            "서울특별시동 테스트로 1 202호")).isFalse();
        assertThat(RegistryAddressMatcher.same("서울 테스트로 1 서울빌 202호",
            "서울특별시 테스트로 1 서울특별시빌 202호")).isFalse();
    }

}
