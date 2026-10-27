package com.safehome.backend.integration.building;

import com.safehome.backend.domain.listing.Listing;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Repository
public class BuildingIdentificationStore {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public BuildingIdentificationStore(JdbcTemplate jdbc, ObjectMapper mapper) {
        this.jdbc = jdbc; this.mapper = mapper;
    }

    /** Joins the registry persistence transaction; the listing pointer changes in that same commit. */
    @Transactional
    public Long create(Listing listing, Long snapshotId, Long documentId,
                       BuildingQueryFactory.Result input, boolean enabled) {
        return create(listing, snapshotId, documentId, input, enabled, listing);
    }

    @Transactional
    public Long create(Listing listing, Long snapshotId, Long documentId,
                       BuildingQueryFactory.Result input, boolean enabled, Listing queryListing) {
        String status = input.status() == BuildingQueryFactory.Status.READY
                ? (enabled ? "PENDING" : "DISABLED") : input.status().name();
        String queryJson = input.query() == null ? null : mapper.writeValueAsString(input.query());
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("queryBasis", snapshotId == null ? "LISTING" : "REGISTRY");
        snapshot.put("address", queryListing.getAddress());
        snapshot.put("detailAddress", queryListing.getDetailAddress());
        snapshot.put("addressSearchEvidence", queryListing.getAddressSearchEvidence());
        snapshot.put("parsedLabelsAndQuery", input);
        String inputJson = mapper.writeValueAsString(snapshot);
        return jdbc.queryForObject("""
                INSERT INTO building_identifications
                  (listing_id, registry_snapshot_id, registry_document_id,
                   execution_status, reason, query_json, input_json)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING id
                """, Long.class, listing.getId(), snapshotId, documentId, status, input.reason(),
                queryJson, inputJson);
    }

    @Transactional
    public BuildingQuery claim(long id) {
        int updated = jdbc.update("""
                UPDATE building_identifications b SET execution_status='RUNNING',
                  attempt_count=attempt_count+1, updated_at=CURRENT_TIMESTAMP
                WHERE b.id=? AND b.execution_status='PENDING' AND b.attempt_count<2
                  AND EXISTS (SELECT 1 FROM listings l WHERE l.id=b.listing_id
                              AND l.current_building_identification_id=b.id)
                """, id);
        if (updated == 0) return null;
        String json = jdbc.queryForObject(
                "SELECT query_json FROM building_identifications WHERE id=?", String.class, id);
        return mapper.readValue(json, BuildingQuery.class);
    }

    /** Caller holds the listing row lock. Copy immutable input and preserve every previous attempt, including successful matches. */
    public Long createRetry(Listing listing) {
        List<Long> ids = jdbc.query("""
                INSERT INTO building_identifications
                  (listing_id, registry_snapshot_id, registry_document_id, execution_status, query_json, input_json)
                SELECT b.listing_id, b.registry_snapshot_id, b.registry_document_id, 'PENDING', b.query_json, b.input_json
                FROM building_identifications b LEFT JOIN registry_documents d ON d.id=b.registry_document_id
                WHERE b.id=? AND b.listing_id=? AND b.query_json IS NOT NULL
                  AND b.execution_status IN ('CALL_FAILED','COMPLETED','DISABLED')
                  AND (b.registry_document_id IS NULL OR (d.listing_id=b.listing_id AND d.status='PARSED' AND d.registry_snapshot_id=b.registry_snapshot_id
                  AND d.id=(SELECT max(latest.id) FROM registry_documents latest WHERE latest.listing_id=b.listing_id)))
                RETURNING id
                """, (rs, row) -> rs.getLong(1), listing.getCurrentBuildingIdentificationId(), listing.getId());
        return ids.isEmpty() ? null : ids.getFirst();
    }

    @Transactional
    public void finish(long id, Resolution resolution, boolean callFailed) {
        String execution = callFailed ? "CALL_FAILED" : "COMPLETED";
        BuildingUnit matched = resolution.status() == Resolution.Status.MATCHED
                && resolution.candidates().size() == 1 ? resolution.candidates().getFirst() : null;
        int updated = jdbc.update("""
                UPDATE building_identifications b SET execution_status=?, resolution_status=?,
                  reason=?, candidate_count=?, management_number=?, parent_management_number=?,
                  unit_name=?, source=?, queried_at=?, updated_at=CURRENT_TIMESTAMP
                WHERE b.id=? AND b.execution_status='RUNNING'
                  AND EXISTS (SELECT 1 FROM listings l WHERE l.id=b.listing_id
                              AND l.current_building_identification_id=b.id)
                """, execution, callFailed ? null : resolution.status().name(), resolution.reason(),
                resolution.candidates().size(), matched == null ? null : matched.managementNumber(),
                matched == null ? null : matched.parentManagementNumber(),
                matched == null ? null : matched.unitName(), resolution.source(),
                OffsetDateTime.ofInstant(resolution.queriedAt(), java.time.ZoneOffset.UTC), id);
        if (updated == 0) jdbc.update("""
                UPDATE building_identifications SET execution_status='SUPERSEDED',
                  resolution_status=?, reason=?, candidate_count=?, management_number=?,
                  parent_management_number=?, unit_name=?, source=?, queried_at=?,
                  updated_at=CURRENT_TIMESTAMP WHERE id=? AND execution_status='RUNNING'
                """, callFailed ? null : resolution.status().name(), resolution.reason(), resolution.candidates().size(),
                matched == null ? null : matched.managementNumber(),
                matched == null ? null : matched.parentManagementNumber(),
                matched == null ? null : matched.unitName(), resolution.source(),
                OffsetDateTime.ofInstant(resolution.queriedAt(), java.time.ZoneOffset.UTC), id);
    }

    public BuildingIdentificationResponse current(long listingId) {
        List<BuildingIdentificationResponse> rows = jdbc.query("""
                SELECT b.* FROM listings l JOIN building_identifications b
                  ON b.id=l.current_building_identification_id
                WHERE l.id=?
                """, (rs, row) -> map(rs), listingId);
        return rows.isEmpty() ? BuildingIdentificationResponse.notRequested() : rows.getFirst();
    }

    private BuildingIdentificationResponse map(ResultSet rs) throws SQLException {
        String value = rs.getString("resolution_status");
        OffsetDateTime queried = rs.getObject("queried_at", OffsetDateTime.class);
        return new BuildingIdentificationResponse(rs.getLong("id"), rs.getObject("registry_snapshot_id", Long.class),
                rs.getString("execution_status"), value == null ? null : Resolution.Status.valueOf(value),
                rs.getString("reason"), (Integer) rs.getObject("candidate_count"),
                rs.getString("management_number"), rs.getString("parent_management_number"),
                rs.getString("unit_name"), rs.getString("source"),
                queried == null ? null : queried.toInstant());
    }

    /** A restart must never trigger a public API request, including previously queued work. */
    public List<Long> recover(boolean enabled) {
        jdbc.update("""
                UPDATE building_identifications SET execution_status='CALL_FAILED',
                  reason='REQUEST_INTERRUPTED', updated_at=CURRENT_TIMESTAMP
                WHERE execution_status IN ('PENDING','RUNNING')
                """);
        return List.of();
    }
}
