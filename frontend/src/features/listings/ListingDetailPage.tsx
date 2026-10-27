import { useEffect, useRef, useState, type ReactNode } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ArrowLeft, CircleAlert, CircleCheck, CircleDashed, CircleX, Clock3, FileUp, LoaderCircle, RefreshCw, ShieldAlert } from 'lucide-react'
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom'
import { Cell, Pie, PieChart, ResponsiveContainer } from 'recharts'
import { Button, ErrorNotice, PageHeading, cx, panel } from '../../components/ui'
import { canAnalyze, documentStatusLabel, formatBuildingType, formatDate, listingStatusLabel, ownsListing, reanalysisLabel, riskLabel, transactionLabel, won } from '../../lib/format'
import { errorStatus } from '../../lib/api/client'
import { useAuth } from '../auth/AuthProvider'
import { listingsApi } from './api'
import { registryPdfError } from './registryPdf'
import { RegistryAnalysisFailure, RegistryAnalysisFailureDialog, registryFailureMessage } from './RegistryAnalysisFailure'
import { RegistryProgress } from './RegistryProgress'
import { BuildingIdentificationCard } from './BuildingIdentificationCard'
import { BuildingRegisterCard } from './BuildingRegisterCard'
import { RegistryDocumentHistory } from './RegistryDocumentHistory'
import { RegistryFactsCard } from './RegistryFactsCard'
import { missingPrimaryRegister } from './buildingRegisterPolicy'
import { RiskFactorCard } from './RiskFactorCard'
import { formatRiskText } from '../../lib/riskPresentation'
import type { RegistryDocument, SafetyResult } from '../../types/api'

