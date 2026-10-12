package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import com.safehome.backend.domain.risk.dto.SafetyReanalysisExecutionResponse;
import com.safehome.backend.domain.risk.policy.RiskPolicyVersionService;
import com.safehome.backend.domain.risk.reanalysis.SafetyReanalysisReason;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Transactional
@Rollback
class SafetyReanalysisServiceIntegrationTest {

    private static final Long PROPERTY_ID = 5L;

    @Autowired
    private SafetyReanalysisService safetyReanalysisService;

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
    void shouldNotCreateNewAnalysisWhenReanalysisIsNotRequired() {
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

        SafetyReanalysisExecutionResponse response =
                safetyReanalysisService
                        .reanalyzeIfRequired(PROPERTY_ID);

        long afterCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        assertThat(response.performed())
                .isFalse();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.UP_TO_DATE
                );

        assertThat(response.analysis())
                .isNull();

        assertThat(afterCount)
                .isEqualTo(beforeCount);
    }

    @Test
    void shouldCreateNewAnalysisWhenNoPreviousAnalysisExists() {
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

        SafetyReanalysisExecutionResponse response =
                safetyReanalysisService
                        .reanalyzeIfRequired(PROPERTY_ID);

        long afterCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        assertThat(response.performed())
                .isTrue();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.NO_PREVIOUS_ANALYSIS
                );

        assertThat(response.analysis())
                .isNotNull();

        assertThat(response.analysis().registrySnapshotId())
                .isEqualTo(snapshot.getId());

        assertThat(afterCount)
                .isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldCreateNewAnalysisWhenRegistryRightsChanged() {
        RegistrySnapshot previousSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-2",
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        saveRight(
                previousSnapshot,
                "300000000",
                registeredAt
        );

        saveRight(
                latestSnapshot,
                "200000000",
                registeredAt
        );

        saveAnalysis(
                previousSnapshot.getId(),
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 11, 0)
        );

        long beforeCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        SafetyReanalysisExecutionResponse response =
                safetyReanalysisService
                        .reanalyzeIfRequired(PROPERTY_ID);

        long afterCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        assertThat(response.performed())
                .isTrue();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.REGISTRY_CHANGED
                );

        assertThat(response.analysis())
                .isNotNull();

        assertThat(response.analysis().registrySnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(afterCount)
                .isEqualTo(beforeCount + 1);
    }

    @Test
    void shouldNotCreateNewAnalysisWhenNewSnapshotHasSameRights() {
        RegistrySnapshot previousSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-2",
                LocalDateTime.of(2026, 10, 12, 10, 0)
        );

        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        saveRight(
                previousSnapshot,
                "300000000",
                registeredAt
        );

        saveRight(
                latestSnapshot,
                "300000000",
                registeredAt
        );

        saveAnalysis(
                previousSnapshot.getId(),
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 11, 0)
        );

        long beforeCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        SafetyReanalysisExecutionResponse response =
                safetyReanalysisService
                        .reanalyzeIfRequired(PROPERTY_ID);

        long afterCount =
                riskAnalysisRepository
                        .findByPropertyIdOrderByAnalyzedAtDescIdDesc(
                                PROPERTY_ID
                        )
                        .size();

        assertThat(response.performed())
                .isFalse();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.UP_TO_DATE
                );

        assertThat(response.analysis())
                .isNull();

        assertThat(afterCount)
                .isEqualTo(beforeCount);
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