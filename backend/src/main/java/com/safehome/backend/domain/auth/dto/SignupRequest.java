package com.safehome.backend.domain.auth.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(
    @NotBlank @Email @Size(max = 255) String email,
    @NotBlank @Size(min = 12, max = 72) String password,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 30) String phone
) {}
