package com.safehome.backend.health.controller;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.health.HealthResponse;
import com.safehome.backend.health.service.HealthService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
public class HealthController {

    private final HealthService healthService;

    public HealthController(HealthService healthService) {
        this.healthService = healthService;
    }

    @GetMapping
    public ApiResponse<HealthResponse> health() {
        return ApiResponse.success(
                healthService.getHealth()
        );
    }
}