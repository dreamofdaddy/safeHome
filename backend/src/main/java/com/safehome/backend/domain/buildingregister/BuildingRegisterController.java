package com.safehome.backend.domain.buildingregister;

import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.AuthService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/listings/{listingId}/building-register")
public class BuildingRegisterController {
    private final BuildingRegisterService service;private final AuthService auth;
    public BuildingRegisterController(BuildingRegisterService service,AuthService auth){this.service=service;this.auth=auth;}
    public record Consent(boolean consent) {}
    @GetMapping public ApiResponse<BuildingRegisterModels.Envelope> current(@RequestHeader("Authorization") String token,@PathVariable long listingId) {
        return ApiResponse.success(service.current(auth.requireUser(token).getId(),listingId));
    }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BuildingRegisterModels.Review>> upload(@RequestHeader("Authorization") String token,@PathVariable long listingId,
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(service.upload(auth.requireUser(token).getId(),listingId,files)));
    }
    @PostMapping(path="/analysis",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<BuildingRegisterModels.Review>> uploadAndAnalyze(@RequestHeader("Authorization") String token,@PathVariable long listingId,
            @RequestPart("files") List<MultipartFile> files,@RequestParam boolean consent,@RequestParam java.util.UUID requestId) {
        return ResponseEntity.accepted().body(ApiResponse.success(service.uploadAndAnalyze(auth.requireUser(token).getId(),listingId,files,consent,requestId)));
    }
    @GetMapping("/{reviewId}") public ApiResponse<BuildingRegisterModels.Review> review(@RequestHeader("Authorization") String token,
            @PathVariable long listingId,@PathVariable long reviewId) {
        return ApiResponse.success(service.review(auth.requireUser(token).getId(),listingId,reviewId));
    }
    @PostMapping("/{reviewId}/processing") public ResponseEntity<ApiResponse<BuildingRegisterModels.Review>> start(@RequestHeader("Authorization") String token,
            @PathVariable long listingId,@PathVariable long reviewId,@RequestBody Consent body) {
        return ResponseEntity.accepted().body(ApiResponse.success(service.start(auth.requireUser(token).getId(),listingId,reviewId,body.consent())));
    }
    @GetMapping("/{reviewId}/files/{index}") public ResponseEntity<byte[]> file(@RequestHeader("Authorization") String token,
            @PathVariable long listingId,@PathVariable long reviewId,@PathVariable int index) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CACHE_CONTROL,"no-store")
                .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=building-register-"+reviewId+"-"+index+".pdf")
                .body(service.download(auth.requireUser(token).getId(),listingId,reviewId,index));
    }
}
