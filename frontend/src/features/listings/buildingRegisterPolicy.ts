import type { BuildingRegisterEntry, BuildingRegisterReview } from '../../types/api'

export type RequiredRegister = 'EXCLUSIVE' | 'GENERAL' | 'EXCLUSIVE_OR_GENERAL'
export function requiredRegisterType(buildingType?: string): RequiredRegister {
  if (['APARTMENT', 'OFFICETEL', 'VILLA', 'MULTI_UNIT'].includes(buildingType || '')) return 'EXCLUSIVE'
  if (['HOUSE', 'DETACHED_HOUSE', 'MULTI_FAMILY'].includes(buildingType || '')) return 'GENERAL'
  return 'EXCLUSIVE_OR_GENERAL'
}
export function primaryEntries(entries: BuildingRegisterEntry[], required: RequiredRegister) {
  return entries.filter(entry => required === 'EXCLUSIVE_OR_GENERAL'
    ? ['EXCLUSIVE', 'GENERAL'].includes(entry.documentType) : entry.documentType === required)
}
export const requiredRegisterLabel: Record<RequiredRegister, string> = {
  EXCLUSIVE: '해당 호실 전유부', GENERAL: '일반건축물대장', EXCLUSIVE_OR_GENERAL: '전유부 또는 일반건축물대장',
}
export function requiresNewBuildingFiles(errorCode?: string) {
  return ['EXCLUSIVE_REGISTER_REQUIRED', 'GENERAL_REGISTER_REQUIRED', 'PRIMARY_REGISTER_REQUIRED'].includes(errorCode || '')
}
export function missingPrimaryRegister(review?: BuildingRegisterReview | null, buildingType?: string) {
  return requiresNewBuildingFiles(review?.errorCode) || Boolean(review?.result
    && !primaryEntries(review.result.entries, review.requiredDocumentType || requiredRegisterType(buildingType)).length)
}
