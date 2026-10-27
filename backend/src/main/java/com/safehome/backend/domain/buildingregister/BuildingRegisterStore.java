package com.safehome.backend.domain.buildingregister;

import static com.safehome.backend.domain.buildingregister.BuildingRegisterModels.*;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import java.sql.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Repository
public class BuildingRegisterStore {
    private final JdbcTemplate jdbc; private final ObjectMapper mapper; private final ListingRepository listings;
    public BuildingRegisterStore(JdbcTemplate jdbc,ObjectMapper mapper,ListingRepository listings) {
        this.jdbc=jdbc;this.mapper=mapper;this.listings=listings;
    }
    public Context context(Listing listing) {
        return jdbc.queryForObject("""
            SELECT d.id AS document_id,d.registry_snapshot_id,p.address AS registry_address,p.road_address,p.unit_number,
              b.id AS api_id,b.execution_status,b.resolution_status,b.unit_name,b.reason
            FROM listings l LEFT JOIN registry_documents d ON d.id=(SELECT max(x.id) FROM registry_documents x
                WHERE x.listing_id=l.id)
            LEFT JOIN properties p ON p.id=d.property_id AND d.status='PARSED'
            LEFT JOIN building_identifications b ON b.id=l.current_building_identification_id WHERE l.id=?
            """, (r,n)->new Context(listing.getAddress(),listing.getDetailAddress(),listing.getBuildingType(),
                    r.getObject("document_id",Long.class),r.getObject("registry_snapshot_id",Long.class),
                    r.getString("registry_address"),r.getString("road_address"),r.getString("unit_number"),
                    r.getObject("api_id",Long.class),r.getString("execution_status"),r.getString("resolution_status"),
                    r.getString("unit_name"),r.getString("reason")),listing.getId());
    }
    public long create(Listing listing,List<FileRef> files) {
        return jdbc.queryForObject("INSERT INTO building_register_reviews(listing_id,owner_id,status,files_json,context_json) VALUES(?,?,'UPLOADED',?,?) RETURNING id",
                Long.class,listing.getId(),listing.getRegisteredByUserId(),mapper.writeValueAsString(files),mapper.writeValueAsString(context(listing)));
    }
    public List<Stored> history(long listingId) {
        return jdbc.query("SELECT * FROM building_register_reviews WHERE listing_id=? ORDER BY id DESC LIMIT 50",this::map,listingId);
    }
    public boolean hasRunningWork(long listingId) {
        Integer count=jdbc.queryForObject("SELECT (SELECT count(*) FROM building_register_reviews WHERE listing_id=? AND status='PROCESSING')+(SELECT count(*) FROM building_identifications WHERE listing_id=? AND execution_status IN ('PENDING','RUNNING'))",Integer.class,listingId,listingId);
        return count!=null && count>0;
    }
    public List<String> allFileKeys(long listingId) {
        return jdbc.query("SELECT files_json FROM building_register_reviews WHERE listing_id=?",(r,n)->
            List.of(mapper.readValue(r.getString(1),FileRef[].class)),listingId).stream().flatMap(List::stream).map(FileRef::storageKey).toList();
    }
    public Stored byRequest(long listingId,UUID requestId) {
        var rows=jdbc.query("SELECT * FROM building_register_reviews WHERE listing_id=? AND analysis_request_id=?",this::map,listingId,requestId);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public void bindRequest(long id,UUID requestId){jdbc.update("UPDATE building_register_reviews SET analysis_request_id=? WHERE id=?",requestId,id);}
    public Stored get(long id) {
        var rows=jdbc.query("SELECT * FROM building_register_reviews WHERE id=?",this::map,id);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public Stored latestParsed(long listingId) {
        var rows=jdbc.query("SELECT * FROM building_register_reviews WHERE listing_id=? AND status='PARSED' ORDER BY id DESC LIMIT 1",this::map,listingId);
        return rows.isEmpty()?null:rows.getFirst();
    }
    public boolean sourceMatches(Stored row) {
        var listing=listings.findById(row.listingId()).orElse(null);
        return listing!=null && Objects.equals(listing.getRegisteredByUserId(),row.ownerId())
            && Objects.equals(listing.getAddress(),row.context().address())
            && Objects.equals(listing.getDetailAddress(),row.context().detailAddress())
            && Objects.equals(listing.getBuildingType(),row.context().buildingType());
    }
    public boolean documentCurrent(Stored row) {
        return sourceMatches(row) && Objects.equals(jdbc.queryForObject("SELECT max(id) FROM building_register_reviews WHERE listing_id=?",Long.class,row.listingId()),row.id());
    }
    public boolean current(Stored row) {
        var listing=listings.findById(row.listingId()).orElse(null);
        return listing!=null && Objects.equals(listing.getRegisteredByUserId(),row.ownerId())
                && Objects.equals(jdbc.queryForObject("SELECT max(id) FROM building_register_reviews WHERE listing_id=?",Long.class,row.listingId()),row.id())
                && row.context().equals(context(listing));
    }
    public boolean start(long id) {
        return jdbc.update("UPDATE building_register_reviews SET status='PROCESSING',consented_at=CURRENT_TIMESTAMP,error_code=NULL,result_json=NULL,attempt_count=attempt_count+1,processed_at=NULL,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status IN ('UPLOADED','FAILED')",id)==1;
    }
    @Transactional public void complete(long id,Result result,String provider,String model) {
        Stored row=get(id);if(row==null || !"PROCESSING".equals(row.status())) return;
        // Lock the listing, matching upload/start ordering, before deciding whether this is still current.
        listings.findByIdForUpdate(row.listingId()).orElseThrow();
        boolean current=current(row);
        jdbc.update("UPDATE building_register_reviews SET status=?,result_json=?,error_code=?,parser_provider=?,parser_model=?,processed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'",
                current?"PARSED":"SUPERSEDED",mapper.writeValueAsString(result),current?null:"SOURCE_CHANGED",provider,model,id);
    }
    @Transactional(propagation=org.springframework.transaction.annotation.Propagation.REQUIRES_NEW) public void fail(long id,String code) {
        jdbc.update("UPDATE building_register_reviews SET status='FAILED',error_code=?,processed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE id=? AND status='PROCESSING'",code,id);
    }
    public void recover() {
        jdbc.update("UPDATE building_register_reviews SET status='FAILED',error_code='INTERRUPTED',processed_at=CURRENT_TIMESTAMP,updated_at=CURRENT_TIMESTAMP WHERE status='PROCESSING'");
    }
    private Stored map(ResultSet r,int n) throws SQLException {
        return new Stored(r.getLong("id"),r.getLong("listing_id"),r.getLong("owner_id"),r.getString("status"),
                List.of(mapper.readValue(r.getString("files_json"),FileRef[].class)),
                mapper.readValue(r.getString("context_json"),Context.class),r.getString("result_json")==null?null:mapper.readValue(r.getString("result_json"),Result.class),
                r.getString("error_code"),r.getInt("attempt_count"),r.getString("parser_provider"),r.getString("parser_model"),
                r.getObject("created_at",java.time.OffsetDateTime.class),r.getObject("processed_at",java.time.OffsetDateTime.class));
    }
}
