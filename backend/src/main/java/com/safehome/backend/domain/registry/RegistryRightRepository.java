package com.safehome.backend.domain.registry;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistryRightRepository
        extends JpaRepository<RegistryRight, Long> {

    List<RegistryRight> findByRegistrySnapshotId(
            Long registrySnapshotId
    );

    List<RegistryRight> findByRegistrySnapshotIdAndStatus(
            Long registrySnapshotId,
            String status
    );
}