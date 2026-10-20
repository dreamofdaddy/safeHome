package com.safehome.backend.domain.auth.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(
    @NotBlank @Size(max = 254) String email,
    @NotBlank @Size(min = 12, max = 72)
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S{12,72}$")
    String password,
    @NotBlank @Size(max = 100) String name,
    @Size(max = 13) @Pattern(regexp = "^$|^010-?[0-9]{4}-?[0-9]{4}$") String phone
) {}
