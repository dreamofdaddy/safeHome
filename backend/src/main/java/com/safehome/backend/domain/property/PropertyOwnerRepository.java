package com.safehome.backend.domain.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PropertyOwnerRepository extends JpaRepository<PropertyOwner, Long> {
    List<PropertyOwner> findByPropertyId(Long propertyId);
    List<PropertyOwner> findByRegistrySnapshotId(Long registrySnapshotId);
}
