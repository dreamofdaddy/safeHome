package com.safehome.backend.domain.risk.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RiskFactorUpdateRequest {

    @NotBlank
    @Size(max = 50)
    private String factorType;

    @NotBlank
    @Size(max = 30)
    private String severity;

    @Size(max = 500)
    private String value;

    private String description;

    public RiskFactorUpdateRequest() {
    }

    public String getFactorType() {
        return factorType;
    }

    public void setFactorType(String factorType) {
        this.factorType = factorType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}