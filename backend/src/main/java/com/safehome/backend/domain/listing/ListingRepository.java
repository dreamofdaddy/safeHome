package com.safehome.backend.domain.listing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ListingRepository extends JpaRepository<Listing, Long> {

    List<Listing> findByPropertyIdOrderByListedAtDesc(Long propertyId);

    List<Listing> findByPropertyIdAndStatusOrderByListedAtDesc(Long propertyId, String status);

    List<Listing> findBySellerUserIdOrderByListedAtDesc(Long sellerUserId);

    List<Listing> findByStatusOrderByListedAtDesc(String status);
}