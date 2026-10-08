package com.safehome.backend.domain.property;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PropertyOwnerRepository
        extends JpaRepository<PropertyOwner, Long> {

    List<PropertyOwner> findByPropertyId(Long propertyId);

    List<PropertyOwner> findByUserId(Long userId);

    Optional<PropertyOwner> findByPropertyIdAndUserId(
            Long propertyId,
            Long userId
    );

    boolean existsByPropertyIdAndUserId(
            Long propertyId,
            Long userId
    );
}