export function ListingDetailPage() {
  const [selectedRegistryFile, setSelectedRegistryFile] = useState<File | null>(null)
  const [registryFileError, setRegistryFileError] = useState<string>()
  const [checkingRegistryFile, setCheckingRegistryFile] = useState(false)
  const [selectedRegistryDocument, setSelectedRegistryDocument] = useState<RegistryDocument | null>(null)
  const [dismissedFailure, setDismissedFailure] = useState<string | null>(null)
  const registryRequestId = useRef<string | null>(null)
  const registryFileCheck = useRef(0)
  const registryFileInput = useRef<HTMLInputElement>(null)
  const submittedRegistryDocument = useRef<number | null>(null)
  const location = useLocation(); const listUrl = `/listings${location.search}`
  const navigate = useNavigate(); const id = Number(useParams().id); const { user } = useAuth(); const queryClient = useQueryClient()
  const listing = useQuery({ queryKey: ['listing', id], queryFn: () => listingsApi.one(id), enabled: Number.isFinite(id) })
  const owner = ownsListing(listing.data, user)
  const processing = useQuery({ queryKey: ['processing', id], queryFn: () => listingsApi.processing(id), enabled: owner, retry: (count, error) => errorStatus(error) !== 404 && count < 1, refetchInterval: (query) => query.state.data?.status === 'PROCESSING' ? 5000 : false })
  useEffect(() => {
    if (processing.data?.status === 'PARSED' || processing.data?.status === 'FAILED') {
      void queryClient.invalidateQueries({ queryKey: ['listing', id] })
      void queryClient.invalidateQueries({ queryKey: ['building-identification', id] })
      void queryClient.invalidateQueries({ queryKey: ['registry-facts', id] })
      void queryClient.invalidateQueries({ queryKey: ['registry-documents', id] })
      void queryClient.invalidateQueries({ queryKey: ['safety-result', id] })
      void queryClient.invalidateQueries({ queryKey: ['building-register', id] })
    }
  }, [processing.data?.status, processing.data?.documentId, queryClient, id])
  const result = useQuery({ queryKey: ['safety-result', id], queryFn: () => listingsApi.safetyResult(id), enabled: owner, retry: (count, error) => errorStatus(error) !== 404 && count < 1 })
  const building = useQuery({ queryKey: ['building-identification', id], queryFn: () => listingsApi.buildingIdentification(id),
    enabled: owner, refetchInterval: (query) => ['PENDING', 'RUNNING'].includes(query.state.data?.executionStatus || '') ? 5000 : false })
  const buildingRegister = useQuery({ queryKey: ['building-register', id], queryFn: () => listingsApi.buildingRegister(id), enabled: owner,
    refetchInterval: query => query.state.data?.latest?.status === 'PROCESSING' ? 3000 : false })
  const refresh = async () => Promise.all([listing.refetch(), processing.refetch(), result.refetch(), building.refetch(), buildingRegister.refetch()])
  const registrySubmission = useMutation({ mutationFn: async () => {
    if (!selectedRegistryFile) throw new Error('등기사항증명서 PDF를 선택해 주세요.')
    const error = await registryPdfError(selectedRegistryFile); if (error) throw new Error(error)
    registryRequestId.current ||= crypto.randomUUID()
    return listingsApi.uploadAndAnalyzeRegistry(id, selectedRegistryFile, registryRequestId.current)
  }, onSuccess: async data => {
    setSelectedRegistryDocument(null)
    submittedRegistryDocument.current = data.documentId
    queryClient.setQueryData(['processing', id], data)
    await queryClient.invalidateQueries({ queryKey: ['listing', id] })
    await queryClient.invalidateQueries({ queryKey: ['documents'] })
    await queryClient.invalidateQueries({ queryKey: ['registry-documents', id] })
    if (['PARSED', 'FAILED'].includes(data.status)) { registryRequestId.current = null; await processing.refetch() }
  } })
  useEffect(() => queryClient.getQueryCache().subscribe(event => {
    const key = event.query.queryKey
    if (key[0] !== 'processing' || key[1] !== id) return
    const completed = event.query.state.data as { documentId?: number; status?: string } | undefined
    if (submittedRegistryDocument.current == null || completed?.documentId !== submittedRegistryDocument.current || !['PARSED', 'FAILED'].includes(completed?.status || '')) return
    submittedRegistryDocument.current = null
    registryRequestId.current = null
    registryFileCheck.current++
    setSelectedRegistryFile(null); setRegistryFileError(undefined); setCheckingRegistryFile(false)
    if (registryFileInput.current) registryFileInput.current.value = ''
  }), [queryClient, id])
  const retryBuilding = useMutation({ mutationFn: (input?: import('../../types/api').BuildingLookupInput) => listingsApi.retryBuildingIdentification(id, input),
    onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['building-identification', id] }) } })
  const analyze = useMutation({ mutationFn: () => listingsApi.analyze(id), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['listing', id] }); await result.refetch() } })
  const removal = useMutation({ mutationFn: () => listingsApi.remove(id), onSuccess: async () => { await queryClient.invalidateQueries({ queryKey: ['listings'] }); await queryClient.invalidateQueries({ queryKey: ['documents'] }); navigate(listUrl) } })
  if (listing.isPending) return <div className="py-24 text-center text-sm text-slate-500">매물 정보를 불러오는 중입니다…</div>
  if (listing.error || !listing.data) return <div className="py-20"><ErrorNotice error={listing.error || new Error('매물을 찾을 수 없습니다.')} /></div>
  if (!owner) return <div className="mt-16 rounded-2xl border border-slate-200 bg-white p-16 text-center"><ShieldAlert className="mx-auto text-brand-600" size={40} /><h1 className="mt-5 text-xl font-extrabold">내 매물만 확인할 수 있습니다</h1><p className="mt-2 text-sm text-slate-500">이 매물은 현재 계정으로 등록되지 않았습니다.</p><Link to={listUrl} className="mt-6 inline-block"><Button>내 매물로 돌아가기</Button></Link></div>
  const item = listing.data; const document = processing.data; const safety = result.data
  const deferredOwnershipResult = Boolean(safety?.analysis.factors.some(factor => factor.factorType === 'CO_OWNER_ISSUE'))
  const canStartSafety = document?.status === 'PARSED' && !registrySubmission.isPending && canAnalyze(item, safety, user) && !result.isPending && (!result.error || errorStatus(result.error) === 404)
  const refreshing = listing.isFetching || processing.isFetching || result.isFetching || building.isFetching
  const registryRunning = registrySubmission.isPending || document?.status === 'PROCESSING'
  const failureKey = document?.status === 'FAILED' ? `safehome.registry-failure.${id}.${document.documentId}.${document.processedAt || document.errorCode}` : ''
  const failureDialogOpen = Boolean(failureKey && !registryRunning && dismissedFailure !== failureKey && sessionStorage.getItem(failureKey) !== 'acknowledged')
  const registryNeedsReview = Boolean(item.propertyId) && item.status === 'READY_FOR_ANALYSIS'
  const registryFailed = !registryRunning && (document?.status === 'FAILED' || (processing.isError && errorStatus(processing.error) !== 404 && !document))
  const analysisFailed = !analyze.isPending && (analyze.isError || (!safety && result.isError && errorStatus(result.error) !== 404))
  const buildingRunning = retryBuilding.isPending || ['PENDING', 'RUNNING'].includes(building.data?.executionStatus || '') || buildingRegister.data?.latest?.status === 'PROCESSING'
  const canModify = !registryRunning && !buildingRunning && !analyze.isPending
  const buildingCompleted = building.data?.executionStatus === 'COMPLETED'
  const buildingMatched = buildingCompleted && building.data?.resolutionStatus === 'MATCHED'
  const buildingInputMissing = ['INPUT_INCOMPLETE', 'INPUT_CONFLICT'].includes(building.data?.executionStatus || '')
  const registerReview = buildingRegister.data?.latest
  const missingRegister = missingPrimaryRegister(registerReview, item.buildingType)
  const buildingDescription = registerReview?.status === 'PROCESSING' ? '첨부한 건축물대장 파일을 분석하고 기존 자료와 비교하고 있습니다.'
    : registerReview?.status === 'FAILED' ? '대장 파일 분석에 실패했습니다. 아래에서 사유를 확인하고 다시 시도해 주세요.'
    : missingRegister ? '필수 대장 문서가 확인되지 않았습니다. 아래 파일 분석 영역에서 보완해 주세요.'
    : registerReview?.status === 'PARSED' ? '첨부 대장 분석을 완료했습니다. 자료 대조와 위반 관련 기재를 아래에서 확인해 주세요.'
    : registerReview?.status === 'UPLOADED' ? '대장 파일을 등록했습니다. 전송 동의 후 아래에서 분석을 시작해 주세요.'
    : buildingRunning ? '건축물대장을 조회하고 건축물·호실을 비교하고 있습니다.'
    : building.error || building.data?.executionStatus === 'CALL_FAILED' ? '건축물대장 조회에 실패했습니다. 아래에서 사유를 확인하고 다시 시도해 주세요.'
    : buildingMatched ? '공공 API의 건축물·호실 식별을 완료했습니다. 대장 파일을 등록하면 관련 기재를 추가로 확인할 수 있습니다.'
    : buildingCompleted ? '조회는 완료했으나 일치하는 호실을 확정하지 못했습니다. 아래 상세 결과를 확인해 주세요.'
    : buildingInputMissing ? '조회에 필요한 주소·동·층·호 정보를 보완해야 합니다. 아래 상세 사유를 확인해 주세요.'
    : building.data?.executionStatus === 'DISABLED' ? '공공 API 조회는 아직 실행하지 않았습니다. 아래 조회 버튼으로 건축물·호실을 확인할 수 있습니다.' : '매물주소와 상세주소로 공공 API를 조회할 수 있습니다. 문서 분석과 별도로 실행합니다.'
  return <>
    {document?.status === 'FAILED' && <RegistryAnalysisFailureDialog result={document} open={failureDialogOpen} onDismiss={() => { sessionStorage.setItem(failureKey, 'acknowledged'); setDismissedFailure(failureKey) }} />}
    <PageHeading eyebrow={`LISTING #${item.id}`} title={item.buildingName || item.address} description={`${item.address}${item.detailAddress ? ` · ${item.detailAddress}` : ''}`} back={<Link to={listUrl} className="mb-5 flex items-center gap-1 text-xs text-slate-500 hover:text-brand-700"><ArrowLeft size={14} /> 내 매물</Link>} action={<div className="flex flex-wrap items-center gap-3"><button onClick={() => void refresh()} disabled={refreshing} title="서버의 진행 상태와 저장된 결과를 다시 조회합니다. 새 분석을 실행하지 않습니다." className="flex items-center gap-1 text-xs font-bold text-brand-700 disabled:opacity-50"><RefreshCw size={14} className={refreshing ? 'animate-spin' : undefined} /> {refreshing ? '상태 확인 중…' : '상태 새로고침'}</button><span className="rounded-lg bg-brand-50 px-3 py-2 text-xs font-bold text-brand-700">{listingStatusLabel[item.status]}</span></div>} />

    <div className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_310px]"><div className="space-y-5"><section className={panel} role="region" aria-label="안전성 분석 점검 현황"><div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-extrabold">안전성 분석 점검 현황</h2></div><p className="mt-4 rounded-lg bg-slate-50 p-3 text-xs leading-6 text-slate-600">등기사항증명서 분석 후 선택 항목 등록 여부와 관계없이 분석할 수 있습니다. 위험 분석에는 등기사항증명서·거래조건과 분석된 건축물대장의 위반 표시를 반영합니다. 준비 중인 항목은 점검 완료로 처리하지 않습니다.</p><ol aria-label="안전성 점검 항목" className="my-7 space-y-0"><Step number={1} done={document?.status === 'PARSED' && !registryNeedsReview} attention={registryNeedsReview} statusLabel={registryNeedsReview ? '재분석 필요' : undefined} current={registryRunning} failed={registryFailed} title="등기사항증명서 분석" description={registryNeedsReview ? '매물 주소가 변경되어 기존 등기사항증명서와 다시 대조해야 합니다. 아래 동의 후 재분석 기능을 이용해 주세요.' : registryRunning ? '서버가 등기사항증명서를 분석하고 있습니다.' : processing.isError && errorStatus(processing.error) !== 404 && !document ? '등기사항증명서 분석 상태를 불러오지 못했습니다. 상태를 새로고침해 주세요.' : document?.status === 'FAILED' ? registryFailureMessage(document.errorCode).title : document?.status === 'PROCESSING' ? '서버가 등기사항증명서를 분석하고 있습니다.' : document?.status === 'PARSED' ? '등기사항증명서 분석이 완료되었습니다.' : '등기사항증명서 파일 등록 후 분석할 수 있습니다.'} /><Step number={2} optional title="건축물대장 분석" done={registerReview?.status === 'PARSED' && !registerReview.stale && !missingRegister} current={registerReview?.status === 'PROCESSING' || (!registerReview && buildingRunning)} failed={registerReview?.status === 'FAILED' || (!registerReview && (Boolean(building.error) || building.data?.executionStatus === 'CALL_FAILED'))} attention={Boolean(registerReview?.stale) || missingRegister || (!registerReview && (buildingCompleted || buildingInputMissing))} statusLabel={registerReview?.status === 'PARSED' && !registerReview.stale ? missingRegister ? '필수 문서 보완 필요' : '분석 완료' : registerReview?.stale ? '이전 문서 이력' : registerReview?.status === 'UPLOADED' ? '파일 분석 대기' : !registerReview && buildingMatched ? '호실 식별 완료' : !registerReview && buildingInputMissing ? '정보 보완 필요' : undefined} description={buildingDescription} />
        <Step number={3} optional prepared title="전입세대열람원 확인" description="전입세대 자료와 선순위 임차 관련 확인을 제공할 예정입니다." />
        <Step number={4} optional prepared title="전세가율 및 시세 분석" description="매매·전월세 실거래가를 비교하는 시세 분석을 준비 중입니다." />
        <Step number={5} optional prepared title="계약정보 등록 및 표준계약서 생성" description="계약정보 입력, 소유자·임대인 대조와 계약서 초안 생성을 제공할 예정입니다." />
        <Step number={6} optional prepared title="소유자·임대인 신원 및 인감증명서 확인" description="당사자의 실물 확인 여부와 확인일을 기록하는 기능을 준비 중입니다." />
        <Step number={7} optional prepared title="관리비 및 세금·체납 정보 확인" description="납세증명서 첨부와 관리비·공과금 직접 확인 기록을 준비 중입니다." />
        <Step number={8} attention={safety?.reanalysisAvailable} statusLabel={safety?.reanalysisAvailable && !analyze.isPending && !analysisFailed ? '재분석 필요' : undefined} done={Boolean(safety) && !analyze.isPending} current={analyze.isPending} failed={analysisFailed} title="안전성 분석" description={analyze.isPending ? '안전성 분석을 진행하고 있습니다.' : analyze.isError ? '안전성 분석에 실패했습니다. 다시 시도해 주세요.' : analysisFailed ? '분석 결과를 불러오지 못했습니다. 상태를 새로고침해 주세요.' : safety?.reanalysisAvailable ? '자료 또는 평가 기준이 바뀌었습니다. 아래 결과는 이전 분석이며 재분석이 필요합니다.' : safety ? '안전성 분석이 완료되었습니다. 아래 결과를 확인해 주세요.' : result.isPending ? '저장된 분석 결과를 확인하고 있습니다.' : item.status === 'REGISTRY_READY' ? '등기사항증명서 분석이 완료되었습니다. 아래 안전성 분석 영역에서 분석을 시작할 수 있습니다.' : '등기사항증명서 분석 후 시작할 수 있습니다.'} /></ol>
      </section>
      <section className={`${panel} scroll-mt-24`} role="region" aria-label="등기사항증명서 분석" id="registry-document-files">
        <div className="flex items-center justify-between gap-3"><h2 className="text-xl font-extrabold">등기사항증명서 분석</h2><span className="rounded-md bg-brand-50 px-2.5 py-1 text-xs font-bold text-brand-700">필수</span></div>
        <p className="mt-3 text-xs leading-6 text-slate-500">현재 매물의 등기사항증명서를 등록하고 분석합니다. 갑구의 소유관계와 을구의 권리사항을 확인합니다.</p>
        <RegistryDocumentHistory listingId={id} selected={selectedRegistryDocument} onSelect={setSelectedRegistryDocument} />
        {document?.status === 'FAILED' && !registryRunning && !selectedRegistryDocument && <RegistryAnalysisFailure result={document} />}
        <div className="mt-5" aria-label="등기사항증명서 분석 결과">
          {item.propertyId && !selectedRegistryDocument && (registryRunning || document?.status === 'FAILED') && <p className="mb-3 rounded-lg bg-amber-50 p-3 text-xs text-amber-900">이전 문서의 분석 결과입니다. 새 문서 분석이 완료되면 갱신됩니다.</p>}
          {selectedRegistryDocument ? <><p className="mb-3 text-xs text-slate-500">선택한 문서의 분석 결과입니다.</p>{selectedRegistryDocument.status === 'PARSED' ? <RegistryFactsCard listingId={id} documentId={selectedRegistryDocument.id} embedded /> : <p className="rounded-xl bg-slate-50 p-5 text-sm text-slate-500">{documentStatusLabel[selectedRegistryDocument.status]}{selectedRegistryDocument.errorCode && ` · ${registryFailureMessage(selectedRegistryDocument.errorCode).title}`}</p>}</> : (document?.status === 'PARSED' || Boolean(item.propertyId)) ? <RegistryFactsCard listingId={id} embedded /> : <p className="rounded-xl bg-slate-50 px-5 py-9 text-center text-sm text-slate-400">분석내용 없음</p>}
        </div>
        {registrySubmission.isPending ? <div role="status" className="mt-4 rounded-xl bg-brand-50 p-4 text-sm text-brand-800">파일을 등록하고 분석을 준비하고 있습니다…</div> : registryRunning && <RegistryProgress />}
        <div className="mt-6 border-t border-slate-100 pt-5">
          <label className="flex cursor-pointer flex-col items-center gap-2 rounded-xl border-2 border-dashed border-brand-500/30 bg-brand-50/50 p-6 text-center text-brand-700"><FileUp size={26} /><strong className="text-sm">{selectedRegistryFile?.name || '등기사항증명서 PDF 선택'}</strong><span className="text-xs text-slate-500">암호가 없는 PDF · 최대 10MB · 50페이지</span><input ref={registryFileInput} type="file" accept="application/pdf,.pdf" aria-label="등기사항증명서 PDF 파일" className="sr-only" disabled={registryRunning || buildingRunning} onChange={async e => {
            const file = e.target.files?.[0]; const check = ++registryFileCheck.current
            setSelectedRegistryFile(null); setRegistryFileError(undefined); setSelectedRegistryDocument(null); registrySubmission.reset(); registryRequestId.current = null
            setCheckingRegistryFile(Boolean(file)); if (!file) return
            const error = await registryPdfError(file)
            if (check !== registryFileCheck.current) return
            setCheckingRegistryFile(false); setRegistryFileError(error); if (!error) setSelectedRegistryFile(file)
          }} /></label>
          {checkingRegistryFile && <p role="status" className="mt-3 text-xs text-slate-500">PDF 파일을 확인하고 있습니다.</p>}
          <ErrorNotice error={registryFileError ? new Error(registryFileError) : null} />
          <Button className="mt-4" disabled={!selectedRegistryFile || checkingRegistryFile || registryRunning || buildingRunning || analyze.isPending} busy={registrySubmission.isPending} onClick={() => {
            if (window.confirm('선택한 등기사항증명서 PDF에 포함된 소유자 정보 등을 OpenAI로 전송하여 분석합니다. API 사용 비용이 발생합니다. 분석을 시작하시겠습니까?')) registrySubmission.mutate()
          }}>등기사항증명서 분석</Button>
          <p className="mt-3 text-xs leading-6 text-slate-500">분석 버튼을 누르면 파일 등록과 AI 분석을 순서대로 진행합니다. 이전 파일과 분석 이력은 보존됩니다. 파일 선택만으로는 전송하지 않습니다.</p>
          <ErrorNotice error={registrySubmission.error} />
          <ErrorNotice error={processing.error && errorStatus(processing.error) !== 404 ? processing.error : null} />
        </div>
      </section>
      <BuildingRegisterCard key={id} listingId={id} buildingType={item.buildingType} blocked={registryRunning || analyze.isPending} data={buildingRegister.data} error={buildingRegister.error} />
      {building.data && <BuildingIdentificationCard listing={item} documentAnalyzed={registerReview?.status === 'PARSED' && !registerReview.stale && !missingRegister && Boolean(registerReview.result)} result={building.data} onRetry={input => retryBuilding.mutate(input)} retrying={retryBuilding.isPending} retryError={retryBuilding.error} />}
      {building.error && <ErrorNotice error={building.error} />}
      <section className={`${panel} scroll-mt-24`} role="region" aria-label="안전성 분석 실행"><h2 className="text-xl font-extrabold">안전성 분석</h2><p className="mt-3 text-xs leading-6 text-slate-500">안전성 분석은 문서 이력 선택과 관계없이 실행 시점의 최신 등기자료·현재 거래조건과 가장 최근 분석에 성공한 건축물대장의 위반 표시를 기준으로 합니다. 선택 점검 항목은 등록되지 않아도 분석할 수 있습니다.</p>
        {canStartSafety && <Button className="mt-4" busy={analyze.isPending} onClick={() => analyze.mutate()}>{analyze.isPending ? '안전성 분석 중…' : safety?.reanalysisAvailable ? '새 기준으로 재분석' : safety ? '안전성 분석' : '안전성 분석 시작'}</Button>}
        {!canStartSafety && <Button className="mt-4" disabled busy={analyze.isPending}>안전성 분석</Button>}
        {!canStartSafety && !safety && !analyze.isPending && <p className="mt-4 rounded-lg bg-slate-50 p-3 text-xs text-slate-600">등기사항증명서 분석 완료 후 분석할 수 있습니다.</p>}
        {safety?.reanalysisAvailable && <p className="mt-4 rounded-lg bg-amber-50 px-4 py-3 text-xs text-amber-800">{reanalysisLabel[safety.reanalysisReason]} 아래 결과는 이전 분석입니다.</p>}
        <ErrorNotice error={analyze.error || (result.error && errorStatus(result.error) !== 404 ? result.error : null)} />
      </section>
      {deferredOwnershipResult && <p className="rounded-xl bg-amber-50 p-4 text-sm text-amber-900">이전 평가 기준의 결과입니다. 안전성 분석을 다시 실행해 주세요. 기존 분석 이력은 보존됩니다.</p>}
      {safety && !deferredOwnershipResult && <div role="region" aria-label="안전성 분석 결과" className="scroll-mt-24 focus:outline-none"><SafetyResultCard result={safety} /></div>}</div><aside><section className={`${panel} sticky top-24`} role="region" aria-label="매물 정보"><span className="text-[11px] font-extrabold tracking-[.2em] text-brand-500">PROPERTY INFO</span><h2 className="mt-3 text-xl font-extrabold">매물 정보</h2><dl className="mt-5 divide-y divide-slate-100 text-xs">{[["거래 유형", transactionLabel[item.transactionType]], [item.transactionType === 'SALE' ? '매매가' : '보증금', won(item.transactionType === 'SALE' ? item.salePrice : item.depositAmount)], ...(item.transactionType === 'MONTHLY_RENT' ? [['월세', won(item.monthlyRent)]] : []), ['매물주소', `${item.address} ${item.detailAddress || ''}`.trim()], ['건물 유형', formatBuildingType(item.buildingType)], ['등기사항증명서 상태', document ? documentStatusLabel[document.status] : processing.isPending ? '확인 중' : '미등록'], ['등록일', formatDate(item.createdAt)]].map(([term, value]) => <div key={term} className="flex justify-between gap-4 py-3"><dt className="shrink-0 text-slate-400">{term}</dt><dd className="m-0 break-words text-right font-bold text-slate-700">{value}</dd></div>)}</dl><div aria-label="매물 관리"><div className="mt-5 grid grid-cols-2 gap-2 border-t border-slate-100 pt-5">{canModify ? <Link to={`/listings/${id}/edit${location.search}`}><Button className="w-full px-2 text-xs" variant="secondary">매물 수정</Button></Link> : <Button className="w-full px-2 text-xs" variant="secondary" disabled>매물 수정</Button>}<Button className="w-full px-2 text-xs" variant="secondary" disabled={!canModify} busy={removal.isPending} onClick={() => { if (window.confirm('매물을 목록에서 삭제하시겠습니까? 기존 문서·분석 이력은 비공개로 보존됩니다.')) removal.mutate() }}>매물 삭제</Button></div><ErrorNotice error={removal.error} /></div></section></aside></div>
  </>
}

