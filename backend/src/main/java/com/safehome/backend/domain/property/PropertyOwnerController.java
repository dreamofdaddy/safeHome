package com.safehome.backend.domain.property;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.property.dto.PropertyOwnerCreateRequest;
import com.safehome.backend.domain.property.dto.PropertyOwnerResponse;
import com.safehome.backend.domain.property.dto.PropertyOwnerUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;

import java.util.List;

@RestController
@RequestMapping("/api/property-owners")
public class PropertyOwnerController {

    private final PropertyOwnerService propertyOwnerService;

    public PropertyOwnerController(
            PropertyOwnerService propertyOwnerService
    ) {
        this.propertyOwnerService = propertyOwnerService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyOwnerResponse>> create(
            @Valid @RequestBody PropertyOwnerCreateRequest request
    ) {
        PropertyOwner propertyOwner =
                propertyOwnerService.create(
                        request.propertyId(),
                        request.userId(),
                        request.ownershipRatio()
                );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(PropertyOwnerResponse.from(propertyOwner)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyOwnerResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(
                propertyOwnerService.findAll()
                        .stream()
                        .map(PropertyOwnerResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyOwnerResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                PropertyOwnerResponse.from(propertyOwnerService.findById(id))
        ));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<PropertyOwnerResponse>>> findByPropertyId(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyOwnerService.findByPropertyId(propertyId)
                        .stream()
                        .map(PropertyOwnerResponse::from)
                        .toList()
        ));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<PropertyOwnerResponse>>> findByUserId(
            @PathVariable Long userId
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                propertyOwnerService.findByUserId(userId)
                        .stream()
                        .map(PropertyOwnerResponse::from)
                        .toList()
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyOwnerResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody PropertyOwnerUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                PropertyOwnerResponse.from(
                        propertyOwnerService.updateOwnershipRatio(id, request.ownershipRatio())
                )
        ));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<Void> verify(
            @PathVariable Long id
    ) {
        propertyOwnerService.verify(id);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        propertyOwnerService.delete(id);

        return ResponseEntity.noContent().build();
    }
}