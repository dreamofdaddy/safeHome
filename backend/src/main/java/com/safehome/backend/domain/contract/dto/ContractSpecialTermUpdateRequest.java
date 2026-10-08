package com.safehome.backend.domain.contract.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ContractSpecialTermUpdateRequest {

    @NotBlank
    @Size(max = 50)
    private String termType;

    @NotBlank
    private String content;

    private Integer sequence;

    public ContractSpecialTermUpdateRequest() {
    }

    public String getTermType() {
        return termType;
    }

    public void setTermType(String termType) {
        this.termType = termType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getSequence() {
        return sequence;
    }

    public void setSequence(Integer sequence) {
        this.sequence = sequence;
    }
}