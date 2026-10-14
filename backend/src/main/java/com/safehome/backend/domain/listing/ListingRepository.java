package com.safehome.backend.domain.listing;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.Optional;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select l from Listing l where l.id = :id")
    Optional<Listing> findByIdForUpdate(@Param("id") Long id);

    List<Listing> findByPropertyIdOrderByListedAtDesc(Long propertyId);

    List<Listing> findByPropertyIdAndStatusOrderByListedAtDesc(Long propertyId, String status);

    List<Listing> findByRegisteredByUserIdOrderByListedAtDesc(Long registeredByUserId);

    List<Listing> findByStatusOrderByListedAtDesc(String status);
}