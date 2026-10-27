import { useEffect, useRef, useState, type ChangeEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ExternalLink, FileUp } from 'lucide-react'
import { Button, ErrorNotice, input, panel } from '../../components/ui'
import { formatDate } from '../../lib/format'
import type { BuildingRegisterEnvelope, BuildingRegisterReview } from '../../types/api'
import { listingsApi } from './api'
import { registryPdfError } from './registryPdf'
import { missingPrimaryRegister, primaryEntries, requiredRegisterLabel, requiredRegisterType, type RequiredRegister } from './buildingRegisterPolicy'

const statuses: Record<string, string> = { UPLOADED: '파일 등록 완료 · 분석 대기', PROCESSING: 'AI 분석 중', PARSED: '파일 분석 완료', FAILED: '파일 분석 실패', SUPERSEDED: '이전 자료 기준' }
const types: Record<string, string> = { GENERAL: '일반건축물대장', TITLE: '표제부', EXCLUSIVE: '전유부', SUMMARY: '총괄표제부', UNKNOWN: '대장 종류 확인 필요' }
const checks: Record<string, string> = { MATCHED: '일치', REVIEW_REQUIRED: '대조 확인 필요', DIFFERENT: '불일치 · 추가 확인 필요', NOT_AVAILABLE: '비교 정보 부족', NOT_APPLICABLE: '해당 대장 비교 대상 아님' }
const evidenceKinds: Record<string, string> = { VIOLATION: '위반 관련 기재', USE_CHANGE: '용도 변경 관련 기재', AREA_CHANGE: '면적 변경 관련 기재', OTHER: '추가 기재' }
const errors: Record<string, string> = {
  PARSER_NOT_CONFIGURED: 'AI 분석 설정을 확인해 주세요.', PARSER_AUTH_FAILED: 'AI 서비스 인증 설정을 확인해 주세요.',
  PARSER_RATE_LIMITED: 'AI 서비스 호출 한도를 초과했습니다. 잠시 후 다시 시도해 주세요.',
  NOT_BUILDING_REGISTER: '건축물대장으로 확인되지 않는 문서입니다. 발급한 대장 PDF를 다시 등록해 주세요.',
  PARSER_INVALID_OUTPUT: '문서 내용을 확실하게 분석하지 못했습니다. 원본이 선명한지 확인해 주세요.',
  PARSER_INCOMPLETE: '일부 파일 또는 내용의 분석이 누락됐습니다. 완료된 결과가 아닙니다. 전체 문서와 파일 구성을 확인해 주세요.',
  PARSER_SOURCE_MISMATCH: '분석 내용과 원본 파일의 문서 종류가 맞지 않습니다. 근거를 확정할 수 없어 분석을 완료하지 않았습니다.',
  PARSER_INVALID_SOURCE: '원본 PDF를 확인할 수 없습니다. 파일을 다시 확인해 주세요.',
  FILE_INTEGRITY_FAILED: '저장된 파일을 확인할 수 없습니다. 원본 PDF를 다시 등록해 주세요.',
  SOURCE_CHANGED: '비교할 자료가 변경되었습니다. 최신 자료로 새 묶음을 등록해 주세요.',
  INTERRUPTED: '서버 재시작으로 분석이 중단됐습니다. 다시 시작하려면 전송 동의 후 버튼을 눌러 주세요.',
  EXCLUSIVE_REGISTER_REQUIRED: '필수 전유부가 확인되지 않았습니다. 해당 호실 전유부의 전체 페이지를 포함해 파일을 다시 등록해 주세요.',
  GENERAL_REGISTER_REQUIRED: '필수 일반건축물대장이 확인되지 않았습니다. 해당 건물의 일반건축물대장을 포함해 다시 등록해 주세요.',
  PRIMARY_REGISTER_REQUIRED: '전유부 또는 일반건축물대장이 필요합니다. 표제부·총괄표제부만으로는 분석할 수 없습니다.',
  QUEUE_BUSY: '분석 요청이 많습니다. 잠시 후 다시 시도해 주세요.',
}

