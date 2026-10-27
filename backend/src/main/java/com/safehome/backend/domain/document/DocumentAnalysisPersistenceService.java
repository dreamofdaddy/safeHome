package com.safehome.backend.domain.document;

import com.safehome.backend.domain.document.parser.RegistryExtractionValidator;
import com.safehome.backend.domain.document.parser.RegistryValidationDiagnostics;
import com.safehome.backend.domain.document.parser.RegistryParseException;
import com.safehome.backend.domain.document.parser.ParsedRegistryDocument;
import com.safehome.backend.domain.document.parser.VerifiedRegistryDocument;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import com.safehome.backend.domain.property.OwnerIdentityService;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.property.PropertyOwnerRepository;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentAnalysisPersistenceService {
    private final RegistryDocumentRepository documents;
    private final ListingRepository listings;
    private final PropertyRepository properties;
    private final RegistrySnapshotRepository snapshots;
    private final PropertyOwnerRepository owners;
    private final RegistryRightRepository rights;
    private final OwnerIdentityService identities;
    private final RegistryExtractionValidator validator;
    private final RegistryValidationDiagnostics diagnostics;

    public DocumentAnalysisPersistenceService(RegistryDocumentRepository documents,
            ListingRepository listings, PropertyRepository properties,
            RegistrySnapshotRepository snapshots, PropertyOwnerRepository owners,
            RegistryRightRepository rights, OwnerIdentityService identities,
            RegistryExtractionValidator validator,
            RegistryValidationDiagnostics diagnostics) {
        this.documents = documents; this.listings = listings; this.properties = properties;
        this.snapshots = snapshots; this.owners = owners; this.rights = rights;
        this.identities = identities; this.validator = validator; this.diagnostics = diagnostics;
    }

    @Transactional
    public void complete(Long documentId, ParsedRegistryDocument parsed, String provider, String model) {
        RegistryDocument document = documents.findById(documentId).orElseThrow();
        if (!"PROCESSING".equals(document.getStatus())) return;
        Listing listing = listings.findByIdForUpdate(document.getListingId()).orElseThrow();
        VerifiedRegistryDocument verified;
        try {
            verified = validator.validate(parsed, listing);
        } catch (RegistryParseException ex) {
            diagnostics.record(documentId, listing, parsed, ex.code());
            throw ex;
        }

        Property property = properties.findByRegistryPropertyNumber(verified.registryPropertyNumber())
            .orElseGet(() -> properties.save(new Property(verified.registryPropertyNumber(),
                verified.address(), verified.roadAddress(),
                fallback(verified.buildingName(), listing.getBuildingName()), listing.getBuildingType(),
                verified.unitNumber(), null)));
        if (!sameAddress(property.getAddress(), verified.address()))
            throw new RegistryParseException("REGISTRY_PROPERTY_CONFLICT");

        RegistrySnapshot snapshot = snapshots.save(RegistrySnapshot.create(property,
            "UPLOADED_PDF", verified.observedAt(), document.getSha256()));
        for (VerifiedRegistryDocument.Owner owner : verified.owners()) {
            var savedOwner="CORPORATION".equals(owner.entityType())
                ? PropertyOwner.createCorporation(property,snapshot,owner.name(),
                    owner.maskedIdentifier()==null?null:identities.encryptCorporateIdentifier(owner.maskedIdentifier()),owner.ownershipRatio(),owner.registeredAt())
                : PropertyOwner.create(property,snapshot,owner.name(),identities.encryptMaskedIdentifier(owner.maskedIdentifier()),
                    identities.hashBirth6(owner.maskedIdentifier()),owner.ownershipRatio(),owner.registeredAt());
            savedOwner.preserveOwnershipShare(owner.ownershipShare());
            savedOwner.preserveSourceEvidence(owner.ownershipType(),owner.priorityNumber(),owner.ownerAddress(),owner.acquisitionAmount());
            owners.save(savedOwner);
        }
        for (VerifiedRegistryDocument.Right right : verified.rights()) {
            var savedRight = RegistryRight.create(snapshot, right.type(), right.holderName(),
                right.amount(), right.priority(), right.registeredAt(), right.status());
            savedRight.preserveSourceName(right.sourceName());
            rights.save(savedRight);
        }

        listing.markRegistryParsed(property.getId());
        document.markParsed(property.getId(), snapshot.getId(),
            verified.registryPropertyNumber(), provider, model);
        snapshots.flush();
        // Building Hub is queried only by an explicit authenticated lookup request.
    }

    private String fallback(String parsed, String original) {
        return parsed == null || parsed.isBlank() ? original : parsed.trim();
    }

    private boolean sameAddress(String left, String right) {
        return com.safehome.backend.domain.document.parser.RegistryAddressMatcher.same(left, right);
    }
}
