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
    private final CallHistory.Sink history;
    private final Semaphore inFlight = new Semaphore(2);
    public BuildingResolver(BuildingHubClient client) { this(client, CallHistory.NONE); }
    public BuildingResolver(BuildingHubClient client, CallHistory.Sink history) { this.client = client; this.history = history; }
    public Resolution resolve(BuildingQuery query) {
        return resolve(query, CallHistory.Job.create(null, CallHistory.EntryPoint.DIRECT));
    }
    public Resolution resolve(BuildingQuery query, CallHistory.Job job) {
        long started = System.nanoTime();
        CallHistory.emit(history, new CallHistory.Event(Instant.now(), job, CallHistory.Kind.JOB_STARTED,
                null, null, null, CallHistory.Outcome.STARTED, 0, null, null, null));
        Resolution result;
        try { result = collect(query, job); }
        catch (RuntimeException ignored) { result = Resolution.unavailable("UNEXPECTED_FAILURE"); }
        CallHistory.emit(history, new CallHistory.Event(Instant.now(), job, CallHistory.Kind.JOB_FINISHED,
                null, null, null, result.collectionComplete() || "PARENT_NOT_CORROBORATED".equals(result.reason())
                        ? CallHistory.Outcome.COMPLETED : CallHistory.Outcome.FAILURE,
                CallHistory.elapsed(started), result.reason(), result.status(), result.collectionComplete()));
        return result;
    }
    private Resolution collect(BuildingQuery query, CallHistory.Job job) {
        if (!inFlight.tryAcquire()) return Resolution.unavailable("LOCAL_CAPACITY_EXCEEDED");
        Instant queriedAt = Instant.now();
        long deadline = System.nanoTime() + Duration.ofSeconds(120).toNanos();
        try {
            var expos = client.fetchAll(BuildingHubClient.Operation.EXPOS, query, deadline, job);
            var title = client.fetchAll(BuildingHubClient.Operation.TITLE, query, deadline, job);
            var basis = client.fetchAll(BuildingHubClient.Operation.BASIS, query, deadline, job);
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
