package com.safehome.backend.domain.risk;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.risk.dto.SafetyAnalysisResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/properties")
public class SafetyAnalysisController {

    private final SafetyAnalysisService safetyAnalysisService;

    public SafetyAnalysisController(
            SafetyAnalysisService safetyAnalysisService) {
        this.safetyAnalysisService = safetyAnalysisService;
    }

    @PostMapping("/{propertyId}/safety-analysis")
    public ResponseEntity<ApiResponse<SafetyAnalysisResponse>> analyze(
            @PathVariable Long propertyId) {

        SafetyAnalysisResponse response =
                safetyAnalysisService.analyze(propertyId);

        URI location = URI.create(
                "/api/risk-analyses/" + response.id()
        );

        return ResponseEntity
                .created(location)
                .body(ApiResponse.success(response));
    }
}
