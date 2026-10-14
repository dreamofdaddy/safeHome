package com.safehome.backend.domain.risk;

import com.safehome.backend.common.exception.BusinessException;
import com.safehome.backend.common.exception.ErrorCode;
import com.safehome.backend.domain.document.RegistryDocumentRepository;
import com.safehome.backend.domain.listing.Listing;
import com.safehome.backend.domain.listing.ListingRepository;
import com.safehome.backend.domain.registry.RegistrySnapshot;
import com.safehome.backend.domain.registry.RegistrySnapshotRepository;
import com.safehome.backend.domain.risk.dto.ListingSafetyResultResponse;
import com.safehome.backend.domain.risk.dto.RiskFactorResponse;
import com.safehome.backend.domain.risk.dto.SafetyAnalysisResponse;
import com.safehome.backend.domain.risk.policy.RiskPolicyVersionService;
import com.safehome.backend.domain.risk.reanalysis.SafetyReanalysisReason;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.Map;
import java.util.Objects;

@Service
public class ListingSafetyAnalysisService {
    private static final Map<String, Integer> SEVERITY_ORDER = Map.of(
        "LOW", 1, "MEDIUM", 2, "HIGH", 3, "CRITICAL", 4);

    private final ListingRepository listings;
    private final RegistryDocumentRepository documents;
    private final RegistrySnapshotRepository snapshots;
    private final SafetyAnalysisService safetyAnalysis;
    private final RiskAnalysisService riskAnalyses;
    private final RiskPolicyVersionService policies;

    public ListingSafetyAnalysisService(ListingRepository listings,
            RegistryDocumentRepository documents, RegistrySnapshotRepository snapshots,
            SafetyAnalysisService safetyAnalysis, RiskAnalysisService riskAnalyses,
            RiskPolicyVersionService policies) {
        this.listings = listings;
        this.documents = documents;
        this.snapshots = snapshots;
        this.safetyAnalysis = safetyAnalysis;
        this.riskAnalyses = riskAnalyses;
        this.policies = policies;
    }

    @Transactional
    public ListingSafetyResultResponse analyze(Long userId, Long listingId) {
        Listing listing = listings.findByIdForUpdate(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        assertOwner(listing, userId);
        if (listing.getPropertyId() == null || (!"REGISTRY_READY".equals(listing.getStatus())
                && !"SAFETY_ANALYZED".equals(listing.getStatus()))) {
            throw new BusinessException(ErrorCode.ANALYSIS_NOT_READY);
        }
        RegistrySnapshot latestSnapshot = latestSnapshot(listing);
        if (listing.getRiskAnalysisId() == null) {
            documents.findTopByListingIdOrderByIdDesc(listingId)
                .filter(value -> "PARSED".equals(value.getStatus()))
                .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_READY));
        } else {
            SafetyAnalysisResponse current = riskAnalyses.findResultById(listing.getRiskAnalysisId());
            if (reanalysisReason(current, latestSnapshot.getId()) == SafetyReanalysisReason.UP_TO_DATE) {
                return response(listing, current, latestSnapshot.getId());
            }
        }
        SafetyAnalysisResponse result = safetyAnalysis.analyzeForListing(
            listing, latestSnapshot.getId());
        listing.markSafetyAnalyzed(result.id());
        return response(listing, result, latestSnapshot.getId());
    }

    @Transactional(readOnly = true)
    public ListingSafetyResultResponse result(Long userId, Long listingId) {
        Listing listing = listings.findById(listingId)
            .orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
        assertOwner(listing, userId);
        if (listing.getRiskAnalysisId() == null) {
            throw new BusinessException(ErrorCode.ANALYSIS_RESULT_NOT_FOUND);
        }
        SafetyAnalysisResponse analysis = riskAnalyses.findResultById(listing.getRiskAnalysisId());
        return response(listing, analysis, latestSnapshot(listing).getId());
    }

    private void assertOwner(Listing listing, Long userId) {
        if (!listing.getRegisteredByUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }

    private RegistrySnapshot latestSnapshot(Listing listing) {
        return snapshots.findTopByPropertyIdOrderByObservedAtDescIdDesc(listing.getPropertyId())
            .orElseThrow(() -> new BusinessException(ErrorCode.ANALYSIS_NOT_READY));
    }

    private SafetyReanalysisReason reanalysisReason(SafetyAnalysisResponse analysis,
            Long latestSnapshotId) {
        if (!Objects.equals(analysis.policyVersion(), policies.getActivePolicyVersion())) {
            return SafetyReanalysisReason.POLICY_VERSION_CHANGED;
        }
        if (!Objects.equals(analysis.registrySnapshotId(), latestSnapshotId)) {
            return SafetyReanalysisReason.REGISTRY_CHANGED;
        }
        return SafetyReanalysisReason.UP_TO_DATE;
    }

    private ListingSafetyResultResponse response(Listing listing, SafetyAnalysisResponse analysis,
            Long latestSnapshotId) {
        String highestSeverity = analysis.factors().stream()
            .map(RiskFactorResponse::severity)
            .filter(SEVERITY_ORDER::containsKey)
            .max(Comparator.comparingInt(SEVERITY_ORDER::get))
            .orElse(null);
        SafetyReanalysisReason reason = reanalysisReason(analysis, latestSnapshotId);
        return new ListingSafetyResultResponse(listing.getId(), listing.getStatus(), analysis,
            summary(analysis.riskLevel(), analysis.factors().size()), highestSeverity,
            analysis.factors().size(), latestSnapshotId,
            reason != SafetyReanalysisReason.UP_TO_DATE, reason);
    }

    private String summary(String riskLevel, int factorCount) {
        return switch (riskLevel) {
            case "LOW" -> "현재 등기부에서 유효한 주요 위험 요소가 확인되지 않았습니다.";
            case "MEDIUM" -> "주의가 필요한 위험 요소가 " + factorCount + "건 확인되었습니다.";
            case "HIGH" -> "위험 요소가 " + factorCount + "건 확인되어 계약 전 상세 확인이 필요합니다.";
            case "CRITICAL" -> "중대한 위험 요소가 " + factorCount + "건 확인되어 계약 진행 전 추가 확인이 필요합니다.";
            default -> "분석 결과를 확인해 주세요.";
        };
    }
}
