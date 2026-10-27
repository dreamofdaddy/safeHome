package com.safehome.backend.domain.listing;

import com.safehome.backend.common.code.CodeService;
import com.safehome.backend.domain.buildingregister.BuildingRegisterStore;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.domain.document.dto.ListingRegistrationRequest;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class ListingServiceValidationTest {

    @Mock private ListingRepository listingRepository;
    @Mock private PropertyRepository propertyRepository;
    @Mock private UserRepository userRepository;
    @Mock private CodeService codeService;
    @Mock private BuildingRegisterStore buildingRegisterStore;

    private ListingService listingService;

    @BeforeEach
    void setUp() {
        openMocks(this);
        listingService = new ListingService(
                listingRepository, propertyRepository, userRepository, codeService, null, buildingRegisterStore);
        when(codeService.getRequiredActiveCode(anyString(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(1));
    }

    @Test
    void shouldRejectNegativeAmount() {
        ListingRegistrationRequest request = request(
                "JEONSE", new BigDecimal("-1"), null, null, listedAt(), expiredAt());
        assertThatThrownBy(() -> listingService.createPendingAnalysis(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectJeonseWithoutDeposit() {
        ListingRegistrationRequest request = request(
                "JEONSE", null, null, null, listedAt(), expiredAt());
        assertThatThrownBy(() -> listingService.createPendingAnalysis(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectMonthlyRentWithoutMonthlyRent() {
        ListingRegistrationRequest request = request(
                "MONTHLY_RENT", new BigDecimal("50000000"), null, null, listedAt(), expiredAt());
        assertThatThrownBy(() -> listingService.createPendingAnalysis(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectSaleWithoutSalePrice() {
        ListingRegistrationRequest request = request(
                "SALE", null, null, null, listedAt(), expiredAt());
        assertThatThrownBy(() -> listingService.createPendingAnalysis(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldRejectExpiredListingBeforeListedAt() {
        ListingRegistrationRequest request = request(
                "JEONSE", new BigDecimal("300000000"), null, null,
                listedAt(), listedAt().minusHours(1));
        assertThatThrownBy(() -> listingService.createPendingAnalysis(1L, request))
                .isInstanceOf(BusinessException.class);
    }

    private ListingRegistrationRequest request(
            String transactionType,
            BigDecimal depositAmount,
            BigDecimal monthlyRent,
            BigDecimal salePrice,
            LocalDateTime listedAt,
            LocalDateTime expiredAt) {
        return new ListingRegistrationRequest(
                "서울특별시 중구 세종대로 110", "테스트 건물", "APARTMENT",
                "101호", transactionType, depositAmount, monthlyRent, salePrice,
                listedAt, expiredAt);
    }

    private LocalDateTime listedAt() {
        return LocalDateTime.of(2026, 10, 1, 10, 0);
    }

    private LocalDateTime expiredAt() {
        return LocalDateTime.of(2026, 12, 31, 23, 59);
    }
}
