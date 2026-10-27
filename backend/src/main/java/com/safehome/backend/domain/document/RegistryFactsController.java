package com.safehome.backend.domain.document;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.common.exception.*;
import com.safehome.backend.domain.auth.AuthService;
import com.safehome.backend.domain.listing.ListingRepository;
import com.safehome.backend.domain.property.PropertyOwnerRepository;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Comparator;
import com.safehome.backend.domain.registry.RegistryRight;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listings/{listingId}/registry-facts")
public class RegistryFactsController {
    private final AuthService auth;private final ListingRepository listings;private final RegistryDocumentRepository documents;
    private final PropertyOwnerRepository owners;private final RegistryRightRepository rights;
    public RegistryFactsController(AuthService auth,ListingRepository listings,RegistryDocumentRepository documents,
            PropertyOwnerRepository owners,RegistryRightRepository rights) {
        this.auth=auth;this.listings=listings;this.documents=documents;this.owners=owners;this.rights=rights;
    }
    public record OwnerFact(String name,String ownershipShare,String ownershipType,String priorityNumber,String ownerAddress,BigDecimal acquisitionAmount,String entityType) {}
    public record RightFact(String type,BigDecimal amount,Integer priority,LocalDateTime registeredAt,String status,String sourceName) {}
    public record Facts(Long documentId,Long registrySnapshotId,List<OwnerFact> owners,List<RightFact> rights,boolean ownershipNeedsReview) {}
    @GetMapping @Transactional(readOnly=true)
    public ApiResponse<Facts> get(@RequestHeader("Authorization") String authorization,@PathVariable Long listingId,@RequestParam(required=false) Long documentId) {
        Long userId=auth.requireUser(authorization).getId();
        var listing=listings.findById(listingId).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!listing.getRegisteredByUserId().equals(userId))throw new BusinessException(ErrorCode.FORBIDDEN);
        var doc=(documentId==null?documents.findFirstByListingIdAndStatusOrderByIdDesc(listingId,"PARSED"):documents.findById(documentId))
            .orElseThrow(()->new BusinessException(ErrorCode.ANALYSIS_NOT_READY));
        if(!listingId.equals(doc.getListingId()) || !"PARSED".equals(doc.getStatus()))throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
        var rows=owners.findByRegistrySnapshotId(doc.getRegistrySnapshotId());
        var total=rows.stream().map(o->o.getOwnershipRatio()).filter(v->v!=null).reduce(BigDecimal.ZERO,BigDecimal::add);
        boolean review=rows.isEmpty() || rows.stream().anyMatch(o->o.getOwnershipShare()==null)
            || total.subtract(BigDecimal.valueOf(100)).abs().compareTo(new BigDecimal("0.01"))>0;
        return ApiResponse.success(new Facts(doc.getId(),doc.getRegistrySnapshotId(),
            rows.stream().map(o->new OwnerFact(o.getOwnerName(),o.getOwnershipShare(),o.getOwnershipType(),o.getRegistryPriorityNumber(),o.getOwnerAddress(),o.getAcquisitionAmount(),o.getOwnerEntityType())).toList(),
            rights.findByRegistrySnapshotIdAndStatus(doc.getRegistrySnapshotId(),"ACTIVE").stream()
                .sorted(Comparator.comparing(RegistryRight::getPriority, Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(RegistryRight::getId))
                .map(r->new RightFact(r.getRightType(),r.getAmount(),r.getPriority(),r.getRegisteredAt(),r.getStatus(),r.getSourceName())).toList(),review));
    }
}
