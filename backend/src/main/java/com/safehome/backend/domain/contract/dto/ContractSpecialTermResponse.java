package com.safehome.backend.domain.contract.dto;

import com.safehome.backend.domain.contract.ContractSpecialTerm;

import java.time.LocalDateTime;

public record ContractSpecialTermResponse(
        Long id,
        Long contractId,
        String termType,
        String content,
        Integer sequence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ContractSpecialTermResponse from(
            ContractSpecialTerm term
    ) {
        return new ContractSpecialTermResponse(
                term.getId(),
                term.getContractId(),
                term.getTermType(),
                term.getContent(),
                term.getSequence(),
                term.getCreatedAt(),
                term.getUpdatedAt()
        );
    }
}