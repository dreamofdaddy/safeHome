package com.safehome.backend.health.service;

import com.safehome.backend.health.HealthResponse;
import org.springframework.stereotype.Service;

@Service
public class HealthService {

    public HealthResponse getHealth() {
        return new HealthResponse(
                "safehome-backend",
                "UP"
        );
    }
}