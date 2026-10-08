package com.safehome.backend.domain.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskAnalysisRepository extends JpaRepository<RiskAnalysis, Long> {

    List<RiskAnalysis> findByPropertyIdOrderByAnalyzedAtDesc(Long propertyId);

    Optional<RiskAnalysis> findTopByPropertyIdOrderByAnalyzedAtDesc(Long propertyId);
}