function Step({ number, optional, prepared, attention, statusLabel, done, current, failed, title, description }: {
  number: number; optional?: boolean; prepared?: boolean; attention?: boolean; statusLabel?: string;
  done?: boolean; current?: boolean; failed?: boolean; title: string; description: ReactNode;
}) {
  const state = current ? '진행 중' : failed ? '오류' : prepared ? '준비 중' : attention ? '확인 필요' : done ? '완료' : '대기'
  const label = statusLabel || state
  return <li className="flex gap-4 pb-6 last:pb-0"><span role="img" aria-label={`${title}: ${label}`} title={label} className={cx('grid size-8 shrink-0 place-items-center rounded-full',
    state === '완료' && 'bg-brand-100 text-brand-700', state === '진행 중' && 'bg-blue-100 text-blue-700',
    state === '오류' && 'bg-red-100 text-red-600', ['대기', '준비 중'].includes(state) && 'bg-slate-100 text-slate-500',
    state === '확인 필요' && 'bg-amber-100 text-amber-700')}>
    {state === '완료' ? <CircleCheck size={19} aria-hidden="true" /> : state === '오류' ? <CircleX size={19} aria-hidden="true" />
      : state === '진행 중' ? <LoaderCircle size={19} className="animate-spin motion-reduce:animate-none" aria-hidden="true" />
      : state === '준비 중' ? <CircleDashed size={19} aria-hidden="true" />
      : state === '확인 필요' ? <CircleAlert size={19} aria-hidden="true" /> : <Clock3 size={19} aria-hidden="true" />}
  </span><div className="min-w-0"><div className="flex flex-wrap items-center gap-2"><span className="text-xs font-bold text-slate-400" aria-hidden="true">{String(number).padStart(2, '0')}</span><h3 className="text-sm font-bold">{title}</h3>{optional && <span className="text-[10px] text-slate-500">선택</span>}<span className={cx('rounded px-2 py-0.5 text-[10px] font-semibold', state === '오류' ? 'bg-red-50 text-red-700' : state === '확인 필요' ? 'bg-amber-50 text-amber-800' : 'bg-slate-50 text-slate-600')}>{label}</span></div><p className="mt-1 text-xs leading-5 text-slate-500">{description}</p></div></li>
}

