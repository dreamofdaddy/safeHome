import type { Listing, ListingStatus, RegistryDocument, RiskLevel, SafetyResult, User } from '../types/api'

export const won = (value: number | null | undefined) => value == null ? '—' : `${new Intl.NumberFormat('ko-KR').format(value)}원`
export const formatDate = (value?: string) => value ? new Intl.DateTimeFormat('ko-KR', { dateStyle: 'medium', timeStyle: 'short' }).format(new Date(value)) : '—'
export const transactionLabel = { JEONSE: '전세', MONTHLY_RENT: '월세', SALE: '매매' } as const
export const listingStatusLabel: Record<ListingStatus, string> = { READY_FOR_ANALYSIS: '등기사항증명서 분석 대기', REGISTRY_PROCESSING: '등기사항증명서 분석 중', REGISTRY_READY: '등기사항증명서 분석 완료', REGISTRY_FAILED: '등기사항증명서 분석 실패', SAFETY_ANALYZED: '안전성 분석 완료' }
export const documentStatusLabel: Record<RegistryDocument['status'], string> = { UPLOADED: '업로드 완료', PROCESSING: '분석 중', PARSED: '분석 완료', FAILED: '분석 실패' }
export const riskLabel: Record<RiskLevel, string> = { LOW: '낮음', MEDIUM: '보통', HIGH: '높음', CRITICAL: '매우 높음' }
export const reanalysisLabel: Record<SafetyResult['reanalysisReason'], string> = { NO_PREVIOUS_ANALYSIS: '최초 분석이 필요합니다.', REGISTRY_CHANGED: '새 등기사항증명서 내용이 반영되었습니다.', POLICY_VERSION_CHANGED: '평가 기준이 변경되었습니다.', ANALYSIS_VERSION_CHANGED: '분석 항목과 처리 기준이 보완되었습니다.', LISTING_CHANGED: '매물 입력 정보가 변경되었습니다.', UP_TO_DATE: '최신 분석입니다.' }
export const ownsListing = (listing: Listing | undefined, user: User | null) => Boolean(listing && user && listing.registeredByUserId === user.id)
export const canAnalyze = (listing: Listing | undefined, _result: SafetyResult | undefined, user: User | null) => ownsListing(listing, user) && ['REGISTRY_READY', 'SAFETY_ANALYZED'].includes(listing?.status || '')

const buildingTypes: Record<string, string> = {
  APARTMENT: '아파트', OFFICETEL: '오피스텔', VILLA: '빌라',
  DETACHED_HOUSE: '단독주택', HOUSE: '단독주택', MULTI_FAMILY: '다가구주택', MULTI_UNIT: '다세대주택', OTHER: '기타',
}
export const formatBuildingType = (value?: string | null) => !value ? '—' : Object.hasOwn(buildingTypes, value) ? buildingTypes[value] : '기타'
