package com.safehome.backend.integration.building;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
public class BuildingIdentificationService {
    private final BuildingIdentificationStore store;
    private final BuildingQueryFactory factory;
    private final BuildingIdentificationWorker worker;
    private final ListingRepository listings;
    private final boolean enabled;

    public BuildingIdentificationService(BuildingIdentificationStore store, BuildingQueryFactory factory,
            BuildingIdentificationWorker worker, ListingRepository listings,
            @Value("${safehome.integration.enabled:false}") boolean enabled) {
        this.store = store; this.factory = factory; this.worker = worker;
        this.listings = listings; this.enabled = enabled;
    }

    public BuildingIdentificationResponse current(Long userId, Long listingId) {
        Listing listing = listings.findById(listingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!listing.getRegisteredByUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        return store.current(listingId);
    }

    @Transactional
    public BuildingIdentificationResponse retry(Long userId, Long listingId) {
        Listing listing = listings.findByIdForUpdate(listingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!listing.getRegisteredByUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (!enabled) throw new BusinessException(ErrorCode.BUILDING_INTEGRATION_DISABLED);
        Long id = store.createRetry(listing);
        if (id == null) throw new BusinessException(ErrorCode.BUILDING_RETRY_NOT_ALLOWED);
        listing.setCurrentBuildingIdentificationId(id);
        listings.flush();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCommit() {
                try { worker.process(id); }
                catch (RuntimeException ignored) { /* Retry remains explicit after restart. */ }
            }
        });
        return store.current(listingId);
    }

    @Transactional
    public BuildingIdentificationResponse lookup(Long userId, Long listingId, BuildingLookupRequest request) {
        Listing listing = listings.findByIdForUpdate(listingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!listing.getRegisteredByUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if (!enabled) throw new BusinessException(ErrorCode.BUILDING_INTEGRATION_DISABLED);
        var current = store.current(listingId);
        if (java.util.List.of("PENDING", "RUNNING").contains(current.executionStatus()))
            throw new BusinessException(ErrorCode.BUILDING_RETRY_NOT_ALLOWED);
        if (request != null && request.detailAddress() != null && request.detailAddress().length() > 500)
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        var evidence = request != null && request.addressSearchEvidence() != null
            ? request.addressSearchEvidence().validatedFor(listing.getAddress()) : listing.getAddressSearchEvidence();
        String detail = request != null && request.detailAddress() != null ? request.detailAddress().strip() : listing.getDetailAddress();
        Listing queryListing = Listing.pendingAnalysis(userId, listing.getAddress(), listing.getBuildingName(),
            listing.getBuildingType(), detail, listing.getTransactionType(), listing.getDepositAmount(),
            listing.getMonthlyRent(), listing.getSalePrice(), null, null);
        queryListing.setAddressSearchEvidence(evidence);
        var input = factory.buildListing(queryListing);
        // Store the exact query input while retaining the real listing identity; no document is required.
        Long id = store.create(listing, null, null, input, true, queryListing);
        listing.setCurrentBuildingIdentificationId(id);
        listings.flush();
        if (input.status() == BuildingQueryFactory.Status.READY) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { worker.process(id); } catch (RuntimeException ignored) { /* Pending request is marked interrupted on restart; retry is explicit. */ }
                }
            });
        }
        return store.current(listingId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterrupted() {
        store.recover(enabled); // Mark interrupted work; never issue an external request at startup.
    }
}
