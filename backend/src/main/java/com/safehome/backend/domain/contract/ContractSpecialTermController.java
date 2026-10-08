package com.safehome.backend.domain.contract;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.contract.dto.ContractSpecialTermCreateRequest;
import com.safehome.backend.domain.contract.dto.ContractSpecialTermResponse;
import com.safehome.backend.domain.contract.dto.ContractSpecialTermUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/contract-special-terms")
public class ContractSpecialTermController {

    private final ContractSpecialTermService contractSpecialTermService;

    public ContractSpecialTermController(
            ContractSpecialTermService contractSpecialTermService
    ) {
        this.contractSpecialTermService = contractSpecialTermService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractSpecialTermResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(contractSpecialTermService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractSpecialTermResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(contractSpecialTermService.findById(id)));
    }

    @GetMapping("/contract/{contractId}")
    public ResponseEntity<ApiResponse<List<ContractSpecialTermResponse>>> findByContractId(
            @PathVariable Long contractId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(contractSpecialTermService.findByContractId(contractId))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContractSpecialTermResponse>> create(
            @Valid @RequestBody ContractSpecialTermCreateRequest request
    ) {
        ContractSpecialTermResponse response =
                contractSpecialTermService.create(request);

        URI location = URI.create(
                "/api/contract-special-terms/" + response.id()
        );

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractSpecialTermResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ContractSpecialTermUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(contractSpecialTermService.update(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractSpecialTermService.delete(id);

        return ResponseEntity.noContent().build();
    }
}