function SafetyResultCard({ result }: { result: SafetyResult }) {
  const score = Math.max(0, Math.min(100, Number(result.analysis.riskScore)))
  const tone = result.analysis.riskLevel === 'LOW' ? '#27876c' : result.analysis.riskLevel === 'MEDIUM' ? '#d39b35' : '#d66552'
  return <section className={panel}><div className="flex flex-col justify-between gap-4 sm:flex-row sm:items-start"><div><span className="text-[11px] font-extrabold tracking-[.2em] text-brand-500">SAFETY RESULT</span><h2 className="mt-2 text-xl font-extrabold">안전성 분석 결과</h2></div><span className="self-start rounded-lg px-3 py-2 text-xs font-extrabold" style={{ color: tone, backgroundColor: `${tone}16` }}>위험도 {riskLabel[result.analysis.riskLevel]}</span></div>
    <div className="mt-6 grid items-center gap-5 rounded-xl bg-slate-50 p-5 sm:grid-cols-[150px_1fr]"><div className="relative h-32"><ResponsiveContainer width="100%" height="100%"><PieChart><Pie data={[{ value: score }, { value: 100 - score }]} dataKey="value" startAngle={210} endAngle={-30} innerRadius={45} outerRadius={58} stroke="none"><Cell fill={tone} /><Cell fill="#e7ece8" /></Pie></PieChart></ResponsiveContainer><div className="absolute inset-0 grid place-items-center pt-2 text-center"><div><strong className="block text-3xl">{score}</strong><span className="text-[10px] text-slate-500">위험 점수</span></div></div></div><div><p className="font-bold leading-7">{formatRiskText(result.summary)}</p><p className="mt-2 text-xs text-slate-500">분석일 {formatDate(result.analysis.analyzedAt)}</p></div></div>
    <div className="mt-5 flex flex-wrap gap-2 text-[11px] text-slate-600"><span className="rounded-md bg-slate-100 px-2.5 py-2">위험 요소 {result.factorCount}건</span><span className="rounded-md bg-slate-100 px-2.5 py-2">최고 심각도 {result.highestSeverity ? riskLabel[result.highestSeverity] : '해당 없음'}</span><span className="rounded-md bg-slate-100 px-2.5 py-2">정책 버전 {result.analysis.policyVersion}</span></div>
    <h3 className="mt-7 border-t border-slate-100 pt-6 font-extrabold">확인된 위험 요소</h3>{result.analysis.factors.length ? <div className="mt-3 space-y-3">{result.analysis.factors.map((factor) => <RiskFactorCard key={factor.id} factor={factor} />)}</div> : <p className="mt-3 text-sm text-slate-500">현재 확인된 주요 위험 요소가 없습니다.</p>}
    <p className="mt-5 rounded-lg bg-slate-50 p-3 text-xs leading-6 text-slate-600">이번 위험 점수는 등기사항증명서·거래조건과 분석된 건축물대장의 위반 표시를 반영합니다. 위반 표시 미확인만으로 적법성을 확정하지 않으며, 전입세대·시세·계약·신원·체납 등 준비 중인 점검은 반영되지 않습니다.</p><p className="mt-7 text-[11px] leading-6 text-slate-400">이 결과는 등기사항증명서와 등록된 정보를 기준으로 한 참고 자료입니다. 계약 전 최신 서류와 전문가 검토를 함께 확인하세요.</p>
  </section>
}
