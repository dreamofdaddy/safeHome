package com.safehome.backend.domain.contract;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContractSpecialTermRepository extends JpaRepository<ContractSpecialTerm, Long> {

    List<ContractSpecialTerm> findByContractIdOrderBySequenceAscIdAsc(Long contractId);
}