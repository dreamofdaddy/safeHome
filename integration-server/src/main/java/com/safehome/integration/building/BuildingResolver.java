package com.safehome.integration.building;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Semaphore;

public final class BuildingResolver {
    private final BuildingHubClient client;
    private final Semaphore inFlight = new Semaphore(2);
    public BuildingResolver(BuildingHubClient client) { this.client = client; }
    public Resolution resolve(BuildingQuery query) {
        if (!inFlight.tryAcquire()) return Resolution.unavailable("LOCAL_CAPACITY_EXCEEDED");
        Instant queriedAt = Instant.now();
        long deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
        try {
            var expos = client.fetchAll(BuildingHubClient.Operation.EXPOS, query, deadline);
            var title = client.fetchAll(BuildingHubClient.Operation.TITLE, query, deadline);
            var basis = client.fetchAll(BuildingHubClient.Operation.BASIS, query, deadline);
            // The Basis record explicitly links its PK to the parent Title PK.
            // Expos has no parent-PK field in the published contract.
            Map<String, BuildingUnit> basisById = new HashMap<>();
            for (BuildingUnit unit : basis) basisById.put(unit.managementNumber(), unit);
            Set<String> titleIds = new HashSet<>();
            for (BuildingUnit unit : title) titleIds.add(unit.managementNumber());
            List<BuildingUnit> corroborated = new ArrayList<>();
            for (BuildingUnit unit : expos) {
                BuildingUnit basisUnit = basisById.get(unit.managementNumber());
                if (basisUnit == null || basisUnit.parentManagementNumber() == null
                        || !titleIds.contains(basisUnit.parentManagementNumber())) {
                    return Resolution.unavailable("PARENT_NOT_CORROBORATED");
                }
                corroborated.add(new BuildingUnit(unit.managementNumber(), basisUnit.parentManagementNumber(),
                        unit.sigunguCd(), unit.bjdongCd(), unit.platGbCd(), unit.bun(), unit.ji(),
                        unit.address(), unit.roadAddress(), unit.dongName(), unit.floorGroundType(),
                        unit.floorNumber(), unit.unitName(), unit.purpose()));
            }
            return new BuildingMatcher().match(query, corroborated, true, queriedAt);
        } catch (UpstreamException e) {
            return Resolution.unavailable(e.getMessage());
        } finally { inFlight.release(); }
    }
}
