package com.safehome.backend.common.code.dto;

import com.safehome.backend.common.code.CodeValue;

public record CodeValueResponse(
        Long id,
        String code,
        String codeName,
        String description,
        Integer sortOrder,
        String useYn
) {

    public static CodeValueResponse from(CodeValue codeValue) {
        return new CodeValueResponse(
                codeValue.getId(),
                codeValue.getCode(),
                codeValue.getCodeName(),
                codeValue.getDescription(),
                codeValue.getSortOrder(),
                codeValue.getUseYn()
        );
    }
}