package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import com.safehome.backend.domain.risk.policy.RiskPolicyVersionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SafetyReanalysisControllerIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrySnapshotRepository registrySnapshotRepository;

    @Autowired
    private RegistryRightRepository registryRightRepository;

    @Autowired
    private RiskAnalysisRepository riskAnalysisRepository;

    @Autowired
    private PropertyRepository propertyRepository;

    @Autowired
    private RiskPolicyVersionService riskPolicyVersionService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Property property;
    private String activePolicyVersion;

    @BeforeEach
    void setUp() {
        property = propertyRepository.findById(PROPERTY_ID)
                .orElseThrow();

        activePolicyVersion =
                riskPolicyVersionService.getActivePolicyVersion();

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
            DELETE FROM registry_snapshots
            WHERE property_id = ?
            """,
            PROPERTY_ID
        );
    }

    @Test
    void getDecision_shouldReturnRequiredWhenNoPreviousAnalysisExists()
            throws Exception {

        RegistrySnapshot snapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        saveRight(
                snapshot,
                "300000000",
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        mockMvc.perform(
                get(
                        "/api/properties/{propertyId}/safety-reanalysis",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.propertyId")
                .value(PROPERTY_ID))
        .andExpect(jsonPath("$.data.required")
                .value(true))
        .andExpect(jsonPath("$.data.reason")
                .value("NO_PREVIOUS_ANALYSIS"))
        .andExpect(jsonPath("$.data.latestSnapshotId")
                .value(snapshot.getId()))
        .andExpect(jsonPath("$.data.lastAnalyzedSnapshotId")
                .doesNotExist());
    }

    @Test
    void getDecision_shouldReturnUpToDateWhenLatestSnapshotAlreadyAnalyzed()
            throws Exception {

        RegistrySnapshot snapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        saveRight(
                snapshot,
                "300000000",
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        saveAnalysis(
                snapshot.getId(),
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 12, 10, 30)
        );

        mockMvc.perform(
                get(
                        "/api/properties/{propertyId}/safety-reanalysis",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.required")
                .value(false))
        .andExpect(jsonPath("$.data.reason")
                .value("UP_TO_DATE"))
        .andExpect(jsonPath("$.data.latestSnapshotId")
                .value(snapshot.getId()))
        .andExpect(jsonPath("$.data.lastAnalyzedSnapshotId")
                .value(snapshot.getId()));
    }

    @Test
    void postReanalysis_shouldNotCreateAnalysisWhenUpToDate()
            throws Exception {

        RegistrySnapshot snapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        saveRight(
                snapshot,
                "300000000",
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        saveAnalysis(
                snapshot.getId(),
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 12, 10, 30)
        );

        long beforeCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        mockMvc.perform(
                post(
                        "/api/properties/{propertyId}/safety-reanalysis",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.performed")
                .value(false))
        .andExpect(jsonPath("$.data.reason")
                .value("UP_TO_DATE"))
        .andExpect(jsonPath("$.data.analysis")
                .doesNotExist());

        long afterCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        org.assertj.core.api.Assertions.assertThat(afterCount)
                .isEqualTo(beforeCount);
    }

    @Test
    void postReanalysis_shouldCreateAnalysisWhenRequired()
            throws Exception {

        RegistrySnapshot snapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        saveRight(
                snapshot,
                "300000000",
                LocalDateTime.of(2026, 10, 1, 9, 0)
        );

        long beforeCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        mockMvc.perform(
                post(
                        "/api/properties/{propertyId}/safety-reanalysis",
                        PROPERTY_ID
                )
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.success").value(true))
        .andExpect(jsonPath("$.data.performed")
                .value(true))
        .andExpect(jsonPath("$.data.reason")
                .value("NO_PREVIOUS_ANALYSIS"))
        .andExpect(jsonPath("$.data.analysis")
                .exists())
        .andExpect(jsonPath("$.data.analysis.registrySnapshotId")
                .value(snapshot.getId()));

        long afterCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        org.assertj.core.api.Assertions.assertThat(afterCount)
                .isEqualTo(beforeCount + 1);
    }

    @Test
    void getDecision_shouldReturn404WhenPropertyDoesNotExist()
            throws Exception {

        mockMvc.perform(
                get(
                        "/api/properties/{propertyId}/safety-reanalysis",
                        999999L
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success")
                .value(false))
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-002"));
    }

    @Test
    void postReanalysis_shouldReturn404WhenPropertyDoesNotExist()
            throws Exception {

        mockMvc.perform(
                post(
                        "/api/properties/{propertyId}/safety-reanalysis",
                        999999L
                )
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.success")
                .value(false))
        .andExpect(jsonPath("$.error.code")
                .value("COMMON-002"));
    }

    private RegistrySnapshot saveSnapshot(
            String documentHash,
            LocalDateTime observedAt
    ) {
        RegistrySnapshot snapshot =
                RegistrySnapshot.create(
                        property,
                        "TEST",
                        observedAt,
                        documentHash
                );

        return registrySnapshotRepository.saveAndFlush(
                snapshot
        );
    }

    private RegistryRight saveRight(
            RegistrySnapshot snapshot,
            String amount,
            LocalDateTime registeredAt
    ) {
        RegistryRight right =
                RegistryRight.create(
                        snapshot,
                        CommonCodes.REGISTRY_RIGHT_TYPE_MORTGAGE,
                        "Test Bank",
                        new BigDecimal(amount),
                        1,
                        registeredAt,
                        CommonCodes.REGISTRY_RIGHT_STATUS_ACTIVE
                );

        return registryRightRepository.saveAndFlush(
                right
        );
    }

    private RiskAnalysis saveAnalysis(
            Long registrySnapshotId,
            String policyVersion,
            LocalDateTime analyzedAt
    ) {
        RiskAnalysis analysis =
                new RiskAnalysis(
                        PROPERTY_ID,
                        registrySnapshotId,
                        "1.0",
                        policyVersion,
                        CommonCodes.RISK_LEVEL_LOW,
                        new BigDecimal("10.00"),
                        analyzedAt
                );

        return riskAnalysisRepository.saveAndFlush(
                analysis
        );
    }
}