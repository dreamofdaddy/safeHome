package com.safehome.backend.integration.building;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/listings/{listingId}/building-identification")
public class BuildingIdentificationController {
    private final AuthService auth;
    private final BuildingIdentificationService service;

    public BuildingIdentificationController(AuthService auth, BuildingIdentificationService service) {
        this.auth = auth; this.service = service;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<BuildingIdentificationResponse>> current(
            @RequestHeader("Authorization") String authorization, @PathVariable Long listingId) {
        return ResponseEntity.ok(ApiResponse.success(
                service.current(auth.requireUser(authorization).getId(), listingId)));
    }

    @PostMapping("/lookup")
    public ResponseEntity<ApiResponse<BuildingIdentificationResponse>> lookup(
            @RequestHeader("Authorization") String authorization, @PathVariable Long listingId,
            @org.springframework.web.bind.annotation.RequestBody(required=false) BuildingLookupRequest request) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                service.lookup(auth.requireUser(authorization).getId(), listingId, request)));
    }

    @PostMapping("/retry")
    public ResponseEntity<ApiResponse<BuildingIdentificationResponse>> retry(
            @RequestHeader("Authorization") String authorization, @PathVariable Long listingId) {
        return ResponseEntity.accepted().body(ApiResponse.success(
                service.retry(auth.requireUser(authorization).getId(), listingId)));
    }
}
