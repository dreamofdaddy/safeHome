package com.safehome.backend.domain.registry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrySnapshotRepository
        extends JpaRepository<RegistrySnapshot, Long> {

    List<RegistrySnapshot> findByPropertyId(Long propertyId);

    Optional<RegistrySnapshot> findTopByPropertyIdOrderByObservedAtDesc(
            Long propertyId
    );
}