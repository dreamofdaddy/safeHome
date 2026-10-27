package com.safehome.backend.integration.building;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.domain.document.parser.VerifiedRegistryDocument;
import com.safehome.backend.domain.listing.AddressSearchEvidence;
import com.safehome.backend.domain.listing.Listing;
import java.util.List;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BuildingQueryFactoryTest {
    private final BuildingQueryFactory factory = new BuildingQueryFactory();

    @Test
    void listingQueryIsIndependentOfRegistryAndRequiresExplicitFloor() {
        var ready = factory.buildListing(listing("160동 지하2층 101호", evidence()));
        assertThat(ready.status()).isEqualTo(BuildingQueryFactory.Status.READY);
        assertThat(ready.query().floorGroundType()).isEqualTo(BuildingQuery.Ground.BELOW_GROUND);
        assertThat(ready.query().floorNumber()).isEqualTo(2);
        assertThat(factory.buildListing(listing("160동 101호", evidence())).reason()).isEqualTo("EXPLICIT_FLOOR_AND_UNIT_REQUIRED");
        assertThat(factory.buildListing(listing("160동 1층 101호", null)).reason()).isEqualTo("CONFIRMED_JIBUN_REQUIRED");
    }

    @Test
    void postcodeEvidenceChecksCodeAndSelectedAddress() {
        assertThat(evidence().validatedFor("서울 송파구 잠실동 19").bcode()).isEqualTo("1171010100");
        assertThatThrownBy(() -> evidence().validatedFor("서울 송파구 잠실동 20"))
                .isInstanceOf(BusinessException.class);
        assertThatThrownBy(() -> new AddressSearchEvidence("J", "서울 송파구 잠실동 19",
                "서울 송파구 잠실동 19", "1171010100", "11530", "N")
                .validatedFor("서울 송파구 잠실동 19")).isInstanceOf(BusinessException.class);
    }

    @Test
    void listingMayOmitFloorWhenRegistryExplicitlyProvidesIt() {
        var result = factory.build(listing("제160동 제0101호", evidence()),
                registry("서울 송파구 잠실동 19 제160동 지상1층 101호", "101호"));
        assertThat(result.status()).isEqualTo(BuildingQueryFactory.Status.READY);
        assertThat(result.query()).extracting(BuildingQuery::sigunguCd, BuildingQuery::bjdongCd,
                BuildingQuery::platGbCd, BuildingQuery::bun, BuildingQuery::ji,
                BuildingQuery::dongName, BuildingQuery::floorGroundType,
                BuildingQuery::floorNumber, BuildingQuery::unitNumber)
                .containsExactly("11710", "10100", "0", "0019", "0000", "160",
                        BuildingQuery.Ground.ABOVE_GROUND, 1, "101");
        assertThat(result.listingParts().rawDong()).isEqualTo("제160동");
        assertThat(result.listingParts().rawUnit()).isEqualTo("제0101호");
        assertThat(result.registryParts().rawFloor()).isEqualTo("지상1층");
        assertThat(result.registryParts().rawUnit()).isEqualTo("101호");
    }

    @Test
    void neverInfersFloorFromRoomOrJicheung() {
        var listing = listing("160동 101호", evidence());
        assertThat(factory.build(listing, registry("서울 송파구 잠실동 19 160동 101호", "101호")).status())
                .isEqualTo(BuildingQueryFactory.Status.INPUT_INCOMPLETE);
        assertThat(factory.build(listing, registry("서울 송파구 잠실동 19 160동 지층 101호", "101호")).status())
                .isEqualTo(BuildingQueryFactory.Status.INPUT_INCOMPLETE);
        var basement = factory.build(listing("160동 지하2층 101호", evidence()),
                registry("서울 송파구 잠실동 19 160동 지층 101호", "101호"));
        assertThat(basement.status()).isEqualTo(BuildingQueryFactory.Status.READY);
        assertThat(basement.query().floorGroundType()).isEqualTo(BuildingQuery.Ground.BELOW_GROUND);
        assertThat(basement.query().floorNumber()).isEqualTo(2);
    }

    @Test
    void rejectsConflictsAndAmbiguousJibun() {
        assertThat(factory.build(listing("160동 2층 101호", evidence()),
                registry("서울 송파구 잠실동 19 160동 1층 101호", "101호")).status())
                .isEqualTo(BuildingQueryFactory.Status.INPUT_CONFLICT);
        assertThat(factory.build(listing("160동 지하1층 101호", evidence()),
                registry("서울 송파구 잠실동 19 160동 지상1층 101호", "101호")).status())
                .isEqualTo(BuildingQueryFactory.Status.INPUT_CONFLICT);
        assertThat(factory.build(listing("160동 1층 101호", evidence()),
                registry("서울 송파구 잠실동 19 160동 1층 202호", "202호")).status())
                .isEqualTo(BuildingQueryFactory.Status.INPUT_CONFLICT);
        assertThat(factory.build(listing("160동 1층 101호", null),
                registry("서울 송파구 잠실동 19 160동 1층 101호", "101호")).status())
                .isEqualTo(BuildingQueryFactory.Status.INPUT_INCOMPLETE);
        var ambiguous = new AddressSearchEvidence("J", "서울 송파구 잠실동 19",
                "서울 송파구 잠실동 19", "1171010100", "11710", "Y");
        assertThat(factory.build(listing("160동 1층 101호", ambiguous),
                registry("서울 송파구 잠실동 19 160동 1층 101호", "101호")).status())
                .isEqualTo(BuildingQueryFactory.Status.INPUT_INCOMPLETE);
    }

    @Test
    void parsesConfirmedMountainLotWithoutRoadNumberGuess() {
        var mountain = new AddressSearchEvidence("J", "서울 송파구 잠실동 산 19-2",
                "서울 송파구 잠실동 산 19-2", "1171010100", "11710", "N");
        var result = factory.build(listing("B2층 제1203호", mountain),
                registry("서울 송파구 잠실동 산 19-2 지하2층 1203호", "1203"));
        assertThat(result.status()).isEqualTo(BuildingQueryFactory.Status.READY);
        assertThat(result.query().platGbCd()).isEqualTo("1");
        assertThat(result.query().bun()).isEqualTo("0019");
        assertThat(result.query().ji()).isEqualTo("0002");
        assertThat(result.query().unitNumber()).isEqualTo("1203");
        assertThat(result.listingParts().rawUnit()).isEqualTo("제1203호");
    }

    @Test
    void roadSelectionUsesItsConfirmedJibunRatherThanRoadBuildingNumber() {
        var road = new AddressSearchEvidence("R", "서울 송파구 올림픽로 99",
                "서울 송파구 잠실동 19", "1171010100", "11710", "N");
        var result = factory.build(listing("160동 101호", road),
                registry("서울 송파구 올림픽로 99 160동 1층 101호", "101호"));
        assertThat(result.status()).isEqualTo(BuildingQueryFactory.Status.READY);
        assertThat(result.query().bun()).isEqualTo("0019");
        assertThat(result.query().ji()).isEqualTo("0000");
    }

    private static AddressSearchEvidence evidence() {
        return new AddressSearchEvidence("J", "서울 송파구 잠실동 19",
                "서울 송파구 잠실동 19", "1171010100", "11710", "N");
    }
    private static Listing listing(String detail, AddressSearchEvidence evidence) {
        String address = evidence == null ? "서울 송파구 잠실동 19" : evidence.selectedAddress();
        var listing = Listing.pendingAnalysis(1L, address, "잠실엘스", "APARTMENT", detail,
                "JEONSE", null, null, null, null, null);
        listing.setAddressSearchEvidence(evidence);
        return listing;
    }
    private static VerifiedRegistryDocument registry(String address, String unit) {
        return new VerifiedRegistryDocument("1234-1234-123456", address, null, "잠실엘스", unit,
                null, List.of(), List.of());
    }
}
