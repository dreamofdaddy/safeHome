package com.safehome.backend.domain.registry;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.PropertyNotFoundException;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.comparison.RegistryRightChangeType;
import com.safehome.backend.domain.registry.dto.RegistrySnapshotComparisonResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Rollback
class RegistrySnapshotComparisonServiceIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private RegistrySnapshotComparisonService comparisonService;

    @Autowired
    private RegistrySnapshotRepository registrySnapshotRepository;

    @Autowired
    private RegistryRightRepository registryRightRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Property property;

    @BeforeEach
    void setUp() {
        property = propertyRepository.findById(PROPERTY_ID)
                .orElseThrow();

        jdbcTemplate.update("""
            DELETE FROM registry_rights
            WHERE registry_snapshot_id IN (
                SELECT id
                FROM registry_snapshots
                WHERE property_id = ?
            )
            """,
            PROPERTY_ID
        );

        jdbcTemplate.update("""
            DELETE FROM risk_factors
            WHERE risk_analysis_id IN (
                SELECT id
                FROM risk_analyses
                WHERE property_id = ?
            )
            """,
            PROPERTY_ID
        );

        jdbcTemplate.update("""
            DELETE FROM risk_analyses
            WHERE property_id = ?
            """,
            PROPERTY_ID
        );

        jdbcTemplate.update("""
            DELETE FROM property_owners WHERE property_id = ?
            """, PROPERTY_ID);

        jdbcTemplate.update("""
            DELETE FROM registry_snapshots
            WHERE property_id = ?
            """,
            PROPERTY_ID
        );
    }

    @Test
    void shouldReturnUnchangedWhenSnapshotsHaveSameRights() {
        RegistrySnapshot previous = saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RegistrySnapshot current = saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        saveRight(
                previous,
                CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE,
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        saveRight(
                current,
                CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE,
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistrySnapshotComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.previousSnapshot().id())
                .isEqualTo(previous.getId());

        assertThat(response.currentSnapshot().id())
                .isEqualTo(current.getId());

        assertThat(response.sourceChanged()).isFalse();
        assertThat(response.documentChanged()).isFalse();
        assertThat(response.rightsChanged()).isFalse();

        assertThat(response.rightChanges())
                .hasSize(1);

        assertThat(response.rightChanges().get(0).changeType())
                .isEqualTo(
                        RegistryRightChangeType.UNCHANGED
                );
    }

    @Test
    void shouldDetectChangedRightAmount() {
        RegistrySnapshot previous = saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RegistrySnapshot current = saveSnapshot(
                "TEST",
                "hash-2",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        saveRight(
                previous,
                CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE,
                "Test Bank",
                "300000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        saveRight(
                current,
                CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE,
                "Test Bank",
                "200000000",
                1,
                registeredAt,
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistrySnapshotComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.documentChanged()).isTrue();
        assertThat(response.rightsChanged()).isTrue();

        assertThat(response.rightChanges())
                .hasSize(1);

        assertThat(response.rightChanges().get(0).changeType())
                .isEqualTo(
                        RegistryRightChangeType.CHANGED
                );

        assertThat(response.rightChanges()
                .get(0)
                .previous()
                .amount())
                .isEqualByComparingTo("300000000");

        assertThat(response.rightChanges()
                .get(0)
                .current()
                .amount())
                .isEqualByComparingTo("200000000");
    }

    @Test
    void shouldDetectAddedRight() {
        saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RegistrySnapshot current = saveSnapshot(
                "TEST",
                "hash-2",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        saveRight(
                current,
                CommonCodes.REGISTRY_RIGHT_TYPE_SEIZURE,
                "Test Creditor",
                null,
                1,
                LocalDateTime.of(2026, 10, 5, 9, 0),
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistrySnapshotComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.rightsChanged()).isTrue();

        assertThat(response.rightChanges())
                .hasSize(1);

        assertThat(response.rightChanges().get(0).changeType())
                .isEqualTo(
                        RegistryRightChangeType.ADDED
                );

        assertThat(response.rightChanges().get(0).previous())
                .isNull();

        assertThat(response.rightChanges().get(0).current())
                .isNotNull();
    }

    @Test
    void shouldDetectRemovedRight() {
        RegistrySnapshot previous = saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveSnapshot(
                "TEST",
                "hash-2",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        saveRight(
                previous,
                CommonCodes.REGISTRY_RIGHT_TYPE_SEIZURE,
                "Test Creditor",
                null,
                1,
                LocalDateTime.of(2026, 10, 5, 9, 0),
                CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
        );

        RegistrySnapshotComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.rightsChanged()).isTrue();

        assertThat(response.rightChanges())
                .hasSize(1);

        assertThat(response.rightChanges().get(0).changeType())
                .isEqualTo(
                        RegistryRightChangeType.REMOVED
                );

        assertThat(response.rightChanges().get(0).previous())
                .isNotNull();

        assertThat(response.rightChanges().get(0).current())
                .isNull();
    }

    @Test
    void shouldDetectSourceChange() {
        saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        saveSnapshot(
                "OTHER",
                "hash-1",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        RegistrySnapshotComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.sourceChanged()).isTrue();
        assertThat(response.documentChanged()).isFalse();
        assertThat(response.rightsChanged()).isFalse();
    }

    @Test
    void shouldUseHigherIdAsCurrentWhenObservedAtIsSame() {
        LocalDateTime observedAt =
                LocalDateTime.of(2026, 10, 11, 10, 0);

        RegistrySnapshot previous = saveSnapshot(
                "TEST",
                "hash-1",
                observedAt
        );

        RegistrySnapshot current = saveSnapshot(
                "TEST",
                "hash-2",
                observedAt
        );

        RegistrySnapshotComparisonResponse response =
                comparisonService.compareLatest(PROPERTY_ID);

        assertThat(response.previousSnapshot().id())
                .isEqualTo(previous.getId());

        assertThat(response.currentSnapshot().id())
                .isEqualTo(current.getId());

        assertThat(response.documentChanged())
                .isTrue();
    }

    @Test
    void shouldThrowWhenOnlyOneSnapshotExists() {
        saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        assertThatThrownBy(() ->
                comparisonService.compareLatest(PROPERTY_ID)
        )
        .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldThrowWhenNoSnapshotExists() {
        assertThatThrownBy(() ->
                comparisonService.compareLatest(PROPERTY_ID)
        )
        .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldThrowWhenPropertyDoesNotExist() {
        assertThatThrownBy(() ->
                comparisonService.compareLatest(999999L)
        )
        .isInstanceOf(PropertyNotFoundException.class);
    }

    private RegistrySnapshot saveSnapshot(
            String source,
            String documentHash,
            LocalDateTime observedAt
    ) {
        RegistrySnapshot snapshot =
                RegistrySnapshot.create(
                        property,
                        source,
                        observedAt,
                        documentHash
                );

        return registrySnapshotRepository.saveAndFlush(
                snapshot
        );
    }

    private RegistryRight saveRight(
            RegistrySnapshot snapshot,
            String rightType,
            String holderName,
            String amount,
            Integer priority,
            LocalDateTime registeredAt,
            String status
    ) {
        RegistryRight right =
                RegistryRight.create(
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

        return registryRightRepository.saveAndFlush(
                right
        );
    }
}
