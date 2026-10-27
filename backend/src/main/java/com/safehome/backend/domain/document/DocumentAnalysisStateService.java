package com.safehome.backend.domain.document;

import com.safehome.backend.common.exception.*;
import com.safehome.backend.domain.document.dto.ListingRegistryProcessingResponse;
import com.safehome.backend.domain.listing.*;
import com.safehome.backend.domain.buildingregister.BuildingRegisterStore;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;

@Service
public class DocumentAnalysisStateService {
    private final com.safehome.backend.domain.document.parser.RegistryValidationDiagnostics diagnostics;
    private final ListingRepository listings;private final RegistryDocumentRepository documents;
    private final RegistryAnalysisWorker worker;private final PrivatePdfStorage storage;private final BuildingRegisterStore buildingWork;
    public DocumentAnalysisStateService(ListingRepository listings,RegistryDocumentRepository documents,
            RegistryAnalysisWorker worker,PrivatePdfStorage storage,BuildingRegisterStore buildingWork,com.safehome.backend.domain.document.parser.RegistryValidationDiagnostics diagnostics) {
        this.diagnostics=diagnostics;
        this.listings=listings;this.documents=documents;this.worker=worker;this.storage=storage;this.buildingWork=buildingWork;
    }
    @Transactional public ListingRegistryProcessingResponse start(Long userId,Long listingId) {return start(userId,listingId,false);}
    @Transactional public ListingRegistryProcessingResponse start(Long userId,Long listingId,boolean reparseConsent) {
        Listing listing=listings.findByIdForUpdate(listingId).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        assertOwner(listing,userId);RegistryDocument document=findDocument(listingId);
        if("PROCESSING".equals(document.getStatus()) || buildingWork.hasRunningWork(listingId))throw new BusinessException(ErrorCode.CONFLICT);
        if("PARSED".equals(document.getStatus())) {
            if(!reparseConsent || document.getStorageKey()==null)throw new BusinessException(ErrorCode.CONFLICT);
            String newKey=storage.store(storage.read(document.getStorageKey()));
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
                @Override public void afterCompletion(int status){if(status!=STATUS_COMMITTED)storage.delete(newKey);}
            });
            document=documents.saveAndFlush(new RegistryDocument(userId,document.getPropertyId(),listingId,
                document.getOriginalFilename(),document.getSizeBytes(),document.getSha256(),newKey));
        } else if(!"UPLOADED".equals(document.getStatus()) && !"FAILED".equals(document.getStatus()))throw new BusinessException(ErrorCode.CONFLICT);
        if(!reparseConsent && !"READY_FOR_ANALYSIS".equals(listing.getStatus()) && !"REGISTRY_FAILED".equals(listing.getStatus()))throw new BusinessException(ErrorCode.CONFLICT);
        document.startProcessing();listing.setCurrentBuildingIdentificationId(null);listing.markRegistryProcessing();
        Long documentId=document.getId();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization(){
            @Override public void afterCommit(){worker.process(documentId);}
        });
        return ListingRegistryProcessingResponse.from(document);
    }
    @Transactional(readOnly=true) public ListingRegistryProcessingResponse result(Long userId,Long listingId) {
        var listing=listings.findById(listingId).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        assertOwner(listing,userId);var document=findDocument(listingId);
        var evidence="FAILED".equals(document.getStatus()) && "REGISTRY_ADDRESS_MISMATCH".equals(document.getErrorCode())
            ?diagnostics.addressMismatch(document.getId(),listingId):null;
        return ListingRegistryProcessingResponse.from(document,evidence);
    }
    private RegistryDocument findDocument(Long id){return documents.findTopByListingIdOrderByIdDesc(id).orElseThrow(()->new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));}
    private void assertOwner(Listing listing,Long userId){if(!listing.getRegisteredByUserId().equals(userId))throw new BusinessException(ErrorCode.FORBIDDEN);}
    @EventListener(ApplicationReadyEvent.class) @Transactional
    public void resumeInterruptedWork() {
        // Restart must never silently retransmit a private PDF or repeat a paid inference call.
        for(var doc:documents.findByStatus("PROCESSING")) {
            doc.fail("PARSER_INTERRUPTED");
            listings.findById(doc.getListingId()).ifPresent(Listing::markRegistryFailed);
        }
    }
}
