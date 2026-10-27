package com.safehome.backend.domain.document;
import com.safehome.backend.common.response.ApiResponse;
import com.safehome.backend.domain.auth.AuthService;
import com.safehome.backend.domain.listing.ListingService;
import com.safehome.backend.domain.document.dto.RegistryDocumentResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@RestController
@RequestMapping("/api/listings/{listingId}/registry-documents")
public class ListingRegistryDocumentsController {
    private final AuthService auth;private final ListingService listings;private final RegistryDocumentRepository documents;
    public ListingRegistryDocumentsController(AuthService auth,ListingService listings,RegistryDocumentRepository documents){this.auth=auth;this.listings=listings;this.documents=documents;}
    @GetMapping @Transactional(readOnly=true)
    public ApiResponse<List<RegistryDocumentResponse>> history(@RequestHeader("Authorization")String token,@PathVariable long listingId){
        listings.assertOwner(listingId,auth.requireUser(token).getId());
        return ApiResponse.success(documents.findByListingId(listingId).stream().sorted(Comparator.comparing(RegistryDocument::getId).reversed()).map(RegistryDocumentResponse::from).toList());
    }
}
