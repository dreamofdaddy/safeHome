package com.safehome.integration.building;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public final class CoreChecks {
    private static int checks;
    private static final BuildingQuery VILLA = new BuildingQuery("11530", "10700", "0",
            "0355", "0018", null, BuildingQuery.Ground.ABOVE_GROUND, 1, "101");
    public static void main(String[] args) {
        run();
        System.out.println("PASS: " + checks + " core checks (synthetic fixtures; no live API)");
    }
    public static void run() {
        checks = 0;
        BuildingMatcher matcher = new BuildingMatcher();
        BuildingUnit villa = unit("1018152541", "1018112940", null, "1층 제0101호");
        var result = matcher.match(VILLA, List.of(villa), true, Instant.now());
        check(result.status() == Resolution.Status.MATCHED, "villa");
        check(result.candidates().getFirst().unitName().equals("1층 제0101호"), "preserve original");
        for (String raw : List.of("101호", "제101호", "0101호", "1층 101호", "제1층 제101호")) {
            var equivalent = matcher.match(VILLA, List.of(unit("same", "parent", null, raw)),
                    true, Instant.now());
            check(equivalent.status() == Resolution.Status.MATCHED
                    && equivalent.candidates().getFirst().unitName().equals(raw),
                    "equivalent unit label: " + raw);
        }
        for (int number : List.of(202, 1203)) {
            int floor = number == 202 ? 2 : 12;
            String canonical = String.valueOf(number);
            var query = query(BuildingQuery.Ground.ABOVE_GROUND, floor, "제0" + canonical + "호");
            for (String raw : List.of(canonical + "호", "제" + canonical + "호",
                    "0" + canonical + "호", "제0" + canonical + "호",
                    "제" + floor + "층 제0" + canonical + "호")) {
                var candidate = matcher.match(query,
                        List.of(unitAt("unit-" + number, BuildingQuery.Ground.ABOVE_GROUND, floor, raw)),
                        true, Instant.now());
                check(candidate.status() == Resolution.Status.MATCHED
                        && candidate.candidates().getFirst().unitName().equals(raw),
                        "normalized " + number + " and preserved raw label: " + raw);
            }
        }
        check(BuildingMatcher.normalize("1층", "층")
                .equals(BuildingMatcher.normalize("제1층", "층")), "floor decoration");
        check(matcher.match(VILLA, List.of(unit("conflict", "parent", null, "2층 101호")),
                true, Instant.now()).status() == Resolution.Status.UNVERIFIABLE, "unit label floor conflict");
        check(matcher.match(VILLA, List.of(unit("conflict", "parent", null, "지하1층 101호")),
                true, Instant.now()).status() == Resolution.Status.UNVERIFIABLE, "unit label ground conflict");
        var floorMissing = new BuildingUnit("no-floor", "parent", "11530", "10700", "0", "0355", "0018",
                "address", null, null, BuildingQuery.Ground.ABOVE_GROUND, null, "101호", "villa");
        check(matcher.match(VILLA, List.of(floorMissing), true, Instant.now()).status()
                == Resolution.Status.UNVERIFIABLE, "unit 101 cannot supply missing API floor");
        var floorUnavailable = matcher.match(VILLA, List.of(floorMissing), true, Instant.now());
        check("CANDIDATE_FLOOR_NUMBER_UNAVAILABLE".equals(floorUnavailable.reason()),
                "missing upstream floor has a safe specific diagnostic");
        check(floorUnavailable.collectionComplete() && floorUnavailable.candidates().isEmpty(),
                "missing floor is completed collection without a confirmed match");
        check(matcher.match(VILLA, List.of(villa, floorMissing), true, Instant.now()).status()
                == Resolution.Status.UNVERIFIABLE, "a valid unit cannot override a competitor with missing floor");
        var floorAndGroundMissing = unitAt("incomplete", null, null, "101호");
        check("UNINTERPRETABLE_CANDIDATE".equals(matcher.match(VILLA,
                List.of(floorAndGroundMissing), true, Instant.now()).reason()),
                "multiple missing fields keep the general diagnostic");
        var zeroFloorPage = BuildingHubClient.parse(("<response><header><resultCode>00</resultCode>"
                + "</header><body><pageNo>1</pageNo><totalCount>1</totalCount><items><item>"
                + "<mgmBldrgstPk>synthetic-zero-floor</mgmBldrgstPk><flrGbCd>20</flrGbCd>"
                + "<flrNo>0</flrNo><hoNm>202호</hoNm></item></items></body></response>").getBytes(StandardCharsets.UTF_8));
        check(zeroFloorPage.units().getFirst().floorNumber() == null,
                "upstream zero floor is unavailable and never inferred from room 202");
        var secondFloor = new BuildingUnit("second", "parent", "11530", "10700", "0", "0355", "0018",
                "address", null, null, BuildingQuery.Ground.ABOVE_GROUND, 2, "101호", "villa");
        check(matcher.match(VILLA, List.of(secondFloor), true, Instant.now()).status()
                == Resolution.Status.NOT_FOUND, "unit 101 cannot imply first floor");
        var basementTwo = query(BuildingQuery.Ground.BELOW_GROUND, 2, "101호");
        for (String raw : List.of("지층 101호", "제지층 제0101호", "지하2층 101호",
                "제지하2층 제101호", "B2층 101호", "제B2층 제0101호")) {
            var candidate = matcher.match(basementTwo,
                    List.of(unitAt("basement", BuildingQuery.Ground.BELOW_GROUND, 2, raw)),
                    true, Instant.now());
            check(candidate.status() == Resolution.Status.MATCHED
                    && candidate.candidates().getFirst().unitName().equals(raw),
                    "basement form and original label: " + raw);
        }
        check(matcher.match(basementTwo,
                List.of(unitAt("basement-one", BuildingQuery.Ground.BELOW_GROUND, 1, "지층 101호")),
                true, Instant.now()).status() == Resolution.Status.NOT_FOUND,
                "지층 alone does not imply basement floor one");
        for (String raw : List.of("지층 101호", "지하2층 101호", "B2층 101호")) {
            check(matcher.match(basementTwo,
                    List.of(unitAt("above", BuildingQuery.Ground.ABOVE_GROUND, 2, raw)),
                    true, Instant.now()).status() == Resolution.Status.UNVERIFIABLE,
                    "basement label conflicts with API ground: " + raw);
            check(matcher.match(basementTwo,
                    List.of(unitAt("missing-floor", BuildingQuery.Ground.BELOW_GROUND, null, raw)),
                    true, Instant.now()).status() == Resolution.Status.UNVERIFIABLE,
                    "basement label cannot supply missing API floor: " + raw);
            check(matcher.match(basementTwo,
                    List.of(unitAt("missing-ground", null, 2, raw)),
                    true, Instant.now()).status() == Resolution.Status.UNVERIFIABLE,
                    "basement label cannot supply missing API ground: " + raw);
        }
        for (String raw : List.of("지하1층 101호", "B1층 101호")) {
            check(matcher.match(basementTwo,
                    List.of(unitAt("wrong-floor", BuildingQuery.Ground.BELOW_GROUND, 2, raw)),
                    true, Instant.now()).status() == Resolution.Status.UNVERIFIABLE,
                    "basement label conflicts with API floor: " + raw);
        }
        check(matcher.match(VILLA, List.of(), true, Instant.now()).status() == Resolution.Status.NOT_FOUND, "empty complete");
        check(matcher.match(VILLA, List.of(villa), false, Instant.now()).status() == Resolution.Status.UNVERIFIABLE, "partial collection");
        check(matcher.match(VILLA, List.of(villa, unit("2", "parent", null, "101")), true,
                Instant.now()).status() == Resolution.Status.AMBIGUOUS, "duplicate candidates");
        var underground = new BuildingUnit("b", "p", "11530", "10700", "0", "0355", "0018",
                "address", null, null, BuildingQuery.Ground.BELOW_GROUND, 1, "101", "villa");
        check(matcher.match(VILLA, List.of(underground), true, Instant.now()).status() == Resolution.Status.NOT_FOUND, "basement distinct");
        var unknown = new BuildingUnit("u", "p", "11530", "10700", "0", "0355", "0018",
                "address", null, null, null, 1, "101", "villa");
        check(matcher.match(VILLA, List.of(villa, unknown), true, Instant.now()).status()
                == Resolution.Status.UNVERIFIABLE, "unknown competing candidate");
        check(BuildingMatcher.normalize("Ａ-１０１호", "호").equals("A-101"), "unicode normalization");
        check(!BuildingMatcher.normalize("A-101", "호").equals(BuildingMatcher.normalize("101", "호")), "retain prefix");
        expect(IllegalArgumentException.class, () -> new BuildingQuery("11530", "10700", "0", "355", "18",
                null, BuildingQuery.Ground.ABOVE_GROUND, 1, "101"), "reject unpadded request");

        AtomicInteger pages = new AtomicInteger();
        List<BuildingUnit> apartment = new ArrayList<>();
        for (int i = 0; i < 5678; i++) {
            int dong = 101 + (i % 72);
            String id = i == 59 ? "10251100215481" : "apt-" + i;
            String ho = i == 59 ? "101" : String.valueOf(200 + i);
            apartment.add(new BuildingUnit(id, "parent-" + dong, "11710", "10100", "0", "0019", "0000",
                    "서울 송파구 잠실동 19", null, dong + "동", BuildingQuery.Ground.ABOVE_GROUND, 1, ho, "아파트"));
        }
        var all = new AllPages().collect(page -> {
            pages.incrementAndGet();
            int start = (page - 1) * 100;
            return new AllPages.Page(page, 5678, apartment.subList(start, Math.min(start + 100, 5678)));
        }, 200, 20000);
        check(all.size() == 5678 && pages.get() == 57, "all 57 pages required");
        check(all.stream().map(BuildingUnit::dongName).distinct().count() == 72, "72 buildings");
        var apartmentQuery = new BuildingQuery("11710", "10100", "0", "0019", "0000",
                "제160동", BuildingQuery.Ground.ABOVE_GROUND, 1, "101호");
        result = matcher.match(apartmentQuery, all, true, Instant.now());
        check(result.status() == Resolution.Status.MATCHED
                && result.candidates().getFirst().managementNumber().equals("10251100215481"), "apartment unique");
        var plainDongQuery = new BuildingQuery("11710", "10100", "0", "0019", "0000",
                "160동", BuildingQuery.Ground.ABOVE_GROUND, 1, "101호");
        var decoratedDong = new BuildingUnit("10251100215481", "10251100215480", "11710", "10100",
                "0", "0019", "0000", "서울 송파구 잠실동 19", null, "제160동",
                BuildingQuery.Ground.ABOVE_GROUND, 1, "101", "아파트");
        check(matcher.match(plainDongQuery, List.of(decoratedDong), true, Instant.now()).status()
                == Resolution.Status.MATCHED, "160 dong equals decorated 160 dong");
        expect(UpstreamException.class, () -> new AllPages().collect(
                page -> new AllPages.Page(page, 2, List.of(villa)), 5, 10), "repeated row");
        expect(UpstreamException.class, () -> new AllPages().collect(
                page -> new AllPages.Page(page, page == 1 ? 2 : 3, List.of(villa)), 5, 10), "total changes");
        expect(UpstreamException.class, () -> new AllPages().collect(
                page -> new AllPages.Page(page, 2, page == 1 ? List.of(villa) : List.of()), 5, 10), "empty missing page");
        expect(UpstreamException.class, () -> new AllPages().collect(
                page -> new AllPages.Page(99, 1, List.of(villa)), 5, 10), "wrong page");
        expect(UpstreamException.class, () -> new AllPages().collect(
                page -> new AllPages.Page(page, 2, List.of(villa)), 1, 10), "page limit");

        String item = "<item><mgmBldrgstPk>1018152541</mgmBldrgstPk>"
                + "<sigunguCd>11530</sigunguCd><bjdongCd>10700</bjdongCd><platGbCd>0</platGbCd>"
                + "<bun>0355</bun><ji>0018</ji><platPlc>서울 구로구 개봉동 355-18</platPlc>"
                + "<flrGbCd>20</flrGbCd><flrNo>1.0</flrNo><hoNm>1층 101호</hoNm></item>";
        String ok = "<response><header><resultCode>00</resultCode></header><body><pageNo>1</pageNo>"
                + "<totalCount>1</totalCount><items>" + item + "</items></body></response>";
        var parsed = BuildingHubClient.parse(ok.getBytes(StandardCharsets.UTF_8));
        check(parsed.units().getFirst().floorNumber() == 1, "decimal integral floor");
        check(parsed.units().getFirst().floorGroundType() == BuildingQuery.Ground.ABOVE_GROUND, "ground code");
        check(parsed.units().getFirst().parentManagementNumber() == null, "expos has no parent field");
        expect(UpstreamException.class, () -> BuildingHubClient.parse(
                "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///secret'>]><x>&e;</x>".getBytes(StandardCharsets.UTF_8)), "block XXE");
        AtomicInteger calls = new AtomicInteger();
        BuildingHubClient denied = new BuildingHubClient("fake-key", uri -> {
            calls.incrementAndGet();
            return "<OpenAPI_ServiceResponse><returnReasonCode>30</returnReasonCode></OpenAPI_ServiceResponse>"
                    .getBytes(StandardCharsets.UTF_8);
        });
        expect(UpstreamException.class, () -> denied.fetchAll(BuildingHubClient.Operation.EXPOS, VILLA,
                System.nanoTime() + Duration.ofSeconds(30).toNanos()), "auth denied");
        check(calls.get() == 1, "no auth retry");
        calls.set(0);
        BuildingHubClient transientClient = new BuildingHubClient("fake-key", uri -> {
            if (calls.incrementAndGet() == 1) throw new UpstreamException("UPSTREAM_IO", true);
            return ok.getBytes(StandardCharsets.UTF_8);
        });
        check(transientClient.fetchAll(BuildingHubClient.Operation.EXPOS, VILLA,
                System.nanoTime() + Duration.ofSeconds(30).toNanos()).size() == 1 && calls.get() == 2, "bounded transient retry");
        check(new BuildingResolver(denied).resolve(VILLA).status() == Resolution.Status.UNVERIFIABLE, "error not not-found");
        BuildingHubClient valid = new BuildingHubClient("fake-key", uri -> {
            String xml = uri.getPath().endsWith("getBrExposInfo") ? ok
                    : uri.getPath().endsWith("getBrTitleInfo")
                    ? ok.replace("1018152541", "1018112940")
                    : ok.replace("</mgmBldrgstPk>",
                            "</mgmBldrgstPk><mgmUpBldrgstPk>1018112940</mgmUpBldrgstPk>");
            return xml.getBytes(StandardCharsets.UTF_8);
        });
        var verified = new BuildingResolver(valid).resolve(VILLA);
        check(verified.status() == Resolution.Status.MATCHED
                && verified.candidates().getFirst().managementNumber().equals("1018152541")
                && verified.candidates().getFirst().parentManagementNumber().equals("1018112940"),
                "basis PK links expos and title PKs");
        BuildingHubClient absentParent = new BuildingHubClient("fake-key", uri -> {
            String xml = uri.getPath().endsWith("getBrExposInfo") ? ok
                    : uri.getPath().endsWith("getBrTitleInfo")
                    ? ok.replace("1018152541", "1018112940")
                    : ok.replace("</mgmBldrgstPk>",
                            "</mgmBldrgstPk><mgmUpBldrgstPk>different-parent</mgmUpBldrgstPk>");
            return xml.getBytes(StandardCharsets.UTF_8);
        });
        check(new BuildingResolver(absentParent).resolve(VILLA).status() == Resolution.Status.UNVERIFIABLE,
                "unconfirmed parent");
        String empty = "<response><header><resultCode>00</resultCode></header><body>"
                + "<pageNo>1</pageNo><totalCount>0</totalCount><items/></body></response>";
        BuildingHubClient emptyClient = new BuildingHubClient("fake-key", uri -> empty.getBytes(StandardCharsets.UTF_8));
        check(new BuildingResolver(emptyClient).resolve(VILLA).status() == Resolution.Status.NOT_FOUND,
                "empty complete all operations");
        expect(UpstreamException.class, () -> valid.fetchAll(BuildingHubClient.Operation.EXPOS,
                VILLA, System.nanoTime()), "deadline before collection");
        String noGround = ok.replace("<flrGbCd>20</flrGbCd>", "<flrGbCd>99</flrGbCd>");
        check(matcher.match(VILLA, BuildingHubClient.parse(noGround.getBytes(StandardCharsets.UTF_8)).units(),
                true, Instant.now()).status() == Resolution.Status.UNVERIFIABLE, "unknown upstream ground");
        ResponseDiagnosticChecks.run();
    }
    private static BuildingUnit unit(String id, String parent, String dong, String ho) {
        return new BuildingUnit(id, parent, "11530", "10700", "0", "0355", "0018",
                "서울 구로구 개봉동 355-18", null, dong, BuildingQuery.Ground.ABOVE_GROUND, 1, ho, "다세대주택");
    }
    private static BuildingQuery query(BuildingQuery.Ground ground, int floor, String ho) {
        return new BuildingQuery("11530", "10700", "0", "0355", "0018", null, ground, floor, ho);
    }
    private static BuildingUnit unitAt(String id, BuildingQuery.Ground ground, Integer floor, String ho) {
        return new BuildingUnit(id, "parent", "11530", "10700", "0", "0355", "0018",
                "address", null, null, ground, floor, ho, "villa");
    }
    private static void check(boolean condition, String name) {
        checks++;
        if (!condition) throw new AssertionError(name);
    }
    private static void expect(Class<? extends Throwable> type, Runnable action, String name) {
        try { action.run(); } catch (Throwable error) {
            check(type.isInstance(error), name + ": " + error);
            return;
        }
        throw new AssertionError(name + ": no error");
    }

    private static final class ResponseDiagnosticChecks {
        private static final String SECRET = "sensitive-response-marker";
        private static final BuildingQuery QUERY = new BuildingQuery("11530", "10700", "0",
                "0355", "0018", null, BuildingQuery.Ground.ABOVE_GROUND, 1, "101");
        private static final String EMPTY = "<response><header><resultCode>00</resultCode></header>"
                + "<body><pageNo>1</pageNo><totalCount>0</totalCount><items/></body></response>";

        static void run() {
            check(BuildingHubClient.parse(bytes(EMPTY)).totalCount() == 0, "valid zero response preserved");
            expectCode("MALFORMED_RESPONSE_XML", () -> BuildingHubClient.parse(bytes("<response>" + SECRET)));
            expectCode("MALFORMED_RESPONSE_PAGINATION", () -> BuildingHubClient.parse(
                    bytes(EMPTY.replace("<pageNo>1</pageNo>", ""))));
            expectCode("MALFORMED_RESPONSE_PAGINATION", () -> BuildingHubClient.parse(
                    bytes(EMPTY.replace("<totalCount>0</totalCount>", "<totalCount>unknown</totalCount>"))));
            expectCode("MALFORMED_RESPONSE_PAGINATION", () -> BuildingHubClient.parse(
                    bytes(EMPTY.replace("<pageNo>1</pageNo>", "<pageNo>1</pageNo><pageNo>1</pageNo>"))));
            expectCode("MALFORMED_RESPONSE_PAGINATION", () -> BuildingHubClient.parse(
                    bytes(EMPTY.replace("<totalCount>0</totalCount>", "<totalCount>9999999999999</totalCount>"))));
            expectCode("MALFORMED_RESPONSE_XML", () -> BuildingHubClient.parse(bytes(
                    "<!DOCTYPE x [<!ENTITY e SYSTEM 'file:///secret'>]><x>&e;</x>")));
            for (var operation : BuildingHubClient.Operation.values()) {
                AtomicInteger calls = new AtomicInteger();
                BuildingHubClient client = new BuildingHubClient("fake-key", uri -> {
                    calls.incrementAndGet();
                    return bytes("<response>" + SECRET);
                });
                expectCode(operation.name() + "_PAGE_1_MALFORMED_RESPONSE_XML",
                        () -> client.fetchAll(operation, QUERY, deadline()));
                check(calls.get() == 1, "malformed data is not retried");
            }
            AtomicInteger calls = new AtomicInteger();
            BuildingHubClient unavailable = new BuildingHubClient("fake-key", uri -> {
                calls.incrementAndGet();
                throw new UpstreamException("UPSTREAM_HTTP_503", true);
            });
            expectCode("EXPOS_PAGE_1_UPSTREAM_HTTP_503",
                    () -> unavailable.fetchAll(BuildingHubClient.Operation.EXPOS, QUERY, deadline()));
            check(calls.get() == 2, "HTTP 503 retry limit preserved");
            calls.set(0);
            BuildingHubClient denied = new BuildingHubClient("fake-key", uri -> {
                calls.incrementAndGet();
                return bytes("<OpenAPI_ServiceResponse><returnReasonCode>30</returnReasonCode>"
                        + "</OpenAPI_ServiceResponse>");
            });
            expectCode("TITLE_PAGE_1_UPSTREAM_REJECTED",
                    () -> denied.fetchAll(BuildingHubClient.Operation.TITLE, QUERY, deadline()));
            check(calls.get() == 1, "authentication rejection is not retried");
            BuildingHubClient pageTwo = new BuildingHubClient("fake-key", uri ->
                    uri.getQuery().contains("pageNo=1&") ? bytes(EMPTY
                            .replace("<totalCount>0</totalCount>", "<totalCount>2</totalCount>")
                            .replace("<items/>", "<items><item><mgmBldrgstPk>one</mgmBldrgstPk></item></items>"))
                            : bytes("<response>" + SECRET));
            expectCode("BASIS_PAGE_2_MALFORMED_RESPONSE_XML",
                    () -> pageTwo.fetchAll(BuildingHubClient.Operation.BASIS, QUERY, deadline()));
        }
        private static long deadline() { return System.nanoTime() + Duration.ofSeconds(30).toNanos(); }
        private static byte[] bytes(String value) { return value.getBytes(StandardCharsets.UTF_8); }
        private static void expectCode(String code, Runnable action) {
            try { action.run(); }
            catch (UpstreamException e) {
                check(code.equals(e.getMessage()), "expected " + code + " received " + e.getMessage());
                check(!e.getMessage().contains(SECRET) && !e.getMessage().contains("fake-key")
                        && e.getCause() == null, "response and credentials excluded");
                return;
            }
            throw new AssertionError("Expected " + code);
        }
        private static void check(boolean condition, String description) {
            CoreChecks.check(condition, description);
        }
    }
}
