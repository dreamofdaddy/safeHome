package com.safehome.backend.domain.listing;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.ListingNotFoundException;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.UserNotFoundException;
import com.safehome.backend.domain.listing.dto.ListingResponse;
import com.safehome.backend.domain.document.dto.ListingRegistrationRequest;
import com.safehome.backend.domain.document.RegistryDocumentRepository;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ListingService {

    private final com.safehome.backend.domain.buildingregister.BuildingRegisterStore buildingRegisterDocuments;
    private final CodeService codeService;
    private final RegistryDocumentRepository documents;
    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public ListingService(
        ListingRepository listingRepository,
        PropertyRepository propertyRepository,
        UserRepository userRepository,
        CodeService codeService, RegistryDocumentRepository documents,
        com.safehome.backend.domain.buildingregister.BuildingRegisterStore buildingRegisterDocuments
    ) {
        this.listingRepository = listingRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.codeService = codeService; this.documents = documents;
        this.buildingRegisterDocuments = buildingRegisterDocuments;
    }

    public List<ListingResponse> findAll() {
        return listingRepository.findAll()
                .stream()
                .map(ListingResponse::from)
                .toList();
    }

    public ListingResponse findById(Long id) {
        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        return ListingResponse.from(listing);
    }

    public List<ListingResponse> findByPropertyId(Long propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        return listingRepository.findByPropertyIdOrderByListedAtDesc(propertyId)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }

    public List<ListingResponse> findByRegisteredByUserId(Long registeredByUserId) {
        userRepository.findById(registeredByUserId)
                .orElseThrow(() -> new UserNotFoundException(registeredByUserId));

        return listingRepository.findByRegisteredByUserIdOrderByListedAtDesc(registeredByUserId)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }

    public List<ListingResponse> findByStatus(String status) {
        
        validateStatus(status);
        
        return listingRepository.findByStatusOrderByListedAtDesc(status)
                .stream()
                .map(ListingResponse::from)
                .toList();
    }

    @Transactional
    public ListingResponse createPendingAnalysis(Long userId,
            com.safehome.backend.domain.document.dto.ListingRegistrationRequest request) {
        validateTransactionType(request.transactionType());
        validateTransactionTerms(request.transactionType(), request.depositAmount(),
            request.monthlyRent(), request.salePrice(), request.listedAt(), request.expiredAt());
        codeService.getRequiredActiveCode("PROPERTY_TYPE", request.buildingType());
        Listing listing = Listing.pendingAnalysis(userId, request.address().trim(),
            request.buildingName(), request.buildingType(), request.detailAddress(),
            request.transactionType(), request.depositAmount(), request.monthlyRent(),
            request.salePrice(), request.listedAt(), request.expiredAt());
        listing.setAddressSearchEvidence(request.addressSearchEvidence() == null ? null
            : request.addressSearchEvidence().validatedFor(request.address()));
        return ListingResponse.from(listingRepository.save(listing));
    }

    public void assertOwner(Long listingId, Long userId) {
        Listing listing = listingRepository.findById(listingId)
            .orElseThrow(() -> new ListingNotFoundException(listingId));
        if (!listing.getRegisteredByUserId().equals(userId))
            throw new BusinessException(ErrorCode.FORBIDDEN);
    }

    @Transactional
    public ListingResponse update(Long id, Long userId, ListingRegistrationRequest request) {
        Listing listing = editable(id, userId);
        validateTransactionType(request.transactionType());
        validateTransactionTerms(request.transactionType(), request.depositAmount(), request.monthlyRent(),
            request.salePrice(), request.listedAt(), request.expiredAt());
        codeService.getRequiredActiveCode("PROPERTY_TYPE", request.buildingType());
        if (!java.util.Objects.equals(clean(listing.getAddress()), clean(request.address()))
            || !java.util.Objects.equals(clean(listing.getDetailAddress()), clean(request.detailAddress()))
            || !java.util.Objects.equals(clean(listing.getBuildingName()), clean(request.buildingName()))
            || !java.util.Objects.equals(listing.getBuildingType(), request.buildingType())
            || !java.util.Objects.equals(listing.getAddressSearchEvidence(),request.addressSearchEvidence()))
            throw new BusinessException(ErrorCode.LISTING_IDENTITY_LOCKED);
        listing.reviseTerms(request);
        return ListingResponse.from(listing);
    }

    @Transactional
    public void delete(Long id, Long userId) {
        Listing listing = editable(id, userId);
        // Owner deletion hides this listing only. Retain private files/snapshots/shared property history.
        listingRepository.delete(listing);
        listingRepository.flush();
    }

    public boolean isActiveOwned(Long id,Long userId) {
        return listingRepository.findById(id).map(l->l.getRegisteredByUserId().equals(userId)).orElse(false);
    }

    private Listing editable(Long id, Long userId) {
        Listing listing = listingRepository.findByIdForUpdate(id)
            .orElseThrow(() -> new ListingNotFoundException(id));
        if (!listing.getRegisteredByUserId().equals(userId)) throw new BusinessException(ErrorCode.FORBIDDEN);
        if ("REGISTRY_PROCESSING".equals(listing.getStatus())
                || documents.findByListingId(id).stream().anyMatch(d->"PROCESSING".equals(d.getStatus()))
                || buildingRegisterDocuments.hasRunningWork(id))
            throw new BusinessException(ErrorCode.LISTING_NOT_EDITABLE);
        return listing;
    }

    private String clean(String value) { return value==null || value.isBlank()?null:value.trim(); }

    private void validateTransactionType(String transactionType) {
        if (transactionType == null || transactionType.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                CommonCodes.LISTING_TRANSACTION,
                transactionType
        );
    }


    private void validateTransactionTerms(
            String transactionType,
            BigDecimal depositAmount,
            BigDecimal monthlyRent,
            BigDecimal salePrice,
            LocalDateTime listedAt,
            LocalDateTime expiredAt
    ) {
        validateNonNegative(depositAmount);
        validateNonNegative(monthlyRent);
        validateNonNegative(salePrice);

        if (listedAt != null && expiredAt != null && expiredAt.isBefore(listedAt)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (CommonCodes.LISTING_TRANSACTION_JEONSE.equals(transactionType)
                && (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (CommonCodes.LISTING_TRANSACTION_MONTHLY_RENT.equals(transactionType)
                && (depositAmount == null || depositAmount.compareTo(BigDecimal.ZERO) <= 0
                || monthlyRent == null || monthlyRent.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (CommonCodes.LISTING_TRANSACTION_SALE.equals(transactionType)
                && (salePrice == null || salePrice.compareTo(BigDecimal.ZERO) <= 0)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateNonNegative(BigDecimal value) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                CommonCodes.LISTING_STATUS,
                status
        );
    }
}
