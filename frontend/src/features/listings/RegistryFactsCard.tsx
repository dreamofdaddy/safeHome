import { useQuery } from '@tanstack/react-query'
import { panel, ErrorNotice } from '../../components/ui'
import { formatDate, won } from '../../lib/format'
import { formatRiskText } from '../../lib/riskPresentation'
import { listingsApi } from './api'

export function RegistryFactsCard({ listingId, embedded = false, documentId }: { listingId: number; embedded?: boolean; documentId?: number }) {
  const query = useQuery({ queryKey: ['registry-facts', listingId, documentId], queryFn: () => listingsApi.registryFacts(listingId, documentId), retry: false })
  if (query.isPending) return <p className="text-xs text-slate-500">등기사항증명서 분석 내역 확인 중…</p>
  return <section className={embedded ? '' : panel} role="region" aria-label="등기사항증명서 분석 내역">
    {!embedded && <h2 className="text-xl font-extrabold">등기사항증명서 분석 내역</h2>}

    <ErrorNotice error={query.error} />
    {query.data && <>
      <section aria-label="현재 소유자 정보" className="mt-5">
        <div className="flex flex-wrap items-center justify-between gap-2"><h3 className="text-base font-extrabold">소유자 <span className="text-xs font-normal text-slate-500">현재 소유자만 표시</span></h3><span className="text-xs text-slate-500">갑구 · 분석된 소유자 {query.data.owners.filter(owner => owner.ownershipType !== 'CO_OWNER').length}명</span></div>
        <div className="mt-3 space-y-3">{query.data.owners.filter(owner => owner.ownershipType !== 'CO_OWNER').map((owner, index) => <article key={index} className="rounded-xl border border-slate-200 p-4">
          <dl className="grid gap-x-6 gap-y-4 text-xs sm:grid-cols-2">
            <OwnerField label="순위번호" value={owner.priorityNumber || '확인 필요'} />
            <OwnerField label={owner.entityType === 'CORPORATION' ? '소유자 법인명' : '소유자 성명'} value={owner.name} />
            <OwnerField label={owner.entityType === 'CORPORATION' ? '법인등록번호' : '주민등록번호'} value={owner.entityType === 'CORPORATION' ? '식별정보 보호로 표시하지 않음' : '개인정보 보호로 표시하지 않음'} />
            <OwnerField label="소유자 주소" value={owner.ownerAddress || '확인 필요'} />
            <OwnerField label="거래가액" value={owner.acquisitionAmount == null ? '확인 필요' : won(owner.acquisitionAmount)} />
          </dl>
        </article>)}</div>
        {!query.data.owners.some(owner => owner.ownershipType !== 'CO_OWNER') && <p className="mt-3 rounded-xl bg-slate-50 p-5 text-sm text-slate-500">현재 표시할 소유자 정보가 없습니다.</p>}
      </section>
      <h3 className="mt-5 font-bold">현재 유효한 등기 권리</h3>
      {query.data.rights.length ? <ul className="mt-2 space-y-2">{query.data.rights.map((right, index) => <li key={index} className="rounded-lg bg-slate-50 p-3 text-xs leading-6"><strong>{formatRiskText(right.type)}</strong>{right.sourceName && <span> ({right.sourceName})</span>} · {right.amount == null ? '금액 미확인' : won(right.amount)}<br />등기일 {formatDate(right.registeredAt || undefined)} · 등기 순위 {right.priority ?? '미확인'}</li>)}</ul> : <p className="mt-2 text-xs text-slate-500">분석된 유효 권리가 없습니다. 누락 여부는 원문과 함께 확인해 주세요.</p>}
    </>}
  </section>
}

function OwnerField({ label, value }: { label: string; value: string }) {
  return <div><dt className="text-slate-400">{label}</dt><dd className="mt-1.5 break-words font-semibold text-slate-700">{value}</dd></div>
}
