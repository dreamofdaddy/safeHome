package com.safehome.backend.domain.auth;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.dto.*;
import com.safehome.backend.domain.user.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth) { this.auth = auth; }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<UserResponse>> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(201).body(ApiResponse.success(auth.signup(request)));
    }
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(ApiResponse.success(auth.login(request)));
    }
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> me(@RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(ApiResponse.success(UserResponse.from(auth.requireUserAllowExpiredPassword(authorization))));
    }
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<UserResponse>> changePassword(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(auth.changePassword(authorization, request)));
    }
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestHeader("Authorization") String authorization) {
        auth.logout(authorization);
        return ResponseEntity.noContent().build();
    }
}
