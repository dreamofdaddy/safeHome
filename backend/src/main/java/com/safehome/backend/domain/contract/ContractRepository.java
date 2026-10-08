package com.safehome.backend.domain.contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ContractRepository extends JpaRepository<Contract, Long> {

    Optional<Contract> findTopByPropertyIdAndStatusOrderByStartDateDesc(
            Long propertyId,
            String status
    );

    List<Contract> findByPropertyIdOrderByStartDateDesc(Long propertyId);

    List<Contract> findByListingIdOrderByStartDateDesc(Long listingId);

    List<Contract> findByLandlordUserIdOrderByStartDateDesc(Long landlordUserId);

    List<Contract> findByTenantUserIdOrderByStartDateDesc(Long tenantUserId);

    List<Contract> findByStatusOrderByStartDateDesc(String status);
}