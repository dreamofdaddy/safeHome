package com.safehome.backend.health;

public record HealthResponse(
        String service,
        String status
) {
}