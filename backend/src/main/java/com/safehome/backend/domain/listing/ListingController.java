package com.safehome.backend.domain.listing;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.listing.dto.ListingResponse;
import com.safehome.backend.domain.document.dto.ListingRegistrationRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;
    private final com.safehome.backend.domain.auth.AuthService authService;

    public ListingController(ListingService listingService, com.safehome.backend.domain.auth.AuthService authService) {
        this.listingService = listingService;
        this.authService = authService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(listingService.findAll()));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> findMine(
            @RequestHeader("Authorization") String authorization) {
        Long userId = authService.requireUser(authorization).getId();
        return ResponseEntity.ok(ApiResponse.success(
            listingService.findByRegisteredByUserId(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(listingService.findById(id)));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> findByPropertyId(
            @PathVariable Long propertyId) {
        return ResponseEntity.ok(ApiResponse.success(listingService.findByPropertyId(propertyId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> findByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(listingService.findByStatus(status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> create(
            @Valid @RequestBody ListingRegistrationRequest request,
            @RequestHeader("Authorization") String authorization) {
        if(request.addressSearchEvidence()==null)
            throw new com.safehome.backend.common.exception.BusinessException(com.safehome.backend.common.exception.ErrorCode.INVALID_REQUEST);
        return ResponseEntity.status(201).body(ApiResponse.success(
            listingService.createPendingAnalysis(authService.requireUser(authorization).getId(),request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ListingRegistrationRequest request,
            @RequestHeader("Authorization") String authorization) {

        return ResponseEntity.ok(ApiResponse.success(listingService.update(id, authService.requireUser(authorization).getId(), request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id,
            @RequestHeader("Authorization") String authorization) {
        listingService.delete(id, authService.requireUser(authorization).getId());

        return ResponseEntity.noContent().build();
    }
}
