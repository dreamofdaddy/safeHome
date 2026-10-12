package com.safehome.backend.domain.registry;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class RegistrySnapshotComparisonControllerIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

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
            DELETE FROM registry_snapshots
            WHERE property_id = ?
            """,
            PROPERTY_ID
        );
    }

    @Test
    void comparison_should_return_200_with_snapshot_changes()
            throws Exception {

        RegistrySnapshot previous = saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RegistrySnapshot current = saveSnapshot(
                "OTHER",
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

        mockMvc.perform(
                get(
                        "/api/registry-snapshots/property/{propertyId}/comparison",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data").exists())
        .andExpect(jsonPath("$.data.propertyId")
                .value(PROPERTY_ID))
        .andExpect(jsonPath("$.data.previousSnapshot.id")
                .value(previous.getId()))
        .andExpect(jsonPath("$.data.currentSnapshot.id")
                .value(current.getId()))
        .andExpect(jsonPath("$.data.sourceChanged")
                .value(true))
        .andExpect(jsonPath("$.data.documentChanged")
                .value(true))
        .andExpect(jsonPath("$.data.rightsChanged")
                .value(true))
        .andExpect(jsonPath("$.data.rightChanges.length()")
                .value(1))
        .andExpect(jsonPath("$.data.rightChanges[0].changeType")
                .value("CHANGED"))
        .andExpect(jsonPath("$.data.rightChanges[0].previous.amount")
                .value(300000000))
        .andExpect(jsonPath("$.data.rightChanges[0].current.amount")
                .value(200000000));
    }

    @Test
    void comparison_should_return_200_when_rights_unchanged()
            throws Exception {

        RegistrySnapshot previous = saveSnapshot(
                "TEST",
                "same-hash",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RegistrySnapshot current = saveSnapshot(
                "TEST",
                "same-hash",
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

        mockMvc.perform(
                get(
                        "/api/registry-snapshots/property/{propertyId}/comparison",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.sourceChanged")
                .value(false))
        .andExpect(jsonPath("$.data.documentChanged")
                .value(false))
        .andExpect(jsonPath("$.data.rightsChanged")
                .value(false))
        .andExpect(jsonPath("$.data.rightChanges.length()")
                .value(1))
        .andExpect(jsonPath("$.data.rightChanges[0].changeType")
                .value("UNCHANGED"));
    }

    @Test
    void comparison_should_return_400_when_only_one_snapshot_exists()
            throws Exception {

        saveSnapshot(
                "TEST",
                "hash-1",
                LocalDateTime.of(2026, 10, 11, 10, 0)
        );

        mockMvc.perform(
                get(
                        "/api/registry-snapshots/property/{propertyId}/comparison",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-001"));
    }

    @Test
    void comparison_should_return_404_when_property_not_found()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/registry-snapshots/property/{propertyId}/comparison",
                        999999L
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success").value(false))
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-002"));
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