export function BuildingRegisterCard({ listingId, buildingType, data, error, blocked = false }: {
  listingId: number; buildingType: string; data?: BuildingRegisterEnvelope; error?: unknown; blocked?: boolean;
}) {
  const client = useQueryClient()
  const required = requiredRegisterType(buildingType), requiredLabel = requiredRegisterLabel[required]
  const [requiredFiles, setRequiredFiles] = useState<File[]>([])
  const [optionalFiles, setOptionalFiles] = useState<File[]>([])
  const files = [...requiredFiles, ...optionalFiles]
  const [fileError, setFileError] = useState<string>()
  const [checking, setChecking] = useState(false)
  const checkId = useRef(0), requestId = useRef<string | null>(null), activeReview = useRef<number | null>(null)
  const fileInput = useRef<HTMLInputElement>(null), optionalInput = useRef<HTMLInputElement>(null)
  const [selectedId, setSelectedId] = useState<number>()
  const latest = data?.latest
  const historic = useQuery({ queryKey: ['building-register-history', listingId, selectedId], queryFn: () => listingsApi.buildingRegisterReview(listingId, selectedId!), enabled: Boolean(selectedId) })
  const review = selectedId ? historic.data : latest
  const previousId = !selectedId && !latest?.result ? data?.history.find(row => row.status === 'PARSED')?.id : undefined
  const previous = useQuery({ queryKey: ['building-register-history', listingId, previousId], queryFn: () => listingsApi.buildingRegisterReview(listingId, previousId!), enabled: Boolean(previousId) })
  const displayedResult = review?.result ? review : !selectedId ? previous.data : undefined
  const submission = useMutation({ mutationFn: async () => {
    if (!requiredFiles.length) throw new Error(`${requiredLabel} PDF를 선택해 주세요.`)
    if (files.length > 5 || files.reduce((sum, file) => sum + file.size, 0) > 10 * 1024 * 1024) throw new Error('PDF 최대 5개, 합계 10MB까지 선택해 주세요.')
    for (const file of files) { const message = await registryPdfError(file); if (message) throw new Error(message.replaceAll('등기사항증명서', '건축물대장')) }
    requestId.current ||= crypto.randomUUID()
    return listingsApi.uploadAndAnalyzeBuildingRegister(listingId, files, requestId.current)
  }, onSuccess: async value => {
    activeReview.current = value.id
    setSelectedId(undefined)
    client.setQueryData<BuildingRegisterEnvelope>(['building-register', listingId], old => ({ latest: value, history: [{ id: value.id, status: value.status, stale: value.stale, createdAt: value.createdAt }, ...(old?.history || []).filter(row => row.id !== value.id)] }))
    await client.invalidateQueries({ queryKey: ['building-register', listingId] })
  } })
  useEffect(() => client.getQueryCache().subscribe(event => {
    if (event.query.queryKey[0] !== 'building-register' || event.query.queryKey[1] !== listingId) return
    const completed = (event.query.state.data as BuildingRegisterEnvelope | undefined)?.latest
    if (activeReview.current == null || completed?.id !== activeReview.current || !['PARSED', 'FAILED', 'SUPERSEDED'].includes(completed?.status || '')) return
    activeReview.current = null; requestId.current = null; checkId.current++
    setRequiredFiles([]); setOptionalFiles([]); setFileError(undefined); setChecking(false)
    if (fileInput.current) fileInput.current.value = ''
    if (optionalInput.current) optionalInput.current.value = ''
  }), [client, listingId])
  const download = useMutation({ mutationFn: async ({ id, index }: { id: number; index: number }) => {
    const blob = await listingsApi.buildingRegisterFile(listingId, id, index)
    const url = URL.createObjectURL(blob), link = document.createElement('a')
    link.href = url; link.download = `building-register-${id}-${index}.pdf`; link.click()
    setTimeout(() => URL.revokeObjectURL(url), 1000)
  } })
  const choose = async (event: ChangeEvent<HTMLInputElement>, optional: boolean) => {
    const element = event.currentTarget, selected = Array.from(element.files || [])
    const current = ++checkId.current; setChecking(true); setFileError(undefined); submission.reset(); requestId.current = null; setSelectedId(undefined)
    if (optional) setOptionalFiles([]); else setRequiredFiles([])
    const bundle = [...selected, ...(optional ? requiredFiles : optionalFiles)]
    let message: string | undefined
    if (bundle.length > 5 || bundle.reduce((sum, file) => sum + file.size, 0) > 10 * 1024 * 1024) message = '필수·선택 문서를 합쳐 PDF 최대 5개, 합계 10MB까지 선택해 주세요.'
    for (const file of selected) { if (message) break; message = (await registryPdfError(file))?.replaceAll('등기사항증명서', '건축물대장') }
    if (current !== checkId.current) return
    setChecking(false); setFileError(message)
    if (message) element.value = ''
    else if (optional) setOptionalFiles(selected)
    else setRequiredFiles(selected)
  }
  const missingPrimary = missingPrimaryRegister(review, buildingType)
  const busy = submission.isPending || latest?.status === 'PROCESSING'
  return <section id="building-register-files" className={`${panel} scroll-mt-24`} role="region" aria-label="건축물대장 분석">
    <div className="flex flex-wrap items-center justify-between gap-3"><h2 className="text-xl font-extrabold">건축물대장 분석</h2><a href="https://www.eais.go.kr/moct/bci/aaa01/BCIAAA01V01" target="_blank" rel="noopener noreferrer" className="inline-flex items-center gap-1 text-xs font-bold text-brand-700">세움터 발급 안내 <ExternalLink size={14} /></a></div>
    <p className="mt-3 text-xs leading-6 text-slate-600">건축물대장 분석을 신청할 때는 {requiredLabel}가 필요합니다. 표제부·총괄표제부는 선택 보완 자료입니다. 앞면·뒷면·추가 장을 포함한 전체 PDF를 등록해 주세요.</p>
    <p className="mt-2 text-xs leading-6 text-slate-500">건축물대장은 선택 점검 항목입니다. 첨부하지 않아도 등기사항증명서 기반 안전성 분석을 실행할 수 있습니다.</p>
    {!!data?.history.length && <label className="mt-5 block text-xs font-bold text-slate-700">문서 이력<select className={input} value={selectedId || ''} onChange={e => setSelectedId(e.target.value ? Number(e.target.value) : undefined)}><option value="">최신 자료</option>{data.history.filter(row => row.id !== latest?.id).map(row => <option key={row.id} value={row.id}>이력 #{row.id} · {statuses[row.status]} · {formatDate(row.createdAt)}</option>)}</select></label>}
    {review && <section aria-label="건축물대장 문서 상태" className="mt-3 rounded-xl border border-slate-200 bg-slate-50 p-4 text-xs">
      <p className="font-extrabold text-brand-800">{missingPrimary ? '필수 문서 보완 필요' : statuses[review.status]}</p>
      <ul className="mt-2 space-y-2">{review.files.map(file => <li key={file.index} className="flex flex-wrap items-center justify-between gap-2 text-xs"><span className="min-w-0 break-all text-slate-600">{file.name} · {file.pages}페이지</span><button className="shrink-0 font-bold text-brand-700 disabled:opacity-50" disabled={download.isPending} onClick={() => download.mutate({ id: review.id, index: file.index })}>PDF 다운로드</button></li>)}</ul>
      <p className="mt-2 text-slate-500">등록 {formatDate(review.createdAt)}{review.processedAt && ` · 처리 ${formatDate(review.processedAt)}`}</p>
      {review.status === 'FAILED' && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700">{errors[review.errorCode || ''] || '분석을 완료하지 못했습니다. 원본 PDF를 다시 선택해 분석해 주세요.'}</p>}
    </section>}
    <div className="mt-5" aria-label="건축물대장 분석 결과 영역">
      {displayedResult?.result ? <>{displayedResult.id !== latest?.id && !selectedId && <p className="rounded-lg bg-amber-50 p-3 text-xs text-amber-900">이전 성공 이력 #{displayedResult.id}의 결과입니다. 새 분석이 완료되면 갱신됩니다.</p>}<BuildingRegisterResult review={displayedResult} required={displayedResult.requiredDocumentType || required} /></> : <p className="rounded-xl bg-slate-50 px-5 py-9 text-center text-sm text-slate-400">{previous.isFetching ? '이전 분석 결과 확인 중…' : '분석내용 없음'}</p>}
    </div>
    {busy && <div role="status" aria-live="polite" className="mt-4 rounded-xl bg-brand-50 p-4"><p className="text-sm font-bold text-brand-800">{submission.isPending ? '파일을 등록하고 분석을 준비하고 있습니다…' : '건축물대장을 분석하고 비교하고 있습니다.'}</p><div role="progressbar" aria-label="건축물대장 분석 진행 중" className="mt-3 h-2.5 overflow-hidden rounded-full bg-brand-100"><div className="registry-progress-bar h-full w-2/5 rounded-full bg-brand-600" /></div><p className="mt-3 text-xs text-slate-600">완료되면 자동으로 결과를 표시합니다.</p></div>}
    <div className="mt-6 border-t border-slate-100 pt-5">
      <p className="text-xs text-slate-500">PDF 최대 5개 · 합계 10MB·50페이지 · 암호 없는 파일. 이전 파일과 분석 이력은 보존됩니다.</p>
      <label className="mt-4 block rounded-xl border-2 border-dashed border-brand-500/30 bg-brand-50/50 p-4 text-sm font-semibold text-brand-700"><span className="flex items-center gap-2"><FileUp size={19} />{requiredLabel} PDF <span className="rounded bg-brand-100 px-2 py-0.5 text-[11px]">필수</span></span><input ref={fileInput} className="mt-3 block w-full text-xs" aria-label={`${requiredLabel} PDF 파일 (필수)`} type="file" multiple accept="application/pdf,.pdf" onChange={e => choose(e, false)} disabled={busy || blocked || checking} /></label>
      {requiredFiles.length > 0 && <ul className="mt-2 space-y-1 text-xs text-slate-600">{requiredFiles.map((file, index) => <li key={index}>{file.name}</li>)}</ul>}
      <label className="mt-4 block rounded-xl border border-slate-200 bg-slate-50 p-4 text-sm font-semibold text-slate-700"><span className="flex items-center gap-2"><FileUp size={19} />표제부·총괄표제부 PDF <span className="rounded bg-white px-2 py-0.5 text-[11px]">선택</span></span><input ref={optionalInput} className="mt-3 block w-full text-xs" aria-label="표제부·총괄표제부 PDF 파일 (선택)" type="file" multiple accept="application/pdf,.pdf" onChange={e => choose(e, true)} disabled={busy || blocked || checking} /></label>
      {optionalFiles.length > 0 && <ul className="mt-2 space-y-1 text-xs text-slate-600">{optionalFiles.map((file, index) => <li key={index}>{file.name}</li>)}</ul>}
      {(optionalFiles.length > 0 || fileError) && <button className="mt-2 text-xs font-bold text-brand-700" disabled={busy || blocked || checking} onClick={() => { setOptionalFiles([]); setFileError(undefined); if (optionalInput.current) optionalInput.current.value = '' }}>선택 보완 문서 해제</button>}
      <p className="mt-3 text-xs leading-6 text-slate-500">문서 종류는 분석 후 확인합니다. 보완 문서만으로는 분석을 완료할 수 없습니다. 여러 문서를 하나의 PDF로 발급받았다면 필수 문서 칸에 등록해 주세요.</p>
      {fileError && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-700">{fileError}</p>}
      <Button className="mt-4" disabled={!requiredFiles.length || checking || Boolean(fileError) || busy || blocked} busy={submission.isPending} onClick={() => { if (window.confirm('선택한 건축물대장 PDF에 포함된 소유자 정보 등을 OpenAI로 전송하여 분석합니다. API 사용 비용이 발생합니다. 분석을 시작하시겠습니까?')) submission.mutate() }}>건축물대장 분석</Button>
      <p className="mt-3 text-xs leading-6 text-slate-500">분석 버튼을 누르면 파일 등록과 AI 분석을 순서대로 진행합니다. 파일 선택만으로는 전송하지 않습니다.</p>
      <ErrorNotice error={error || submission.error || historic.error || previous.error || download.error} />
    </div>
    <p className="mt-5 text-[11px] leading-6 text-slate-500">첨부한 문서 범위에서 분석합니다. 위반 표시 미확인은 위반 없음의 보증이 아니며, 현장 불법증축·미등기를 문서만으로 확정하지 않습니다. 위반 표시는 사용자가 안전성 분석을 실행할 때 반영됩니다.</p>
  </section>
}

function BuildingRegisterResult({ review, required }: { review: BuildingRegisterReview; required: RequiredRegister }) {
  const result = review.result!
  const primary = primaryEntries(result.entries, required)
  const primaryMarked = primary.some(entry => entry.violationStatus === 'MARKED')
  const primaryLabel = required === 'EXCLUSIVE' ? '첨부 전유부' : required === 'GENERAL' ? '첨부 일반건축물대장' : '첨부 기본 문서'
  const compared = primary.length > 0 && !result.violationMentioned && result.entries.length > 0
    && result.entries.every(entry => entry.violationStatus === 'NO_MARK_VISIBLE')
  const unreadable = result.entries.filter(entry => entry.violationStatus === 'UNKNOWN')
    .map(entry => `파일 ${entry.fileIndex} · ${entry.page}페이지`).join(', ')
  const exclusiveOnly = result.entries.some(entry => entry.documentType === 'EXCLUSIVE')
    && !result.entries.some(entry => entry.documentType === 'TITLE')
  return <div className="mt-5 space-y-4" role="region" aria-label="건축물대장 분석 결과">
    <p className={`rounded-lg p-3 text-sm font-bold ${compared && !result.violationMentioned ? 'bg-brand-50 text-brand-800' : 'bg-amber-50 text-amber-900'}`}>{!primary.length ? `${requiredRegisterLabel[required]}가 확인되지 않았습니다. 필수 문서를 포함해 다시 등록해 주세요.` : primaryMarked ? `${primaryLabel}에 위반 표시가 있습니다. 대상과 기재 내용을 확인해 주세요.` : result.violationMentioned ? '선택 보완 문서에 위반 표시가 있습니다. 해당 호실과의 관련성을 확인해 주세요.' : compared ? '위험요소가 발견되지 않았습니다.' : `위반 표시를 인식하지 못했습니다: ${unreadable || '문서 기재 상태 미확정'}.`}</p>
    {exclusiveOnly && <p className="rounded-lg bg-slate-50 p-3 text-xs leading-6 text-slate-700">전유부 기준으로 확인했습니다. 선택 문서인 표제부는 미첨부 상태이며, 건물 전체 관련 사항은 이번 문서 확인 범위에 포함되지 않습니다.</p>}
    {result.entries.map((entry, index) => <article key={index} className="rounded-xl border border-slate-200 p-4">
      <h3 className="font-bold">{types[entry.documentType]} <span className="text-xs font-normal text-slate-500">· 파일 {entry.fileIndex} · {entry.page}페이지</span></h3>
      <dl className="mt-3 grid gap-2 text-xs sm:grid-cols-2">{[
        ['발급일', entry.issuedAt || '확인 불가'], ['주소', entry.roadAddress || entry.jibunAddress || '확인 불가'],
        ['동·층·호', [entry.dongName, entry.floorLabel, entry.unitName].filter(Boolean).join(' ') || '기재 확인 불가'],
        ['용도', entry.purpose || '기재 확인 불가'], ['면적', entry.areaSquareMeters == null ? '기재 확인 불가' : `${entry.areaSquareMeters.toLocaleString('ko-KR')}㎡`],
        ['위반 표시', entry.violationStatus === 'MARKED' ? '위반 관련 표시 있음' : entry.violationStatus === 'NO_MARK_VISIBLE' ? '제공된 페이지에서 표시 미확인' : '확인 불가'],
      ].map(([key, value]) => <div key={key}><dt className="text-slate-500">{key}</dt><dd className="mt-1 break-words font-semibold">{value}</dd></div>)}</dl>
      {result.checks.filter(check => check.entryIndex === index + 1).map(check => <ul key={check.entryIndex} className="mt-3 space-y-1 border-t border-slate-100 pt-3 text-xs text-slate-600">
        <li>등록 주소: {checks[check.listingAddress]}</li><li>등기사항증명서 주소: {checks[check.registryAddress]}</li><li>동: {checks[check.dong || 'NOT_AVAILABLE']}</li><li>호실: {checks[check.unit]}</li><li>API 식별 호실: {checks[check.apiUnit]}</li><li>등록 건물 유형과 용도: {checks[check.purpose]}</li>
      </ul>)}
      {entry.evidence.length > 0 && <ul className="mt-3 space-y-2 rounded-lg bg-slate-50 p-3 text-xs leading-6">{entry.evidence.map((note, n) => <li key={n}><strong>{evidenceKinds[note.kind]}</strong> · {note.page}페이지<br />{note.text}</li>)}</ul>}
    </article>)}
  </div>
}
