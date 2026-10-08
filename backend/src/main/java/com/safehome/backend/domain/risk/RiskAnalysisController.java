package com.safehome.backend.domain.risk;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.risk.dto.RiskAnalysisCreateRequest;
import com.safehome.backend.domain.risk.dto.RiskAnalysisResponse;
import com.safehome.backend.domain.risk.dto.RiskAnalysisUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/risk-analyses")
public class RiskAnalysisController {

    private final RiskAnalysisService riskAnalysisService;

    public RiskAnalysisController(RiskAnalysisService riskAnalysisService) {
        this.riskAnalysisService = riskAnalysisService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RiskAnalysisResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(riskAnalysisService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RiskAnalysisResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(riskAnalysisService.findById(id)));
    }

    @GetMapping("/property/{propertyId}")
    public ResponseEntity<ApiResponse<List<RiskAnalysisResponse>>> findByPropertyId(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(riskAnalysisService.findByPropertyId(propertyId))
        );
    }

    @GetMapping("/property/{propertyId}/latest")
    public ResponseEntity<ApiResponse<RiskAnalysisResponse>> findLatestByPropertyId(
            @PathVariable Long propertyId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(riskAnalysisService.findLatestByPropertyId(propertyId))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RiskAnalysisResponse>> create(
            @Valid @RequestBody RiskAnalysisCreateRequest request
    ) {
        RiskAnalysisResponse response = riskAnalysisService.create(request);

        URI location = URI.create("/api/risk-analyses/" + response.id());

        return ResponseEntity
                .created(location)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RiskAnalysisResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RiskAnalysisUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(riskAnalysisService.update(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        riskAnalysisService.delete(id);

        return ResponseEntity.noContent().build();
    }
}