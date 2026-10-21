package com.safehome.integration.building;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Closed metadata schema: no request, URI, response, header, or Throwable fields. */
public final class CallHistory {
    private CallHistory() {}
    public enum EntryPoint { REST, MCP, DIRECT }
    public enum Kind { JOB_STARTED, API_ATTEMPT, JOB_FINISHED }
    public enum Outcome { STARTED, SUCCESS, FAILURE, COMPLETED }
    public record Job(UUID id, Long identificationId, EntryPoint entryPoint) {
        public Job {
            if (id == null || entryPoint == null || (identificationId != null && identificationId < 1))
                throw new IllegalArgumentException("Invalid call history job");
        }
        public static Job create(Long identificationId, EntryPoint entryPoint) {
            return new Job(UUID.randomUUID(), identificationId, entryPoint);
        }
    }
    public record Event(Instant timestamp, Job job, Kind kind, BuildingHubClient.Operation operation,
                        Integer page, Integer attempt, Outcome outcome, long elapsedMillis,
                        String errorCode, Resolution.Status resolutionStatus, Boolean collectionComplete) {
        public Event {
            if (timestamp == null || job == null || kind == null || outcome == null || elapsedMillis < 0)
                throw new IllegalArgumentException("Invalid call history event");
            errorCode = safeCode(errorCode);
        }
    }
    @FunctionalInterface public interface Sink { void record(Event event); }
    public static final Sink NONE = event -> {};
    public static void emit(Sink sink, Event event) {
        try { sink.record(event); }
        catch (RuntimeException ignored) { /* Diagnostics must not change identification results. */ }
    }
    private static final Set<String> CODES = Set.of("SERVICE_KEY_NOT_CONFIGURED", "UPSTREAM_IO",
            "UPSTREAM_REJECTED", "RESPONSE_SIZE", "INTERRUPTED", "COLLECTION_DEADLINE",
            "MALFORMED_RESPONSE_XML_SETUP", "MALFORMED_RESPONSE_XML", "MALFORMED_RESPONSE_HEADER",
            "MALFORMED_RESPONSE_PAGINATION", "MALFORMED_RESPONSE_ITEMS", "INCONSISTENT_PAGINATION",
            "PAGE_LIMIT_EXCEEDED", "INCOMPLETE_COLLECTION", "PARENT_NOT_CORROBORATED",
            "CANDIDATE_FLOOR_NUMBER_UNAVAILABLE", "UNINTERPRETABLE_CANDIDATE", "LOCAL_CAPACITY_EXCEEDED",
            "UNEXPECTED_FAILURE");
    static String safeCode(String raw) {
        if (raw == null) return null;
        if (raw.length() > 100) return "UNEXPECTED_FAILURE";
        String code = raw.replaceFirst("^(TITLE|EXPOS|BASIS)_PAGE_[1-9][0-9]{0,2}_", "");
        return CODES.contains(code) || code.matches("UPSTREAM_HTTP_[1-5][0-9]{2}") ? raw : "UNEXPECTED_FAILURE";
    }
    public static long elapsed(long startNanos) { return Math.max(0, (System.nanoTime() - startNanos) / 1_000_000); }
}
