package com.safehome.backend.domain.auth.dto;

import jakarta.validation.constraints.*;

public record LoginRequest(@NotBlank String email, @NotBlank String password) {}
