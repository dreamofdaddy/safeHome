import { useQuery, useMutation } from '@tanstack/react-query'
import { ErrorNotice, input } from '../../components/ui'
import { formatDate } from '../../lib/format'
import type { RegistryDocument } from '../../types/api'
import { listingsApi } from './api'
const statusLabel: Record<RegistryDocument['status'], string> = { UPLOADED: '파일 등록 완료 · 분석 대기', PROCESSING: '파일 분석 중', PARSED: '파일 분석 완료', FAILED: '파일 분석 실패' }
export function RegistryDocumentHistory({ listingId, selected, onSelect }: { listingId: number; selected: RegistryDocument | null; onSelect: (document: RegistryDocument | null) => void }) {
  const history = useQuery({ queryKey: ['registry-documents', listingId], queryFn: () => listingsApi.registryHistory(listingId), retry: false })
  const current = selected || history.data?.[0]
  const download = useMutation({ mutationFn: async () => {
    if (!current) return
    const blob = await listingsApi.registryDocumentFile(current.id)
    const url = URL.createObjectURL(blob), link = document.createElement('a')
    link.href = url; link.download = current.originalFilename || `registry-${current.id}.pdf`; link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } })
  return <section aria-label="등기사항증명서 문서 이력" className="mt-5">
    {Boolean(history.data?.length) && <label className="block text-xs font-bold text-slate-700">문서 이력<select className={input} value={selected?.id || ''} onChange={e => onSelect(e.target.value ? history.data?.find(doc => doc.id === Number(e.target.value)) || null : null)}><option value="">최신 문서</option>{history.data?.slice(1).map(doc => <option key={doc.id} value={doc.id}>문서 #{doc.id} · {statusLabel[doc.status]} · {formatDate(doc.createdAt)}</option>)}</select></label>}
    {current && <div className="mt-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-xs"><p className="font-extrabold text-brand-800">{statusLabel[current.status]}</p><div className="mt-2 flex flex-wrap items-center justify-between gap-2"><p className="min-w-0 break-all text-slate-600">{current.originalFilename} · {(current.sizeBytes / 1024 / 1024).toLocaleString('ko-KR', { maximumFractionDigits: 2 })}MB</p><button className="shrink-0 font-bold text-brand-700 disabled:opacity-50" disabled={download.isPending} onClick={() => download.mutate()}>PDF 다운로드</button></div><p className="mt-2 text-slate-500">등록 {formatDate(current.createdAt)}{current.processedAt && ` · 처리 ${formatDate(current.processedAt)}`}</p></div>}
    {history.isPending && <p className="text-xs text-slate-500">문서 이력 확인 중…</p>}
    <ErrorNotice error={history.error || download.error} />
  </section>
}
