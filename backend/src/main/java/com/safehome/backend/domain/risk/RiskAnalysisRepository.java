package com.safehome.backend.domain.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RiskAnalysisRepository extends JpaRepository<RiskAnalysis, Long> {

    List<RiskAnalysis> findByPropertyIdOrderByAnalyzedAtDescIdDesc(Long propertyId);

    Optional<RiskAnalysis> findTopByPropertyIdOrderByAnalyzedAtDescIdDesc(Long propertyId);

    List<RiskAnalysis> findTop2ByPropertyIdOrderByAnalyzedAtDescIdDesc(Long propertyId);
}