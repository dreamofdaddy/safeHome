package com.safehome.backend.domain.risk;

import com.safehome.backend.common.code.CommonCodes;
import com.safehome.backend.domain.property.Property;
import com.safehome.backend.domain.property.PropertyRepository;
import com.safehome.backend.domain.registry.RegistryRight;
import com.safehome.backend.domain.registry.RegistryRightRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import com.safehome.backend.domain.risk.dto.SafetyReanalysisDecisionResponse;
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
class SafetyReanalysisDecisionServiceIntegrationTest {

    private static final Long PROPERTY_ID = 5L;
    private String previousPolicyVersion;

    @Autowired
    private SafetyReanalysisDecisionService decisionService;

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

        previousPolicyVersion =
            jdbcTemplate.queryForObject("""
                SELECT policy_version
                FROM risk_policy_versions
                WHERE policy_version <> ?
                ORDER BY id DESC
                LIMIT 1
                """,
                String.class,
                activePolicyVersion
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
    void shouldRequireAnalysisWhenNoPreviousAnalysisExists() {
        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 12, 9, 0)
        );

        SafetyReanalysisDecisionResponse response =
                decisionService.decide(PROPERTY_ID);

        assertThat(response.required()).isTrue();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.NO_PREVIOUS_ANALYSIS
                );

        assertThat(response.latestSnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(response.lastAnalyzedSnapshotId())
                .isNull();

        assertThat(response.activePolicyVersion())
                .isEqualTo(activePolicyVersion);

        assertThat(response.lastPolicyVersion())
                .isNull();
    }

    @Test
    void shouldReturnUpToDateWhenLatestSnapshotAlreadyAnalyzed() {
        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 12, 9, 0)
        );

        saveAnalysis(
                latestSnapshot.getId(),
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 12, 9, 30)
        );

        SafetyReanalysisDecisionResponse response =
                decisionService.decide(PROPERTY_ID);

        assertThat(response.required()).isFalse();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.UP_TO_DATE
                );

        assertThat(response.latestSnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(response.lastAnalyzedSnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(response.activePolicyVersion())
                .isEqualTo(activePolicyVersion);

        assertThat(response.lastPolicyVersion())
                .isEqualTo(activePolicyVersion);
    }

    @Test
    void shouldRequireAnalysisWhenPolicyVersionChanged() {
        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 12, 9, 0)
        );

        saveAnalysis(
                latestSnapshot.getId(),
                previousPolicyVersion,
                LocalDateTime.of(2026, 10, 12, 9, 30)
        );

        SafetyReanalysisDecisionResponse response =
                decisionService.decide(PROPERTY_ID);

        assertThat(response.required()).isTrue();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.POLICY_VERSION_CHANGED
                );

        assertThat(response.latestSnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(response.lastAnalyzedSnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(response.activePolicyVersion())
                .isEqualTo(activePolicyVersion);

        assertThat(response.lastPolicyVersion())
                .isEqualTo(previousPolicyVersion);
    }

    @Test
    void shouldRequireAnalysisWhenRegistryRightsChanged() {
        RegistrySnapshot previousSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-2",
                LocalDateTime.of(2026, 10, 12, 9, 0)
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
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        SafetyReanalysisDecisionResponse response =
                decisionService.decide(PROPERTY_ID);

        assertThat(response.required()).isTrue();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.REGISTRY_CHANGED
                );

        assertThat(response.latestSnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(response.lastAnalyzedSnapshotId())
                .isEqualTo(previousSnapshot.getId());
    }

    @Test
    void shouldReturnUpToDateWhenNewSnapshotHasSameRights() {
        RegistrySnapshot previousSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-2",
                LocalDateTime.of(2026, 10, 12, 9, 0)
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
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        SafetyReanalysisDecisionResponse response =
                decisionService.decide(PROPERTY_ID);

        assertThat(response.required()).isFalse();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.UP_TO_DATE
                );

        assertThat(response.latestSnapshotId())
                .isEqualTo(latestSnapshot.getId());

        assertThat(response.lastAnalyzedSnapshotId())
                .isEqualTo(previousSnapshot.getId());
    }

    @Test
    void shouldCompareLastAnalyzedSnapshotDirectlyWithLatestSnapshot() {
        RegistrySnapshot analyzedSnapshot = saveSnapshot(
                "hash-1",
                LocalDateTime.of(2026, 10, 10, 9, 0)
        );

        RegistrySnapshot intermediateSnapshot = saveSnapshot(
                "hash-2",
                LocalDateTime.of(2026, 10, 11, 9, 0)
        );

        RegistrySnapshot latestSnapshot = saveSnapshot(
                "hash-3",
                LocalDateTime.of(2026, 10, 12, 9, 0)
        );

        LocalDateTime registeredAt =
                LocalDateTime.of(2026, 10, 1, 9, 0);

        /*
         * 마지막으로 분석한 Snapshot과 최신 Snapshot은 동일.
         */
        saveRight(
                analyzedSnapshot,
                "300000000",
                registeredAt
        );

        /*
         * 중간 Snapshot만 금액이 다르다.
         */
        saveRight(
                intermediateSnapshot,
                "200000000",
                registeredAt
        );

        /*
         * 최신 Snapshot에서는 다시 300M.
         */
        saveRight(
                latestSnapshot,
                "300000000",
                registeredAt
        );

        saveAnalysis(
                analyzedSnapshot.getId(),
                activePolicyVersion,
                LocalDateTime.of(2026, 10, 10, 10, 0)
        );

        SafetyReanalysisDecisionResponse response =
                decisionService.decide(PROPERTY_ID);

        /*
         * 잘못 구현해서 최신 2개 Snapshot만 비교한다면
         * 200M -> 300M 변경으로 판단하여 REQUIRED가 된다.
         *
         * 올바른 구현은
         * analyzedSnapshot(300M) -> latestSnapshot(300M)
         * 을 비교하므로 UP_TO_DATE이다.
         */
        assertThat(response.required()).isFalse();

        assertThat(response.reason())
                .isEqualTo(
                        SafetyReanalysisReason.UP_TO_DATE
                );

        assertThat(response.lastAnalyzedSnapshotId())
                .isEqualTo(analyzedSnapshot.getId());

        assertThat(response.latestSnapshotId())
                .isEqualTo(latestSnapshot.getId());
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