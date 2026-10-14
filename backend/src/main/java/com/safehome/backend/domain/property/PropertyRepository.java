package com.safehome.backend.domain.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PropertyRepository
        extends JpaRepository<Property, Long> {
    Optional<Property> findByRegistryPropertyNumber(String registryPropertyNumber);
}
