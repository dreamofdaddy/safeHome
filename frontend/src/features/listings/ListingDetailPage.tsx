import { useEffect } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, Check, LoaderCircle, RefreshCw, ShieldAlert, X } from 'lucide-react'
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom'
import { Cell, Pie, PieChart, ResponsiveContainer } from 'recharts'
import { Button, ErrorNotice, PageHeading, cx, panel } from '../../components/ui'
import { canAnalyze, documentStatusLabel, formatBuildingType, formatDate, listingStatusLabel, ownsListing, reanalysisLabel, riskLabel, transactionLabel, won } from '../../lib/format'
import { errorStatus } from '../../lib/api/client'
import { useAuth } from '../auth/AuthProvider'
import { listingsApi } from './api'
import { RegistryProgress } from './RegistryProgress'
import { BuildingIdentificationCard } from './BuildingIdentificationCard'
import { RiskFactorCard } from './RiskFactorCard'
import { formatRiskText } from '../../lib/riskPresentation'
import type { SafetyResult } from '../../types/api'

export function ListingDetailPage() {
  const location = useLocation(); const listUrl = `/listings${location.search}`
  const navigate = useNavigate(); const id = Number(useParams().id); const { user } = useAuth(); const queryClient = useQueryClient()
  const listing = useQuery({ queryKey: ['listing', id], queryFn: () => listingsApi.one(id), enabled: Number.isFinite(id) })
  const owner = ownsListing(listing.data, user)
  const processing = useQuery({ queryKey: ['processing', id], queryFn: () => listingsApi.processing(id), enabled: owner, refetchInterval: (query) => query.state.data?.status === 'PROCESSING' ? 5000 : false })
  useEffect(() => {
    if (processing.data?.status === 'PARSED' || processing.data?.status === 'FAILED') {
      void queryClient.invalidateQueries({ queryKey: ['listing', id] })
      void queryClient.invalidateQueries({ queryKey: ['building-identification', id] })
    }
  }, [processing.data?.status, queryClient, id])
  const result = useQuery({ queryKey: ['safety-result', id], queryFn: () => listingsApi.safetyResult(id), enabled: owner, retry: (count, error) => errorStatus(error) !== 404 && count < 1 })
  const building = useQuery({ queryKey: ['building-identification', id], queryFn: () => listingsApi.buildingIdentification(id),
    enabled: owner, refetchInterval: (query) => ['PENDING', 'RUNNING'].includes(query.state.data?.executionStatus || '') ? 5000 : false })
  const refresh = async () => Promise.all([listing.refetch(), processing.refetch(), result.refetch(), building.refetch()])
  const start = useMutation({ mutationFn: () => listingsApi.startProcessing(id), onSuccess: refresh })
  const analyze = useMutation({ mutationFn: () => listingsApi.analyze(id), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['listing', id] }); await result.refetch() } })
  const removal = useMutation({ mutationFn: () => listingsApi.remove(id), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['listings'] }); await queryClient.invalidateQueries({ queryKey: ['documents'] }); navigate(listUrl) } })
  if (listing.isPending) return <div className="py-24 text-center text-sm text-slate-500">매물 정보를 불러오는 중입니다…</div>
  if (listing.error || !listing.data) return <div className="py-20"><ErrorNotice error={listing.error || new Error('매물을 찾을 수 없습니다.')} /></div>
  if (!owner) return <div className="mt-16 rounded-2xl border border-slate-200 bg-white p-16 text-center"><ShieldAlert className="mx-auto text-brand-600" size={40} /><h1 className="mt-5 text-xl font-extrabold">내 매물만 확인할 수 있습니다</h1><p className="mt-2 text-sm text-slate-500">이 매물은 현재 계정으로 등록되지 않았습니다.</p><Link to={listUrl} className="mt-6 inline-block"><Button>내 매물로 돌아가기</Button></Link></div>
  const item = listing.data; const document = processing.data; const safety = result.data
  const canStartSafety = canAnalyze(item, safety, user) && !result.isPending && (!result.error || errorStatus(result.error) === 404)
  const refreshing = listing.isFetching || processing.isFetching || result.isFetching || building.isFetching
  const canProcess = ['READY_FOR_ANALYSIS', 'REGISTRY_FAILED'].includes(item.status) && Boolean(document && ['UPLOADED', 'FAILED'].includes(document.status))
  return <>
    <PageHeading eyebrow={`LISTING #${item.id}`} title={item.buildingName || item.address} description={`${item.address}${item.detailAddress ? ` · ${item.detailAddress}` : ''}`} back={<Link to={listUrl} className="mb-5 flex items-center gap-1 text-xs text-slate-500 hover:text-brand-700"><ArrowLeft size={14} /> 내 매물</Link>} action={<span className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-bold text-brand-700">{listingStatusLabel[item.status]}</span>} />
    {['READY_FOR_ANALYSIS', 'REGISTRY_FAILED'].includes(item.status) && <section className={`${panel} mb-5`}><p className="mb-3 text-sm text-slate-600">등기부 판독 성공 전에는 입력 내용을 수정하거나 매물과 첨부 PDF를 삭제할 수 있습니다.</p><div className="flex gap-3"><Link to={`/listings/${id}/edit${location.search}`}><Button variant="secondary">매물 수정</Button></Link><Button variant="secondary" busy={removal.isPending} onClick={() => { if (window.confirm('매물과 첨부된 등기부 PDF를 삭제하시겠습니까?')) removal.mutate() }}>매물 삭제</Button></div><ErrorNotice error={removal.error} /></section>}
    <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_310px]"><div className="space-y-5"><section className={panel}><div className="flex items-center justify-between"><h2 className="text-xl font-extrabold">진행 상태</h2><button onClick={() => void refresh()} disabled={refreshing} title="서버의 진행 상태와 저장된 결과를 다시 조회합니다. 새 분석을 실행하지 않습니다." className="flex items-center gap-1 text-xs font-bold text-brand-700 disabled:opacity-50"><RefreshCw size={14} className={refreshing ? 'animate-spin' : undefined} /> {refreshing ? '상태 확인 중…' : '상태 새로고침'}</button></div><div className="my-7 space-y-0"><Step done title="매물과 PDF 등록" description="등기부 파일이 안전하게 등록되었습니다." /><Step done={document?.status === 'PARSED'} current={document?.status === 'PROCESSING'} failed={document?.status === 'FAILED'} title="등기부 판독" description={document?.status === 'FAILED' ? `판독 실패${document.errorCode ? ` · ${document.errorCode}` : ''}` : document?.status === 'PROCESSING' ? '서버가 등기부를 판독하고 있습니다.' : document?.status === 'PARSED' ? '등기부 판독이 완료되었습니다.' : '버튼을 눌러 판독을 시작하세요.'} /><Step done={Boolean(safety) && !analyze.isPending} current={analyze.isPending} title="안전성 분석" description={analyze.isPending ? '안전성 분석을 진행하고 있습니다.' : safety ? '안전성 분석이 완료되었습니다. 아래 결과를 확인해 주세요.' : result.isPending ? '저장된 분석 결과를 확인하고 있습니다.' : item.status === 'REGISTRY_READY' ? '등기부 판독이 완료되었습니다. 아래 버튼으로 안전성 분석을 시작해 주세요.' : '등기부 판독 후 시작할 수 있습니다.'} /></div>
        {canProcess && <Button variant="secondary" busy={start.isPending} onClick={() => start.mutate()}>{document?.status === 'FAILED' ? '등기부 판독 다시 시도' : '등기부 판독 시작'}</Button>}{document?.status === 'PROCESSING' && <RegistryProgress />}{canStartSafety && <Button busy={analyze.isPending} onClick={() => analyze.mutate()}>{analyze.isPending ? '안전성 분석 중…' : safety?.reanalysisAvailable ? '새 기준으로 재분석' : '안전성 분석 시작'}</Button>}{safety?.reanalysisAvailable && <p className="mt-4 rounded-lg bg-amber-50 px-4 py-3 text-xs text-amber-800">{reanalysisLabel[safety.reanalysisReason]} 아래 결과는 이전 분석입니다.</p>}<ErrorNotice error={start.error || analyze.error || (result.error && errorStatus(result.error) !== 404 ? result.error : null) || (processing.error && errorStatus(processing.error) !== 404 ? processing.error : null)} /></section>
      {building.data && <BuildingIdentificationCard result={building.data} />}
      {building.error && <ErrorNotice error={building.error} />}
      {safety && <div role="region" aria-label="안전성 분석 결과" className="scroll-mt-24 focus:outline-none"><SafetyResultCard result={safety} /></div>}</div><aside><section className={`${panel} sticky top-24`}><span className="text-[11px] font-extrabold tracking-[.2em] text-brand-500">PROPERTY INFO</span><h2 className="mt-3 text-xl font-extrabold">매물 정보</h2><dl className="mt-5 divide-y divide-slate-100 text-xs">{[["거래 유형", transactionLabel[item.transactionType]], [item.transactionType === 'SALE' ? '매매가' : '보증금', won(item.transactionType === 'SALE' ? item.salePrice : item.depositAmount)], ...(item.transactionType === 'MONTHLY_RENT' ? [['월세', won(item.monthlyRent)]] : []), ['매물주소', `${item.address} ${item.detailAddress || ''}`.trim()], ['건물 유형', formatBuildingType(item.buildingType)], ['등기부 상태', document ? documentStatusLabel[document.status] : '확인 중'], ['등록일', formatDate(item.createdAt)]].map(([term, value]) => <div key={term} className="flex justify-between gap-4 py-3"><dt className="shrink-0 text-slate-400">{term}</dt><dd className="m-0 break-words text-right font-bold text-slate-700">{value}</dd></div>)}</dl></section></aside></div>
  </>
}

