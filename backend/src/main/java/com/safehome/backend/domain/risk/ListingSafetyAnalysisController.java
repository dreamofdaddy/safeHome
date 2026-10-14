package com.safehome.backend.domain.risk;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.AuthService;
import com.safehome.backend.domain.risk.dto.ListingSafetyResultResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/listings/{listingId}/safety-analysis")
public class ListingSafetyAnalysisController {
    private final AuthService auth;
    private final ListingSafetyAnalysisService analysis;

    public ListingSafetyAnalysisController(AuthService auth, ListingSafetyAnalysisService analysis) {
        this.auth = auth;
        this.analysis = analysis;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ListingSafetyResultResponse>> analyze(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long listingId) {
        Long userId = auth.requireUser(authorization).getId();
        return ResponseEntity.ok(ApiResponse.success(analysis.analyze(userId, listingId)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<ListingSafetyResultResponse>> result(
            @RequestHeader("Authorization") String authorization,
            @PathVariable Long listingId) {
        Long userId = auth.requireUser(authorization).getId();
        return ResponseEntity.ok(ApiResponse.success(analysis.result(userId, listingId)));
    }
}
