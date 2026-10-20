package com.safehome.backend.integration.building;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.document.parser.VerifiedRegistryDocument;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
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

    /** Called inside the registry persistence transaction. Only the callback runs after commit. */
    public void prepare(Listing listing, long snapshotId, long documentId,
                        VerifiedRegistryDocument registry) {
        var input = factory.build(listing, registry);
        Long id = store.create(listing, snapshotId, documentId, input, enabled);
        listing.setCurrentBuildingIdentificationId(id);
        if (enabled && input.status() == BuildingQueryFactory.Status.READY) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() {
                    try { worker.process(id); }
                    catch (RuntimeException ignored) { /* Durable PENDING row is recovered at startup. */ }
                }
            });
        }
    }

    public BuildingIdentificationResponse current(Long userId, Long listingId) {
        Listing listing = listings.findById(listingId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        if (!listing.getRegisteredByUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
        return store.current(listingId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumeInterrupted() {
        for (Long id : store.recover(enabled)) {
            try { worker.process(id); }
            catch (RuntimeException ignored) { /* Remains PENDING for next restart. */ }
        }
    }
}