function Step({ done, current, failed, title, description }: { done?: boolean; current?: boolean; failed?: boolean; title: string; description: string }) {
  return <div className="flex gap-4 pb-6 last:pb-0"><span className={cx('grid size-8 shrink-0 place-items-center rounded-full bg-slate-100 text-xs font-bold text-slate-400', done && 'bg-brand-600 text-white', current && 'bg-brand-100 text-brand-700', failed && 'bg-red-100 text-red-600')}>{done ? <Check size={15} /> : failed ? <X size={15} /> : current ? <LoaderCircle size={15} className="animate-spin" /> : '•'}</span><div><strong className="text-sm">{title}</strong><p className="mt-1 text-xs leading-5 text-slate-500">{description}</p></div></div>
}

function SafetyResultCard({ result }: { result: SafetyResult }) {
  const score = Math.max(0, Math.min(100, Number(result.analysis.riskScore)))
  const tone = result.analysis.riskLevel === 'LOW' ? '#27876c' : result.analysis.riskLevel === 'MEDIUM' ? '#d39b35' : '#d66552'
  return <section className={panel}><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start"><div><span className="text-[11px] font-extrabold tracking-[.2em] text-brand-500">SAFETY RESULT</span><h2 className="mt-2 text-xl font-extrabold">안전성 분석 결과</h2></div><span className="self-start rounded-lg px-3 py-2 text-xs font-extrabold" style={{ color: tone, backgroundColor: `${tone}16` }}>위험도 {riskLabel[result.analysis.riskLevel]}</span></div>
    <div className="mt-6 grid items-center gap-5 rounded-xl bg-slate-50 p-5 sm:grid-cols-[150px_1fr]"><div className="relative h-32"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={[{ value: score }, { value: 100 - score }]} dataKey="value" startAngle={210} endAngle={-30} innerRadius={45} outerRadius={58} stroke="none"><Cell fill={tone} /><Cell fill="#e7ece8" /></Pie></PieChart></ResponsiveContainer><div className="absolute inset-0 grid place-items-center pt-2 text-center"><div><strong className="block text-3xl">{score}</strong><span className="text-[10px] text-slate-500">위험 점수</span></div></div></div><div><p className="font-bold leading-7">{formatRiskText(result.summary)}</p><p className="mt-2 text-xs text-slate-500">분석일 {formatDate(result.analysis.analyzedAt)}</p></div></div>
    <div className="mt-5 flex flex-wrap gap-2 text-[11px] text-slate-600"><span className="rounded-md bg-slate-100 px-2.5 py-2">위험 요소 {result.factorCount}건</span><span className="rounded-md bg-slate-100 px-2.5 py-2">최고 심각도 {result.highestSeverity ? riskLabel[result.highestSeverity] : '해당 없음'}</span><span className="rounded-md bg-slate-100 px-2.5 py-2">정책 버전 {result.analysis.policyVersion}</span></div>
    <h3 className="mt-7 border-t border-slate-100 pt-6 font-extrabold">확인된 위험 요소</h3>{result.analysis.factors.length ? <div className="mt-3 space-y-3">{result.analysis.factors.map((factor) => <RiskFactorCard key={factor.id} factor={factor} />)}</div> : <p className="mt-3 text-sm text-slate-500">현재 확인된 주요 위험 요소가 없습니다.</p>}
    <p className="mt-7 text-[11px] leading-6 text-slate-400">이 결과는 등기부와 등록된 정보를 기준으로 한 참고 자료입니다. 계약 전 최신 서류와 전문가 검토를 함께 확인하세요.</p>
  </section>
}
