package com.safehome.backend.domain.listing;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.domain.listing.dto.ListingCreateRequest;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.user.User;
import com.safehome.backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class ListingServiceValidationTest {

    @Mock
    private ListingRepository listingRepository;

    @Mock
    private PropertyRepository propertyRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private CodeService codeService;

    private ListingService listingService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        listingService = new ListingService(
                listingRepository,
                propertyRepository,
                userRepository,
                codeService
        );

        when(propertyRepository.findById(1L))
                .thenReturn(Optional.of(mock(Property.class)));
        when(userRepository.findById(1L))
                .thenReturn(Optional.of(mock(User.class)));
        when(codeService.getRequiredActiveCode(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void shouldRejectNegativeAmount() {
        ListingCreateRequest request = baseRequest();
        request.setDepositAmount(new BigDecimal("-1"));

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectJeonseWithoutDeposit() {
        ListingCreateRequest request = baseRequest();
        request.setTransactionType("JEONSE");
        request.setDepositAmount(null);

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectMonthlyRentWithoutMonthlyRent() {
        ListingCreateRequest request = baseRequest();
        request.setTransactionType("MONTHLY_RENT");
        request.setDepositAmount(new BigDecimal("50000000"));
        request.setMonthlyRent(null);

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectSaleWithoutSalePrice() {
        ListingCreateRequest request = baseRequest();
        request.setTransactionType("SALE");
        request.setSalePrice(null);

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectExpiredListingBeforeListedAt() {
        ListingCreateRequest request = baseRequest();
        request.setExpiredAt(request.getListedAt().minusHours(1));

        assertThatThrownBy(() -> listingService.create(request))
                .isInstanceOf(BusinessException.class);
    }

    private ListingCreateRequest baseRequest() {
        ListingCreateRequest request = new ListingCreateRequest();
        request.setPropertyId(1L);
        request.setSellerUserId(1L);
        request.setTransactionType("JEONSE");
        request.setDepositAmount(new BigDecimal("300000000"));
        request.setStatus("ACTIVE");
        request.setListedAt(java.time.LocalDateTime.of(2026, 10, 1, 10, 0));
        request.setExpiredAt(java.time.LocalDateTime.of(2026, 12, 31, 23, 59));
        return request;
    }
}
