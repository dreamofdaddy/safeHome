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
    public Long create(Listing listing, long snapshotId, long documentId,
                       BuildingQueryFactory.Result input, boolean enabled) {
        String status = input.status() == BuildingQueryFactory.Status.READY
                ? (enabled ? "PENDING" : "DISABLED") : input.status().name();
        String queryJson = input.query() == null ? null : mapper.writeValueAsString(input.query());
        var snapshot = new LinkedHashMap<String, Object>();
        snapshot.put("address", listing.getAddress());
        snapshot.put("detailAddress", listing.getDetailAddress());
        snapshot.put("addressSearchEvidence", listing.getAddressSearchEvidence());
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
        return new BuildingIdentificationResponse(rs.getLong("id"), rs.getLong("registry_snapshot_id"),
                rs.getString("execution_status"), value == null ? null : Resolution.Status.valueOf(value),
                rs.getString("reason"), (Integer) rs.getObject("candidate_count"),
                rs.getString("management_number"), rs.getString("parent_management_number"),
                rs.getString("unit_name"), rs.getString("source"),
                queried == null ? null : queried.toInstant());
    }

    /** Single instance development recovery; terminal provider failures are never retried automatically. */
    public List<Long> recover(boolean enabled) {
        jdbc.update("""
                UPDATE building_identifications b SET execution_status='SUPERSEDED', updated_at=CURRENT_TIMESTAMP
                WHERE execution_status IN ('PENDING','RUNNING','DISABLED') AND NOT EXISTS
                  (SELECT 1 FROM listings l WHERE l.id=b.listing_id
                   AND l.current_building_identification_id=b.id)
                """);
        if (!enabled) {
            jdbc.update("""
                    UPDATE building_identifications SET execution_status='DISABLED',
                      updated_at=CURRENT_TIMESTAMP WHERE execution_status IN ('PENDING','RUNNING')
                    """);
            return List.of();
        }
        jdbc.update("""
                UPDATE building_identifications SET execution_status='CALL_FAILED',
                  reason='INTERRUPTED_RETRY_LIMIT', updated_at=CURRENT_TIMESTAMP
                WHERE execution_status IN ('PENDING','RUNNING') AND attempt_count>=2
                """);
        jdbc.update("""
                UPDATE building_identifications SET execution_status='PENDING',
                  updated_at=CURRENT_TIMESTAMP WHERE execution_status IN ('RUNNING','DISABLED')
                  AND attempt_count<2
                """);
        return jdbc.query("""
                SELECT b.id FROM building_identifications b JOIN listings l
                  ON l.current_building_identification_id=b.id
                WHERE b.execution_status='PENDING' AND b.attempt_count<2
                """, (rs, row) -> rs.getLong(1));
    }
}
