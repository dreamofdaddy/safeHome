package com.safehome.backend.domain.document.parser;

import com.safehome.backend.domain.listing.Listing;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/** Private validation evidence with allowlisted right categories. Never serialize the entire parsed document or exception. */
@Component
public class RegistryValidationDiagnostics {
    private static final Logger log = LoggerFactory.getLogger(RegistryValidationDiagnostics.class);
    private final ObjectMapper mapper;
    private final Path directory;

    public RegistryValidationDiagnostics(ObjectMapper mapper,
            @Value("${safehome.document.storage-root:./var/registry-documents}") String storageRoot) {
        this.mapper = mapper;
        directory = Path.of(storageRoot).toAbsolutePath().normalize().resolve("diagnostics/registry-validation");
    }

    private static String sourceCategory(String source) {
        if(source==null)return "UNAVAILABLE";
        for(String label:List.of("담보가등기","소유권이전청구권","소유권이전","가등기","신탁","근저당","저당","임차권","전세권","가압류","압류","가처분","경매","금지사항","환매","지상권","지역권","대지권"))
            if(source.contains(label))return label;
        return "UNRECOGNIZED";
    }

    /** Caller must authenticate listing ownership; never expose paths, owners or the full diagnostic. */
    public com.safehome.backend.domain.document.dto.ListingRegistryProcessingResponse.AddressMismatch addressMismatch(Long documentId,Long listingId) {
        if(documentId==null || listingId==null || !Files.isDirectory(directory))return null;
        try(var paths=Files.list(directory)) {
            var candidates=paths.filter(p->p.getFileName().toString().matches("document-"+documentId+"-[0-9a-f-]{36}\\.json"))
                .filter(p->Files.isRegularFile(p,LinkOption.NOFOLLOW_LINKS))
                .sorted(Comparator.comparingLong((Path p)->{try{return Files.getLastModifiedTime(p,LinkOption.NOFOLLOW_LINKS).toMillis();}catch(Exception ex){return 0;}}).reversed()).toList();
            for(Path candidate:candidates)try {
                if(Files.size(candidate)>262144)continue;
                var report=mapper.readTree(Files.readString(candidate));
                if(report.path("documentId").asLong()!=documentId || report.path("listingId").asLong()!=listingId
                    || !"REGISTRY_ADDRESS_MISMATCH".equals(report.path("errorCode").stringValue()))continue;
                String registered=addressText(report.path("registered").path("fullAddress"));
                String lot=addressText(report.path("extracted").path("lotAddress"));
                String road=addressText(report.path("extracted").path("roadAddress"));
                if(registered!=null && (lot!=null || road!=null))return new com.safehome.backend.domain.document.dto.ListingRegistryProcessingResponse.AddressMismatch(registered,lot,road);
            } catch(Exception ignored) { /* Missing/unreadable evidence must not change the analysis status. */ }
        } catch(Exception ignored) { }
        return null;
    }
    private String addressText(tools.jackson.databind.JsonNode node){
        if(!node.isString())return null;String text=node.stringValue();return text.isBlank() || text.length()>2000?null:text;
    }

    public void record(Long documentId, Listing listing, ParsedRegistryDocument parsed, String code) {
        boolean lotMatches = parsed != null && RegistryAddressMatcher.same(listing.getFullAddress(), parsed.address());
        boolean roadMatches = parsed != null && RegistryAddressMatcher.same(listing.getFullAddress(), parsed.roadAddress());
        try {
            var report = new LinkedHashMap<String,Object>();
            report.put("schemaVersion", 1);
            report.put("recordedAt", OffsetDateTime.now().toString());
            report.put("documentId", documentId);
            report.put("listingId", listing.getId());
            report.put("stage", "VALIDATE_EXTRACTION");
            report.put("errorCode", code);
            var facts = new LinkedHashMap<String,Object>();
            facts.put("extractionPresent", parsed != null);
            facts.put("registryNumberFormatValid", parsed != null && parsed.registryPropertyNumber() != null
                && parsed.registryPropertyNumber().matches("[0-9]{4}-[0-9]{4}-[0-9]{6}"));
            facts.put("ownerCount", parsed == null || parsed.owners() == null ? null : parsed.owners().size());
            facts.put("rightCount", parsed == null || parsed.rights() == null ? null : parsed.rights().size());
            if(parsed!=null && parsed.rights()!=null) {
                var rightChecks=new ArrayList<Map<String,Object>>();int index=0;
                for(var right:parsed.rights()) {
                    var check=new LinkedHashMap<String,Object>();check.put("index",++index);
                    var types=Set.of("MORTGAGE","LEASEHOLD","SEIZURE","PROVISIONAL_SEIZURE","TRUST","AUCTION","OTHER");
                    String type=right!=null && right.type()!=null && types.contains(right.type())?right.type():"UNRECOGNIZED";
                    String status=right!=null && right.status()!=null && Set.of("ACTIVE","INACTIVE").contains(right.status())?right.status():"UNRECOGNIZED";
                    check.put("type",type);check.put("status",status);
                    check.put("rejectionReason","UNRECOGNIZED".equals(type)?"UNSUPPORTED_TYPE":"UNRECOGNIZED".equals(status)?"UNSUPPORTED_STATUS":"NONE");
                    String source=right==null?null:right.sourceName();
                    check.put("sourceCategory", sourceCategory(source));rightChecks.add(check);
                }
                facts.put("rightChecks",rightChecks);
            }
            report.put("facts", facts);
            if ("REGISTRY_ADDRESS_MISMATCH".equals(code)) {
                var registered = new LinkedHashMap<String,Object>();
                registered.put("baseAddress", listing.getAddress());
                registered.put("detailAddress", listing.getDetailAddress());
                registered.put("fullAddress", listing.getFullAddress());
                report.put("registered", registered);
                var extracted = new LinkedHashMap<String,Object>();
                extracted.put("lotAddress", parsed == null ? null : parsed.address());
                extracted.put("roadAddress", parsed == null ? null : parsed.roadAddress());
                report.put("extracted", extracted);
                var comparison = new LinkedHashMap<String,Object>();
                comparison.put("normalizedRegistered", RegistryAddressMatcher.normalize(listing.getFullAddress()));
                comparison.put("normalizedLot", parsed == null ? null : RegistryAddressMatcher.normalize(parsed.address()));
                comparison.put("normalizedRoad", parsed == null ? null : RegistryAddressMatcher.normalize(parsed.roadAddress()));
                comparison.put("lotMatches", lotMatches);
                comparison.put("roadMatches", roadMatches);
                comparison.put("method", "NFKC, leading administrative-name aliases, ordinal/punctuation normalization, optional explicit floor with same unit; no inferred address");
                report.put("comparison", comparison);
            }
            Files.createDirectories(directory);
            Path target = directory.resolve("document-" + documentId + "-" + UUID.randomUUID() + ".json");
            Files.writeString(target, mapper.writerWithDefaultPrettyPrinter().writeValueAsString(report),
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
            log.warn("Registry validation rejected: documentId={}, listingId={}, code={}, lotMatches={}, roadMatches={}, diagnostic={}",
                documentId, listing.getId(), code, lotMatches, roadMatches, target);
        } catch (Exception ex) {
            // Diagnostics must not replace the actual validation error or emit its sensitive message.
            log.warn("Registry validation rejected: documentId={}, listingId={}, code={}, lotMatches={}, roadMatches={}, diagnostic=UNAVAILABLE, diagnosticException={}",
                documentId, listing.getId(), code, lotMatches, roadMatches, ex.getClass().getSimpleName());
        }
    }
}
