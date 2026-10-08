package com.safehome.backend.common.code;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.common.code.dto.CodeGroupResponse;
import com.safehome.backend.common.code.dto.CodeValueResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
public class CodeController {

    private final CodeService codeService;

    public CodeController(CodeService codeService) {
        this.codeService = codeService;
    }

    @GetMapping("/{groupCode}")
    public ResponseEntity<ApiResponse<List<CodeValueResponse>>> getCodes(
            @PathVariable String groupCode
    ) {
        List<CodeValueResponse> response = codeService
                .findActiveValues(groupCode)
                .stream()
                .map(CodeValueResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/groups")
    public ResponseEntity<ApiResponse<List<CodeGroupResponse>>> getGroups() {
        List<CodeGroupResponse> response = codeService
                .findActiveGroups()
                .stream()
                .map(CodeGroupResponse::from)
                .toList();

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}