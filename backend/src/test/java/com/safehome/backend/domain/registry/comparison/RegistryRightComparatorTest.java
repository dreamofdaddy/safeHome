package com.safehome.backend.domain.registry.comparison;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.dto.RegistryRightComparisonResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class RegistryRightComparatorTest {

    private final RegistryRightComparator comparator =
            new RegistryRightComparator();

    @Test
    void shouldReturnUnchangedWhenRightsAreSame() {
        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        RegistryRight previous = right(
                "MORTGAGE",
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight current = right(
                "MORTGAGE",
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(1);
        assertThat(results.get(0).changeType())
                .isEqualTo(RegistryRightChangeType.UNCHANGED);
    }

    @Test
    void shouldReturnAddedWhenRightExistsOnlyInCurrent() {
        RegistryRight current = right(
                "SEIZURE",
                "Test Creditor",
                null,
                1,
                LocalDateTime.of(2026, 10, 2, 9, 0),
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        assertThat(results.get(0).changeType())
                .isEqualTo(RegistryRightChangeType.ADDED);

        assertThat(results.get(0).previous()).isNull();
        assertThat(results.get(0).current()).isNotNull();
    }

    @Test
    void shouldReturnRemovedWhenRightExistsOnlyInPrevious() {
        RegistryRight previous = right(
                "SEIZURE",
                "Test Creditor",
                null,
                1,
                LocalDateTime.of(2026, 10, 2, 9, 0),
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of()
                );

        assertThat(results).hasSize(1);

        assertThat(results.get(0).changeType())
                .isEqualTo(RegistryRightChangeType.REMOVED);

        assertThat(results.get(0).previous()).isNotNull();
        assertThat(results.get(0).current()).isNull();
    }

    @Test
    void shouldReturnChangedWhenAmountChanges() {
        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        RegistryRight previous = right(
                "MORTGAGE",
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight current = right(
                "MORTGAGE",
                "Test Bank",
                "200000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        RegistryRightComparisonResponse result =
                results.get(0);

        assertThat(result.changeType())
                .isEqualTo(RegistryRightChangeType.CHANGED);

        assertThat(result.previous().amount())
                .isEqualByComparingTo("300000000");

        assertThat(result.current().amount())
                .isEqualByComparingTo("200000000");
    }

    @Test
    void shouldReturnChangedWhenStatusChanges() {
        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        RegistryRight previous = right(
                "MORTGAGE",
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight current = right(
                "MORTGAGE",
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        assertThat(results.get(0).changeType())
                .isEqualTo(RegistryRightChangeType.CHANGED);

        assertThat(results.get(0).previous().status())
                .isEqualTo(
                        CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
                );

        assertThat(results.get(0).current().status())
                .isEqualTo(
                        CommonCodes.REGISTRY_RIGHT_STATUS_INACTIVE
                );
    }

    @Test
    void shouldMatchExactRightBeforeChangedRightWhenSameHolderExistsMultipleTimes() {
        LocalDateTime firstRegisteredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        LocalDateTime secondRegisteredAt =
                LocalDateTime.of(2026, 10, 1, 10, 0);

        RegistryRight previousChanged = right(
                "MORTGAGE",
                "Test Bank",
                "100000000",
                1,
                firstRegisteredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight previousUnchanged = right(
                "MORTGAGE",
                "Test Bank",
                "300000000",
                2,
                secondRegisteredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight currentChanged = right(
                "MORTGAGE",
                "Test Bank",
                "150000000",
                1,
                firstRegisteredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight currentUnchanged = right(
                "MORTGAGE",
                "Test Bank",
                "300000000",
                2,
                secondRegisteredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(
                                previousChanged,
                                previousUnchanged
                        ),
                        List.of(
                                currentChanged,
                                currentUnchanged
                        )
                );

        assertThat(results).hasSize(2);

        assertThat(results)
                .anySatisfy(result -> {
                    assertThat(result.changeType())
                            .isEqualTo(
                                    RegistryRightChangeType.UNCHANGED
                            );

                    assertThat(result.previous().amount())
                            .isEqualByComparingTo("300000000");

                    assertThat(result.current().amount())
                            .isEqualByComparingTo("300000000");
                });

        assertThat(results)
                .anySatisfy(result -> {
                    assertThat(result.changeType())
                            .isEqualTo(
                                    RegistryRightChangeType.CHANGED
                            );

                    assertThat(result.previous().amount())
                            .isEqualByComparingTo("100000000");

                    assertThat(result.current().amount())
                            .isEqualByComparingTo("150000000");
                });
    }

    @Test
    void shouldIgnoreBigDecimalScaleWhenComparingAmount() {
        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        RegistryRight previous = right(
                "MORTGAGE",
                "Test Bank",
                "300000000.0",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight current = right(
                "MORTGAGE",
                "Test Bank",
                "300000000.00",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(1);

        assertThat(results.get(0).changeType())
                .isEqualTo(RegistryRightChangeType.UNCHANGED);
    }

    @Test
    void shouldNotMatchChangedRightsWhenLogicalIdentityIsMissing() {
        RegistryRight previous = right(
                "MORTGAGE",
                null,
                "300000000",
                1,
                null,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistryRight current = right(
                "MORTGAGE",
                null,
                "200000000",
                1,
                null,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        List<RegistryRightComparisonResponse> results =
                comparator.compare(
                        List.of(previous),
                        List.of(current)
                );

        assertThat(results).hasSize(2);

        assertThat(results)
                .extracting(
                        RegistryRightComparisonResponse::changeType
                )
                .containsExactlyInAnyOrder(
                        RegistryRightChangeType.ADDED,
                        RegistryRightChangeType.REMOVED
                );
    }

    private RegistryRight right(
            String rightType,
            String holderName,
            String amount,
            Integer priority,
            LocalDateTime registeredAt,
            String status
    ) {
        Property property = mock(Property.class);

        RegistrySnapshot snapshot =
                RegistrySnapshot.create(
                        property,
                        "TEST",
                        LocalDateTime.of(
                                2026,
                                10,
                                11,
                                10,
                                0
                        ),
                        "test-hash"
                );

        return RegistryRight.create(
                snapshot,
                rightType,
                holderName,
                amount == null
                        ? null
                        : new BigDecimal(amount),
                priority,
                registeredAt,
                status
        );
    }
}