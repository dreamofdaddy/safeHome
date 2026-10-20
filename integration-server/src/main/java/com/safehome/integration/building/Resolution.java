package com.safehome.integration.building;

import java.time.Instant;
import java.util.List;

public record Resolution(Status status, List<BuildingUnit> candidates, String reason,
                         String source, Instant queriedAt, boolean collectionComplete) {
    public enum Status { MATCHED, NOT_FOUND, AMBIGUOUS, UNVERIFIABLE }
    public Resolution { candidates = List.copyOf(candidates); }
    public static Resolution unavailable(String reason) {
        return new Resolution(Status.UNVERIFIABLE, List.of(), reason,
                "MOLIT_BUILDING_HUB", Instant.now(), false);
    }
}
