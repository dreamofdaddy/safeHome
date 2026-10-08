package com.safehome.backend.domain.contract;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.domain.contract.dto.ContractCreateRequest;
import com.safehome.backend.domain.listing.ListingRepository;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.user.User;
import com.safehome.backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class ContractServiceValidationTest {

    @Mock
    private ContractRepository contractRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CodeService codeService;

    private ContractService contractService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        contractService = new ContractService(
                contractRepository,
                propertyRepository,
                listingRepository,
                userRepository,
                codeService
        );

        when(propertyRepository.findById(1L))
                .thenReturn(Optional.of(mock(Property.class)));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mock(User.class)));
        when(userRepository.findById(2L))
                .thenReturn(Optional.of(mock(User.class)));
        when(codeService.getRequiredActiveCode(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void shouldRejectNegativeDeposit() {
        ContractCreateRequest request = baseRequest();
        request.setDepositAmount(new BigDecimal("-1"));

        assertThatThrownBy(() -> contractService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectNegativeMonthlyRent() {
        ContractCreateRequest request = baseRequest();
        request.setMonthlyRent(new BigDecimal("-1"));

        assertThatThrownBy(() -> contractService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectEndDateBeforeStartDate() {
        ContractCreateRequest request = baseRequest();
        request.setStartDate(LocalDate.of(2026, 12, 31));
        request.setEndDate(LocalDate.of(2026, 12, 30));

        assertThatThrownBy(() -> contractService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    private ContractCreateRequest baseRequest() {
        ContractCreateRequest request = new ContractCreateRequest();
        request.setPropertyId(1L);
        request.setLandlordUserId(1L);
        request.setTenantUserId(2L);
        request.setContractType("JEONSE");
        request.setDepositAmount(new BigDecimal("300000000"));
        request.setStartDate(LocalDate.of(2026, 10, 1));
        request.setEndDate(LocalDate.of(2027, 9, 30));
        request.setStatus("ACTIVE");
        return request;
    }
}
