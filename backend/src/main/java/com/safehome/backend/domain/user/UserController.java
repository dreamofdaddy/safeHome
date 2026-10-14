package com.safehome.backend.domain.user;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.auth.AuthService;
import com.safehome.backend.domain.user.dto.UserResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final AuthService authService;

    public UserController(UserService userService, AuthService authService) {
        this.userService = userService;
        this.authService = authService;
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> findById(@PathVariable Long id,
            @RequestHeader("Authorization") String authorization) {
        assertSelf(id, authorization);
        return ResponseEntity.ok(ApiResponse.success(userService.findById(id)));
    }

    private void assertSelf(Long id, String authorization) {
        if (!authService.requireUser(authorization).getId().equals(id))
            throw new BusinessException(ErrorCode.FORBIDDEN);
    }

}
