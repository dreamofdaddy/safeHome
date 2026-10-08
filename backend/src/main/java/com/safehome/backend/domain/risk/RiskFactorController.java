package com.safehome.backend.domain.risk;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorCreateRequest;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/risk-factors")
public class RiskFactorController {

    private final RiskFactorService riskFactorService;

    public RiskFactorController(RiskFactorService riskFactorService) {
        this.riskFactorService = riskFactorService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<RiskFactorResponse>>> findAll() {
        return ResponseEntity.ok(ApiResponse.success(riskFactorService.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RiskFactorResponse>> findById(
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(ApiResponse.success(riskFactorService.findById(id)));
    }

    @GetMapping("/analysis/{riskAnalysisId}")
    public ResponseEntity<ApiResponse<List<RiskFactorResponse>>> findByRiskAnalysisId(
            @PathVariable Long riskAnalysisId
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(riskFactorService.findByRiskAnalysisId(riskAnalysisId))
        );
    }

    @PostMapping
    public ResponseEntity<ApiResponse<RiskFactorResponse>> create(
            @Valid @RequestBody RiskFactorCreateRequest request
    ) {
        RiskFactorResponse response = riskFactorService.create(request);

        URI location = URI.create("/api/risk-factors/" + response.id());

        return ResponseEntity
                .created(location)
                .body(ApiResponse.success(response));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<RiskFactorResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody RiskFactorUpdateRequest request
    ) {
        return ResponseEntity.ok(
                ApiResponse.success(riskFactorService.update(id, request))
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id
    ) {
        riskFactorService.delete(id);

        return ResponseEntity.noContent().build();
    }
}