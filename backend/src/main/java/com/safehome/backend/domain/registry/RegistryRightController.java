package com.safehome.backend.domain.registry;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.registry.dto.RegistryRightCreateRequest;
import com.safehome.backend.domain.registry.dto.RegistryRightResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/registry-rights")
public class RegistryRightController {

    private final RegistryRightService registryRightService;

    public RegistryRightController(
            RegistryRightService registryRightService
    ) {
        this.registryRightService = registryRightService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RegistryRightResponse>> create(
            @Valid @RequestBody RegistryRightCreateRequest request
    ) {
        RegistryRight registryRight =
                registryRightService.create(
                        request.registrySnapshotId(),
                        request.rightType(),
                        request.holderName(),
                        request.amount(),
                        request.priority(),
                        request.registeredAt()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(RegistryRightResponse.from(registryRight)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RegistryRightResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                registryRightService.findAll()
                        .stream()
                        .map(RegistryRightResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RegistryRightResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                RegistryRightResponse.from(registryRightService.findById(id))
        ));
    }

    @GetMapping("/snapshot/{registrySnapshotId}")
    public ResponseEntity<ApiResponse<List<RegistryRightResponse>>> findBySnapshotId(
            @PathVariable Long registrySnapshotId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                registryRightService.findBySnapshotId(registrySnapshotId)
                        .stream()
                        .map(RegistryRightResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/snapshot/{registrySnapshotId}/active")
    public ResponseEntity<ApiResponse<List<RegistryRightResponse>>> findActiveBySnapshotId(
            @PathVariable Long registrySnapshotId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                registryRightService.findActiveBySnapshotId(registrySnapshotId)
                        .stream()
                        .map(RegistryRightResponse::from)
                        .toList()
        ));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(
            @PathVariable Long id
    ) {
        registryRightService.deactivate(id);

        return ResponseEntity.noContent().build();
    }
}