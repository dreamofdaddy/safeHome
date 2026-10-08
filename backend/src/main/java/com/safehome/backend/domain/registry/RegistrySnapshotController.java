package com.safehome.backend.domain.registry;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotCreateRequest;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registry-snapshots")
public class RegistrySnapshotController {

    private final RegistrySnapshotService registrySnapshotService;

    public RegistrySnapshotController(
            RegistrySnapshotService registrySnapshotService
    ) {
        this.registrySnapshotService = registrySnapshotService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RegistrySnapshotResponse>> create(
            @Valid @RequestBody RegistrySnapshotCreateRequest request
    ) {
        RegistrySnapshot snapshot =
                registrySnapshotService.create(
                        request.propertyId(),
                        request.source(),
                        request.observedAt(),
                        request.documentHash()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(RegistrySnapshotResponse.from(snapshot)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RegistrySnapshotResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                registrySnapshotService.findAll()
                        .stream()
                        .map(RegistrySnapshotResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RegistrySnapshotResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                RegistrySnapshotResponse.from(registrySnapshotService.findById(id))
        ));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<RegistrySnapshotResponse>>> findByPropertyId(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                registrySnapshotService.findByPropertyId(propertyId)
                        .stream()
                        .map(RegistrySnapshotResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/property/{propertyId}/latest")
    public ResponseEntity<ApiResponse<RegistrySnapshotResponse>> findLatestByPropertyId(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                RegistrySnapshotResponse.from(registrySnapshotService.findLatestByPropertyId(propertyId))
        ));
    }
}