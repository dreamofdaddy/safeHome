package com.safehome.backend.domain.document;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RegistryDocumentRepository extends JpaRepository<RegistryDocument, Long> {
    List<RegistryDocument> findByUploadedByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<RegistryDocument> findTopByListingIdOrderByIdDesc(Long listingId);
    List<RegistryDocument> findByListingId(Long listingId);
    Optional<RegistryDocument> findFirstByListingIdAndStatusOrderByIdDesc(Long listingId,String status);
    Optional<RegistryDocument> findByListingIdAndAnalysisRequestId(Long listingId,java.util.UUID requestId);
    List<RegistryDocument> findByStatus(String status);
}
