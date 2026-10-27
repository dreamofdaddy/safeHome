package com.safehome.backend.domain.document;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.AuthService;
import com.safehome.backend.domain.document.dto.ListingRegistryProcessingResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listings/{listingId}/registry-processing")
public class ListingRegistryProcessingController {
    private final AuthService auth;
    private final DocumentAnalysisStateService state;

    public ListingRegistryProcessingController(AuthService auth, DocumentAnalysisStateService state) {
        this.auth = auth; this.state = state;
    }

    public record StartRequest(boolean reparseConsent) {}

    @PostMapping
    public ResponseEntity<ApiResponse<ListingRegistryProcessingResponse>> start(
            @RequestHeader("Authorization") String authorization, @PathVariable Long listingId,
            @RequestBody(required=false) StartRequest request) {
        Long userId = auth.requireUser(authorization).getId();
        return ResponseEntity.accepted().body(ApiResponse.success(state.start(userId, listingId, request!=null && request.reparseConsent())));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListingRegistryProcessingResponse>> result(
            @RequestHeader("Authorization") String authorization, @PathVariable Long listingId) {
        Long userId = auth.requireUser(authorization).getId();
        return ResponseEntity.ok(ApiResponse.success(state.result(userId, listingId)));
    }
}
