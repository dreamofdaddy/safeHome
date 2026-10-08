package com.safehome.backend.domain.risk.analysis;

import com.safehome.backend.domain.contract.Contract;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.property.PropertyOwner;
import com.safehome.backend.domain.registry.RegistryRight;

import java.util.List;

public class RiskAnalysisContext {

    private final Long propertyId;
    private final Long registrySnapshotId;
    private final List<RegistryRight> registryRights;
    private final List<PropertyOwner> propertyOwners;
    private final Listing listing;
    private final Contract contract;

    public RiskAnalysisContext(
            Long propertyId,
            Long registrySnapshotId,
            List<RegistryRight> registryRights,
            List<PropertyOwner> propertyOwners
    ) {
        this(
                propertyId,
                registrySnapshotId,
                registryRights,
                propertyOwners,
                null,
                null
        );
    }

    public RiskAnalysisContext(
            Long propertyId,
            Long registrySnapshotId,
            List<RegistryRight> registryRights,
            List<PropertyOwner> propertyOwners,
            Listing listing,
            Contract contract
    ) {
        this.propertyId = propertyId;
        this.registrySnapshotId = registrySnapshotId;
        this.registryRights = List.copyOf(registryRights);
        this.propertyOwners = List.copyOf(propertyOwners);
        this.listing = listing;
        this.contract = contract;
    }

    public Long getPropertyId() {
        return propertyId;
    }

    public Long getRegistrySnapshotId() {
        return registrySnapshotId;
    }

    public List<RegistryRight> getRegistryRights() {
        return registryRights;
    }

    public List<PropertyOwner> getPropertyOwners() {
        return propertyOwners;
    }

    public Listing getListing() {
        return listing;
    }

    public Contract getContract() {
        return contract;
    }
}