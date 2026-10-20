export interface ApiErrorBody { code: string; message: string }
export interface ApiResponse<T> { success: boolean; data: T; error?: ApiErrorBody }

export interface User {
  id: number; email: string; name: string; phone?: string;
  verificationStatus: string; status: string; passwordChangeRequired: boolean;
  passwordChangedAt: string; createdAt: string; updatedAt: string;
}

export interface LoginResponse { accessToken: string; tokenType: string; expiresAt: string; user: User }

export type ListingStatus = 'READY_FOR_ANALYSIS' | 'REGISTRY_PROCESSING' | 'REGISTRY_READY' | 'REGISTRY_FAILED' | 'SAFETY_ANALYZED'
export type TransactionType = 'JEONSE' | 'MONTHLY_RENT' | 'SALE'

export interface Listing {
  id: number; propertyId: number | null; riskAnalysisId: number | null; registeredByUserId: number;
  address: string; buildingName?: string; buildingType: string;
  detailAddress?: string; addressSearchEvidence?: AddressSearchEvidence | null; transactionType: TransactionType;
  depositAmount: number | null; monthlyRent: number | null; salePrice: number | null;
  status: ListingStatus; listedAt?: string; expiredAt?: string; createdAt: string; updatedAt: string;
}

export interface RegistryDocument {
  id: number; propertyId: number | null; listingId: number; originalFilename: string;
  sizeBytes: number; sha256: string; status: 'UPLOADED' | 'PROCESSING' | 'PARSED' | 'FAILED';
  registrySnapshotId: number | null; errorCode?: string; processedAt?: string; createdAt: string;
}

export interface RegistryProcessing {
  listingId: number; documentId: number; status: RegistryDocument['status']; errorCode?: string;
  propertyId: number | null; registrySnapshotId: number | null; processedAt?: string;
}

export interface RiskFactor {
  id: number; riskAnalysisId: number; factorType: string; severity: RiskLevel;
  score: number; value?: string; description: string; createdAt: string;
}
export type RiskLevel = 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
export interface SafetyAnalysis {
  id: number; propertyId: number; registrySnapshotId: number; analysisVersion: string;
  policyVersion: string; riskLevel: RiskLevel; riskScore: number; analyzedAt: string; factors: RiskFactor[];
}
export interface SafetyResult {
  listingId: number; listingStatus: ListingStatus; analysis: SafetyAnalysis; summary: string;
  highestSeverity: RiskLevel | null; factorCount: number; latestRegistrySnapshotId: number;
  reanalysisAvailable: boolean; reanalysisReason: 'NO_PREVIOUS_ANALYSIS' | 'POLICY_VERSION_CHANGED' | 'REGISTRY_CHANGED' | 'UP_TO_DATE';
}

export interface ListingMetadata {
  address: string; buildingName: string | null; buildingType: string;
  detailAddress: string | null; addressSearchEvidence?: AddressSearchEvidence | null; transactionType: TransactionType;
  depositAmount: number | null; monthlyRent: number | null; salePrice: number | null;
}

export interface AddressSearchEvidence {
  selectedType: 'R' | 'J'; selectedAddress: string; jibunAddress: string | null;
  bcode: string; sigunguCode: string; noSelected: 'Y' | 'N';
}

export interface ListingRegistration { listing: Listing; document: RegistryDocument; message: string }

export type BuildingExecutionStatus = 'NOT_REQUESTED' | 'PENDING' | 'RUNNING' | 'COMPLETED' |
  'INPUT_INCOMPLETE' | 'INPUT_CONFLICT' | 'CALL_FAILED' | 'DISABLED' | 'SUPERSEDED'
export type BuildingResolutionStatus = 'MATCHED' | 'NOT_FOUND' | 'AMBIGUOUS' | 'UNVERIFIABLE'
export interface BuildingIdentification {
  id: number | null; registrySnapshotId: number | null;
  executionStatus: BuildingExecutionStatus; resolutionStatus: BuildingResolutionStatus | null;
  reason: string | null; candidateCount: number | null;
  managementNumber: string | null; parentManagementNumber: string | null;
  unitName: string | null; source: string | null; queriedAt: string | null;
}
