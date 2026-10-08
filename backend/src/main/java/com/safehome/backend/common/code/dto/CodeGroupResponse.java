package com.safehome.backend.common.code.dto;

import com.safehome.backend.common.code.CodeGroup;

public record CodeGroupResponse(
        Long id,
        String groupCode,
        String groupName,
        String description,
        Integer sortOrder,
        String useYn
) {

    public static CodeGroupResponse from(CodeGroup codeGroup) {
        return new CodeGroupResponse(
                codeGroup.getId(),
                codeGroup.getGroupCode(),
                codeGroup.getGroupName(),
                codeGroup.getDescription(),
                codeGroup.getSortOrder(),
                codeGroup.getUseYn()
        );
    }
}