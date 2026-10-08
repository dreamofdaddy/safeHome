package com.safehome.backend.domain.risk;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RiskFactorRepository extends JpaRepository<RiskFactor, Long> {

    List<RiskFactor> findByRiskAnalysisIdOrderByIdAsc(Long riskAnalysisId);
}