package com.safehome.backend.domain.property;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.property.dto.PropertyCreateRequest;
import com.safehome.backend.domain.property.dto.PropertyResponse;
import com.safehome.backend.domain.property.dto.PropertySafetyTrendResponse;
import com.safehome.backend.domain.property.dto.PropertyUpdateRequest;
import com.safehome.backend.domain.risk.SafetyReanalysisDecisionService;
import com.safehome.backend.domain.risk.SafetyReanalysisService;
import com.safehome.backend.domain.risk.dto.SafetyReanalysisDecisionResponse;
import com.safehome.backend.domain.risk.dto.SafetyReanalysisExecutionResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/properties")
public class PropertyController {

    private final PropertySafetyTrendService propertySafetyTrendService;
    private final PropertyService propertyService;
    private final SafetyReanalysisDecisionService safetyReanalysisDecisionService;
    private final SafetyReanalysisService safetyReanalysisService;

    public PropertyController(
            PropertyService propertyService,
            SafetyReanalysisDecisionService safetyReanalysisDecisionService,
            SafetyReanalysisService safetyReanalysisService,
            PropertySafetyTrendService propertySafetyTrendService
    ) {
        this.propertyService = propertyService;
        this.safetyReanalysisDecisionService =
                safetyReanalysisDecisionService;
        this.safetyReanalysisService =
                safetyReanalysisService;
        this.propertySafetyTrendService =
                propertySafetyTrendService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<PropertyResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(propertyService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> findById(
        @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(propertyService.findById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<PropertyResponse>> create(
        @Valid @RequestBody PropertyCreateRequest request
    ) {
        PropertyResponse response = propertyService.create(request);

        URI location = URI.create("/api/properties/" + response.id());

        return ResponseEntity
            .created(location)
            .body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<PropertyResponse>> update(
        @PathVariable Long id,
        @Valid @RequestBody PropertyUpdateRequest request
    ) {
        return ResponseEntity.ok(
            ApiResponse.success(propertyService.update(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @PathVariable Long id
    ) {
        propertyService.delete(id);

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{propertyId}/safety-reanalysis")
    public ResponseEntity<
            ApiResponse<SafetyReanalysisDecisionResponse>
            > getSafetyReanalysisDecision(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        safetyReanalysisDecisionService
                                .decide(propertyId)
                )
        );
    }

    @PostMapping("/{propertyId}/safety-reanalysis")
    public ResponseEntity<
            ApiResponse<SafetyReanalysisExecutionResponse>
            > reanalyzeSafetyIfRequired(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        safetyReanalysisService
                                .reanalyzeIfRequired(propertyId)
                )
        );
    }

    @GetMapping("/{propertyId}/safety-trend")
    public ResponseEntity<ApiResponse<PropertySafetyTrendResponse>>
    getSafetyTrend(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(
                        propertySafetyTrendService
                                .getTrend(propertyId)
                )
        );
    }
}