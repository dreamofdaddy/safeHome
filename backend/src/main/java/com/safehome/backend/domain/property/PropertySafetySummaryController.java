package com.safehome.backend.domain.property;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.property.dto.PropertySafetySummaryResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/properties")
public class PropertySafetySummaryController {

    private final PropertySafetySummaryService propertySafetySummaryService;

    public PropertySafetySummaryController(
            PropertySafetySummaryService propertySafetySummaryService
    ) {
        this.propertySafetySummaryService = propertySafetySummaryService;
    }

    @GetMapping("/{propertyId}/safety-summary")
    public ResponseEntity<ApiResponse<PropertySafetySummaryResponse>> getSafetySummary(
            @PathVariable Long propertyId
    ) {
        PropertySafetySummaryResponse response =
                propertySafetySummaryService.getSafetySummary(propertyId);

        return ResponseEntity.ok(
                ApiResponse.success(response)
        );
    }
}