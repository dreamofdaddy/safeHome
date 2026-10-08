package com.safehome.backend.domain.listing;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.listing.dto.ListingCreateRequest;
import com.safehome.backend.domain.listing.dto.ListingResponse;
import com.safehome.backend.domain.listing.dto.ListingUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/listings")
public class ListingController {

    private final ListingService listingService;

    public ListingController(ListingService listingService) {
        this.listingService = listingService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ListingResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(listingService.findAll()));
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

    @GetMapping("/seller/{sellerUserId}")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> findBySellerUserId(
            @PathVariable Long sellerUserId) {
        return ResponseEntity.ok(ApiResponse.success(listingService.findBySellerUserId(sellerUserId)));
    }

    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ListingResponse>>> findByStatus(
            @PathVariable String status) {
        return ResponseEntity.ok(ApiResponse.success(listingService.findByStatus(status)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ListingResponse>> create(
            @Valid @RequestBody ListingCreateRequest request) {

        ListingResponse response = listingService.create(request);

        URI location = URI.create("/api/listings/" + response.id());

        return ResponseEntity.created(location).body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ListingResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody ListingUpdateRequest request) {

        return ResponseEntity.ok(ApiResponse.success(listingService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        listingService.delete(id);

        return ResponseEntity.noContent().build();
    }
}