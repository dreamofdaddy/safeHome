package com.safehome.backend.domain.contract;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.contract.dto.ContractCreateRequest;
import com.safehome.backend.domain.contract.dto.ContractResponse;
import com.safehome.backend.domain.contract.dto.ContractUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/contracts")
public class ContractController {

    private final ContractService contractService;

    public ContractController(ContractService contractService) {
        this.contractService = contractService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ContractResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(contractService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(contractService.findById(id)));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> findByPropertyId(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(ApiResponse.success(contractService.findByPropertyId(propertyId)));
    }

    @GetMapping("/listing/{listingId}")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> findByListingId(
            @PathVariable Long listingId) {
        return ResponseEntity.ok(ApiResponse.success(contractService.findByListingId(listingId)));
    }

    @GetMapping("/landlord/{landlordUserId}")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> findByLandlordUserId(
            @PathVariable Long landlordUserId) {
        return ResponseEntity.ok(ApiResponse.success(contractService.findByLandlordUserId(landlordUserId)));
    }

    @GetMapping("/tenant/{tenantUserId}")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> findByTenantUserId(
            @PathVariable Long tenantUserId) {
        return ResponseEntity.ok(ApiResponse.success(contractService.findByTenantUserId(tenantUserId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ContractResponse>>> findByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(contractService.findByStatus(status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ContractResponse>> create(
            @Valid @RequestBody ContractCreateRequest request) {

        ContractResponse response = contractService.create(request);

        URI location = URI.create("/api/contracts/" + response.id());

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ContractResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ContractUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(contractService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        contractService.delete(id);

        return ResponseEntity.noContent().build();
    }
}