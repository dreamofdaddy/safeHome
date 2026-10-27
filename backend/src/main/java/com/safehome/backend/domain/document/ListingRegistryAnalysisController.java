package com.safehome.backend.domain.document;

import com.safehome.backend.common.exception.*;
import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.AuthService;
import com.safehome.backend.domain.document.dto.ListingRegistryProcessingResponse;
import com.safehome.backend.domain.listing.*;
import com.safehome.backend.domain.buildingregister.BuildingRegisterStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.ResponseEntity;
import java.util.UUID;

@RestController
@RequestMapping("/api/listings/{listingId}/registry-analysis")
public class ListingRegistryAnalysisController {
    private final AuthService auth; private final RegistryUploadAnalysisService service;
    public ListingRegistryAnalysisController(AuthService auth,RegistryUploadAnalysisService service){this.auth=auth;this.service=service;}
    @PostMapping(consumes="multipart/form-data")
    public ResponseEntity<ApiResponse<ListingRegistryProcessingResponse>> analyze(
        @RequestHeader("Authorization") String authorization,@PathVariable Long listingId,
        @RequestPart("registryPdf") MultipartFile file,@RequestParam boolean consent,@RequestParam UUID requestId) {
        return ResponseEntity.accepted().body(ApiResponse.success(service.analyze(auth.requireUser(authorization).getId(),listingId,file,consent,requestId)));
    }
}

@Service
class RegistryUploadAnalysisService {
    private final ListingRepository listings;private final RegistryDocumentRepository documents;
    private final RegistryDocumentService uploads;private final DocumentAnalysisStateService state;private final BuildingRegisterStore building;
    RegistryUploadAnalysisService(ListingRepository listings,RegistryDocumentRepository documents,RegistryDocumentService uploads,
        DocumentAnalysisStateService state,BuildingRegisterStore building){this.listings=listings;this.documents=documents;this.uploads=uploads;this.state=state;this.building=building;}
    @Transactional
    public ListingRegistryProcessingResponse analyze(Long userId,Long listingId,MultipartFile file,boolean consent,UUID requestId){
        if(!consent || requestId==null)throw new BusinessException(ErrorCode.INVALID_REQUEST);
        Listing listing=listings.findByIdForUpdate(listingId).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if(!listing.getRegisteredByUserId().equals(userId))throw new BusinessException(ErrorCode.FORBIDDEN);
        var existing=documents.findByListingIdAndAnalysisRequestId(listingId,requestId);
        if(existing.isPresent())return ListingRegistryProcessingResponse.from(existing.get());
        if("REGISTRY_PROCESSING".equals(listing.getStatus()) || documents.findByListingId(listingId).stream().anyMatch(d->"PROCESSING".equals(d.getStatus()))
            || building.hasRunningWork(listingId))throw new BusinessException(ErrorCode.CONFLICT);
        uploads.attach(userId,listingId,file,requestId);
        listing.requireRegistryReview();
        return state.start(userId,listingId);
    }
}
