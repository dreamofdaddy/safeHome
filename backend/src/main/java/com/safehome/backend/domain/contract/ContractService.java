package com.safehome.backend.domain.contract;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.ContractNotFoundException;
import com.safehome.backend.common.exception.ListingNotFoundException;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.common.exception.UserNotFoundException;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.contract.dto.ContractCreateRequest;
import com.safehome.backend.domain.contract.dto.ContractResponse;
import com.safehome.backend.domain.contract.dto.ContractUpdateRequest;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ContractService {

    private final CodeService codeService;
    private final ContractRepository contractRepository;
    private final PropertyRepository propertyRepository;
    private final ListingRepository listingRepository;
    private final UserRepository userRepository;

    public ContractService(
        ContractRepository contractRepository,
        PropertyRepository propertyRepository,
        ListingRepository listingRepository,
        UserRepository userRepository,
        CodeService codeService
    ) {
        this.contractRepository = contractRepository;
        this.propertyRepository = propertyRepository;
        this.listingRepository = listingRepository;
        this.userRepository = userRepository;
        this.codeService = codeService;
    }

    public List<ContractResponse> findAll() {
        return contractRepository.findAll()
                .stream()
                .map(ContractResponse::from)
                .toList();
    }

    public ContractResponse findById(Long id) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));

        return ContractResponse.from(contract);
    }

    public List<ContractResponse> findByPropertyId(Long propertyId) {
        propertyRepository.findById(propertyId)
                .orElseThrow(() -> new PropertyNotFoundException(propertyId));

        return contractRepository.findByPropertyIdOrderByStartDateDesc(propertyId)
                .stream()
                .map(ContractResponse::from)
                .toList();
    }

    public List<ContractResponse> findByListingId(Long listingId) {
        listingRepository.findById(listingId)
                .orElseThrow(() -> new ListingNotFoundException(listingId));

        return contractRepository.findByListingIdOrderByStartDateDesc(listingId)
                .stream()
                .map(ContractResponse::from)
                .toList();
    }

    public List<ContractResponse> findByLandlordUserId(Long landlordUserId) {
        userRepository.findById(landlordUserId)
                .orElseThrow(() -> new UserNotFoundException(landlordUserId));

        return contractRepository.findByLandlordUserIdOrderByStartDateDesc(landlordUserId)
                .stream()
                .map(ContractResponse::from)
                .toList();
    }

    public List<ContractResponse> findByTenantUserId(Long tenantUserId) {
        userRepository.findById(tenantUserId)
                .orElseThrow(() -> new UserNotFoundException(tenantUserId));

        return contractRepository.findByTenantUserIdOrderByStartDateDesc(tenantUserId)
                .stream()
                .map(ContractResponse::from)
                .toList();
    }

    public List<ContractResponse> findByStatus(String status) {
        
        validateStatus(status);
        
        return contractRepository.findByStatusOrderByStartDateDesc(status)
                .stream()
                .map(ContractResponse::from)
                .toList();
    }

    @Transactional
    public ContractResponse create(ContractCreateRequest request) {

        validatePeriod(request.getStartDate(), request.getEndDate());
        validateAmounts(request.getDepositAmount(), request.getMonthlyRent());
        validateContractType(request.getContractType());
        validateStatus(request.getStatus());

        propertyRepository.findById(request.getPropertyId())
                .orElseThrow(() ->
                        new PropertyNotFoundException(request.getPropertyId()));

        if (request.getListingId() != null) {
            Listing listing = listingRepository.findById(request.getListingId())
                    .orElseThrow(() ->
                            new ListingNotFoundException(request.getListingId()));

            if (!listing.getPropertyId().equals(request.getPropertyId())) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
            }
        }

        userRepository.findById(request.getLandlordUserId())
                .orElseThrow(() ->
                        new UserNotFoundException(request.getLandlordUserId()));

        userRepository.findById(request.getTenantUserId())
                .orElseThrow(() ->
                        new UserNotFoundException(request.getTenantUserId()));

        Contract contract = new Contract(
                request.getPropertyId(),
                request.getListingId(),
                request.getLandlordUserId(),
                request.getTenantUserId(),
                request.getContractType(),
                request.getDepositAmount(),
                request.getMonthlyRent(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus(),
                request.getSignedAt()
        );

        Contract saved = contractRepository.save(contract);

        return ContractResponse.from(saved);
    }

    @Transactional
    public ContractResponse update(Long id, ContractUpdateRequest request) {

        validatePeriod(request.getStartDate(), request.getEndDate());
        validateAmounts(request.getDepositAmount(), request.getMonthlyRent());
        validateContractType(request.getContractType());
        validateStatus(request.getStatus());

        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));

        contract.update(
                request.getContractType(),
                request.getDepositAmount(),
                request.getMonthlyRent(),
                request.getStartDate(),
                request.getEndDate(),
                request.getStatus(),
                request.getSignedAt()
        );

        return ContractResponse.from(contract);
    }

    @Transactional
    public void delete(Long id) {

        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> new ContractNotFoundException(id));

        contractRepository.delete(contract);
    }


    private void validateAmounts(
            BigDecimal depositAmount,
            BigDecimal monthlyRent
    ) {
        if (depositAmount != null && depositAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        if (monthlyRent != null && monthlyRent.compareTo(BigDecimal.ZERO) < 0) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validatePeriod(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }
    }

    private void validateContractType(String contractType) {
        if (contractType == null || contractType.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                CommonCodes.CONTRACT_TYPE,
                contractType
        );
    }

    private void validateStatus(String status) {
        if (status == null || status.isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_REQUEST);
        }

        codeService.getRequiredActiveCode(
                "CONTRACT_STATUS",
                status
        );
    }
}