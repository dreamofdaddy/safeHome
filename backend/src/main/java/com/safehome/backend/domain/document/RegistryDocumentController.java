package com.safehome.backend.domain.document;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.AuthService;
import com.safehome.backend.domain.document.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/registry-documents")
public class RegistryDocumentController {
    private final RegistryDocumentService service;
    private final AuthService auth;
    public RegistryDocumentController(RegistryDocumentService service, AuthService auth) {
        this.service = service; this.auth = auth;
    }

    @PostMapping(path = "/listings", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ListingRegistrationResponse>> register(
            @RequestHeader("Authorization") String authorization,
            @Valid @RequestPart("metadata") ListingRegistrationRequest metadata,
            @RequestPart("registryPdf") MultipartFile registryPdf) {
        var result = service.register(auth.requireUser(authorization).getId(), metadata, registryPdf);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result));
    }

    @GetMapping("/mine")
    public ResponseEntity<ApiResponse<List<RegistryDocumentResponse>>> mine(
            @RequestHeader("Authorization") String authorization) {
        return ResponseEntity.ok(ApiResponse.success(service.mine(auth.requireUser(authorization).getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<RegistryDocumentResponse>> get(
            @RequestHeader("Authorization") String authorization, @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(service.get(auth.requireUser(authorization).getId(), id)));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@RequestHeader("Authorization") String authorization,
                                           @PathVariable Long id) {
        Long userId = auth.requireUser(authorization).getId();
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CACHE_CONTROL,"no-store")
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=registry-" + id + ".pdf")
            .body(service.download(userId, id));
    }
}
