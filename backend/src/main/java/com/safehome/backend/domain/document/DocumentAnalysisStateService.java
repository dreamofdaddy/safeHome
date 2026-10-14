package com.safehome.backend.domain.document;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.document.dto.ListingRegistryProcessingResponse;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class DocumentAnalysisStateService {
    private final ListingRepository listings;
    private final RegistryDocumentRepository documents;
    private final RegistryAnalysisWorker worker;

    public DocumentAnalysisStateService(ListingRepository listings,
            RegistryDocumentRepository documents,
            RegistryAnalysisWorker worker) {
        this.listings = listings; this.documents = documents;
        this.worker = worker;
    }

    @Transactional
    public ListingRegistryProcessingResponse start(Long userId, Long listingId) {
        Listing listing = listings.findByIdForUpdate(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        assertOwner(listing, userId);
        RegistryDocument document = findDocument(listingId);
        if (!"UPLOADED".equals(document.getStatus()) && !"FAILED".equals(document.getStatus()))
            throw new BusinessException(ErrorCode.CONFLICT);
        if (!"READY_FOR_ANALYSIS".equals(listing.getStatus())
                && !"REGISTRY_FAILED".equals(listing.getStatus()))
            throw new BusinessException(ErrorCode.CONFLICT);
        document.startProcessing();
        listing.markRegistryProcessing();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() { worker.process(document.getId()); }
        });
        return ListingRegistryProcessingResponse.from(document);
    }

    @Transactional(readOnly = true)
    public ListingRegistryProcessingResponse result(Long userId, Long listingId) {
        Listing listing = listings.findById(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        assertOwner(listing, userId);
        RegistryDocument document = findDocument(listingId);
        return ListingRegistryProcessingResponse.from(document);
    }

    private RegistryDocument findDocument(Long listingId) {
        return documents.findTopByListingIdOrderByIdDesc(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
    }

    private void assertOwner(Listing listing, Long userId) {
        if (!listing.getRegisteredByUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterruptedWork() {
        for (RegistryDocument document : documents.findByStatus("PROCESSING")) {
            worker.process(document.getId());
        }
    }
}
