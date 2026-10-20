import { useQuery } from '@tanstack/react-query'
import { Shield } from 'lucide-react'
import { reanalysisLabel, riskLabel } from '../../lib/format'
import type { RiskLevel } from '../../types/api'
import { listingsApi } from './api'

const riskTone: Record<RiskLevel, string> = {
  LOW: 'border-emerald-200 bg-emerald-50 text-emerald-800',
  MEDIUM: 'border-amber-200 bg-amber-50 text-amber-800',
  HIGH: 'border-orange-200 bg-orange-50 text-orange-800',
  CRITICAL: 'border-red-200 bg-red-50 text-red-800',
}

export function ListingSafetyBadge({ listingId }: { listingId: number }) {
  const result = useQuery({ queryKey: ['safety-result', listingId], queryFn: () => listingsApi.safetyResult(listingId), retry: false })
  if (result.isPending) return <span className="text-[11px] text-slate-500">분석 결과 확인 중…</span>
  if (result.error || !result.data) return <span className="rounded-md bg-slate-100 px-2.5 py-1 text-[11px] text-slate-600">분석 결과 조회 실패</span>
  const { analysis, reanalysisAvailable, reanalysisReason } = result.data
  return <>
    <span className={`inline-flex items-center gap-1 rounded-md border px-2.5 py-1 text-[11px] font-bold ${riskTone[analysis.riskLevel]}`} title="저장된 분석 결과입니다. 위험점수는 높을수록 위험도가 높습니다.">
      <Shield size={13} aria-hidden="true" />위험도 {riskLabel[analysis.riskLevel]} · {analysis.riskScore}점
    </span>
    {reanalysisAvailable && <span className="rounded-md bg-amber-50 px-2.5 py-1 text-[11px] font-semibold text-amber-800" title={reanalysisLabel[reanalysisReason]}>이전 결과 · 재분석 필요</span>}
  </>
}
