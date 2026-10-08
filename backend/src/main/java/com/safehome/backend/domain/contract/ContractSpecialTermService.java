package com.safehome.backend.domain.contract;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.exception.ContractNotFoundException;
import com.safehome.backend.common.exception.ContractSpecialTermNotFoundException;
import com.safehome.backend.domain.contract.dto.ContractSpecialTermCreateRequest;
import com.safehome.backend.domain.contract.dto.ContractSpecialTermResponse;
import com.safehome.backend.domain.contract.dto.ContractSpecialTermUpdateRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ContractSpecialTermService {

    private final CodeService codeService;
    private final ContractSpecialTermRepository contractSpecialTermRepository;
    private final ContractRepository contractRepository;

    public ContractSpecialTermService(
        ContractSpecialTermRepository contractSpecialTermRepository,
        ContractRepository contractRepository,
        CodeService codeService
    ) {
        this.contractSpecialTermRepository = contractSpecialTermRepository;
        this.contractRepository = contractRepository;
        this.codeService = codeService;
    }

    public List<ContractSpecialTermResponse> findAll() {
        return contractSpecialTermRepository.findAll()
                .stream()
                .map(ContractSpecialTermResponse::from)
                .toList();
    }

    public ContractSpecialTermResponse findById(Long id) {
        ContractSpecialTerm term = contractSpecialTermRepository.findById(id)
                .orElseThrow(() -> new ContractSpecialTermNotFoundException(id));

        return ContractSpecialTermResponse.from(term);
    }

    public List<ContractSpecialTermResponse> findByContractId(Long contractId) {
        contractRepository.findById(contractId)
                .orElseThrow(() -> new ContractNotFoundException(contractId));

        return contractSpecialTermRepository
                .findByContractIdOrderBySequenceAscIdAsc(contractId)
                .stream()
                .map(ContractSpecialTermResponse::from)
                .toList();
    }

    @Transactional
    public ContractSpecialTermResponse create(
            ContractSpecialTermCreateRequest request
    ) {
        contractRepository.findById(request.getContractId())
                .orElseThrow(() ->
                        new ContractNotFoundException(request.getContractId()));

        codeService.getRequiredActiveCode(
                "CONTRACT_TERM_TYPE",
                request.getTermType()
        );        
        
        ContractSpecialTerm term = new ContractSpecialTerm(
                request.getContractId(),
                request.getTermType(),
                request.getContent(),
                request.getSequence()
        );

        ContractSpecialTerm saved =
                contractSpecialTermRepository.save(term);

        return ContractSpecialTermResponse.from(saved);
    }

    @Transactional
    public ContractSpecialTermResponse update(
            Long id,
            ContractSpecialTermUpdateRequest request
    ) {
        ContractSpecialTerm term =
                contractSpecialTermRepository.findById(id)
                        .orElseThrow(() ->
                                new ContractSpecialTermNotFoundException(id));

        codeService.getRequiredActiveCode(
                "CONTRACT_TERM_TYPE",
                request.getTermType()
        );

        term.update(
                request.getTermType(),
                request.getContent(),
                request.getSequence()
        );

        return ContractSpecialTermResponse.from(term);
    }

    @Transactional
    public void delete(Long id) {
        ContractSpecialTerm term =
                contractSpecialTermRepository.findById(id)
                        .orElseThrow(() ->
                                new ContractSpecialTermNotFoundException(id));

        contractSpecialTermRepository.delete(term);
    }
}