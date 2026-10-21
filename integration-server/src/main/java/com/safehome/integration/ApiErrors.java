package com.safehome.integration;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestControllerAdvice
public class ApiErrors {
    @ExceptionHandler({org.springframework.http.converter.HttpMessageNotReadableException.class,
            IllegalArgumentException.class, org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<?> invalid() {
        return ResponseEntity.badRequest().body(Map.of("success", false, "error",
                Map.of("code", "INTEGRATION-001", "message", "Invalid building identification")));
    }
}
