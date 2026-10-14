package com.safehome.backend.domain.document;

import com.safehome.backend.domain.listing.ListingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentAnalysisFailureService {
    private final RegistryDocumentRepository documents;
    private final ListingRepository listings;

    public DocumentAnalysisFailureService(RegistryDocumentRepository documents,
                                          ListingRepository listings) {
        this.documents = documents; this.listings = listings;
    }

    @Transactional
    public void fail(Long documentId, String code) {
        RegistryDocument document = documents.findById(documentId).orElseThrow();
        if (!"PROCESSING".equals(document.getStatus())) return;
        document.fail(code);
        listings.findById(document.getListingId()).orElseThrow().markRegistryFailed();
    }
}
