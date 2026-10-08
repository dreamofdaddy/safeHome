package com.safehome.backend.domain.listing;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.common.exception.ListingNotFoundException;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.UserNotFoundException;
import com.safehome.backend.domain.listing.dto.ListingCreateRequest;
import com.safehome.backend.domain.listing.dto.ListingResponse;
import com.safehome.backend.domain.listing.dto.ListingUpdateRequest;
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

    private final CodeService codeService;
    private final ListingRepository listingRepository;
    private final PropertyRepository propertyRepository;
    private final UserRepository userRepository;

    public ListingService(
        ListingRepository listingRepository,
        PropertyRepository propertyRepository,
        UserRepository userRepository,
        CodeService codeService
    ) {
        this.listingRepository = listingRepository;
        this.propertyRepository = propertyRepository;
        this.userRepository = userRepository;
        this.codeService = codeService;
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

    public List<ListingResponse> findBySellerUserId(Long sellerUserId) {
        userRepository.findById(sellerUserId)
                .orElseThrow(() -> new UserNotFoundException(sellerUserId));

        return listingRepository.findBySellerUserIdOrderByListedAtDesc(sellerUserId)
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
    public ListingResponse create(ListingCreateRequest request) {

        propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() -> new PropertyNotFoundException(request.getPropertyId()));

        userRepository.findById(request.getSellerUserId())
                .orElseThrow(() -> new UserNotFoundException(request.getSellerUserId()));

        validateTransactionType(request.getTransactionType());
        validateStatus(request.getStatus());
        validateTransactionTerms(
                request.getTransactionType(),
                request.getDepositAmount(),
                request.getMonthlyRent(),
                request.getSalePrice(),
                request.getListedAt(),
                request.getExpiredAt()
        );
        
        Listing listing = new Listing(
                request.getPropertyId(),
                request.getSellerUserId(),
                request.getTransactionType(),
                request.getDepositAmount(),
                request.getMonthlyRent(),
                request.getSalePrice(),
                request.getStatus(),
                request.getListedAt(),
                request.getExpiredAt()
        );

        Listing saved = listingRepository.save(listing);

        return ListingResponse.from(saved);
    }

    @Transactional
    public ListingResponse update(Long id, ListingUpdateRequest request) {

        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        validateTransactionType(request.getTransactionType());
        validateStatus(request.getStatus());
        validateTransactionTerms(
                request.getTransactionType(),
                request.getDepositAmount(),
                request.getMonthlyRent(),
                request.getSalePrice(),
                request.getListedAt(),
                request.getExpiredAt()
        );

        listing.update(
                request.getTransactionType(),
                request.getDepositAmount(),
                request.getMonthlyRent(),
                request.getSalePrice(),
                request.getStatus(),
                request.getListedAt(),
                request.getExpiredAt()
        );

        return ListingResponse.from(listing);
    }

    @Transactional
    public void delete(Long id) {

        Listing listing = listingRepository.findById(id)
                .orElseThrow(() -> new ListingNotFoundException(id));

        listingRepository.delete(listing);
    }

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