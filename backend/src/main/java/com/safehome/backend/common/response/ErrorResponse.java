package com.safehome.backend.common.response;

public record ErrorResponse(
        String code,
        String message
) {
}