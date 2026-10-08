package com.safehome.backend.domain.property;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PropertyRepository
        extends JpaRepository<Property, Long> {
}