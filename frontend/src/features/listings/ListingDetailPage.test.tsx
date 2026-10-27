import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ListingDetailPage } from './ListingDetailPage'
import { listingsApi } from './api'
import type { BuildingIdentification, BuildingRegisterReview, Listing, RegistryProcessing, SafetyResult } from '../../types/api'

vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ user: { id: 7 } }) }))
vi.mock('./api', () => ({ listingsApi: { one: vi.fn(), processing: vi.fn(), safetyResult: vi.fn(), buildingIdentification: vi.fn(), retryBuildingIdentification: vi.fn(), buildingRegister: vi.fn(), buildingRegisterReview: vi.fn(), uploadBuildingRegister: vi.fn(), startBuildingRegister: vi.fn(), buildingRegisterFile: vi.fn(), analyze: vi.fn(), startProcessing: vi.fn(), remove: vi.fn(), registryFacts: vi.fn(), uploadAndAnalyzeRegistry: vi.fn(), registryHistory: vi.fn(), registryDocumentFile: vi.fn() } }))
vi.mock('recharts', () => ({ Cell: () => null, Pie: () => null, PieChart: () => null, ResponsiveContainer: () => null }))

const listing = { id: 8, registeredByUserId: 7, address: '서울 테스트로 1', buildingType: 'HOUSE', transactionType: 'JEONSE', depositAmount: 200000000, status: 'REGISTRY_READY' } as Listing
const safety: SafetyResult = { listingId: 8, listingStatus: 'SAFETY_ANALYZED', summary: '분석 완료', highestSeverity: null, factorCount: 0, latestRegistrySnapshotId: 3, reanalysisAvailable: false, reanalysisReason: 'UP_TO_DATE', analysis: { id: 2, propertyId: 2, registrySnapshotId: 3, analysisVersion: '1', policyVersion: '1', riskLevel: 'LOW', riskScore: 0, analyzedAt: '2026-10-15T16:00:00', factors: [] } }
const missing = Object.assign(new Error('결과 없음'), { status: 404 })

function open(path = '/listings/8') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, retryDelay: 0 } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><Routes><Route path="/listings/:id" element={<ListingDetailPage />} /></Routes></MemoryRouter></QueryClientProvider>)
}
beforeEach(() => {
  vi.mocked(listingsApi.registryHistory).mockResolvedValue([])
  sessionStorage.clear()
  Object.defineProperty(HTMLDialogElement.prototype, 'showModal', { configurable: true, value: function(this: HTMLDialogElement) { this.setAttribute('open', '') } })
  Object.defineProperty(HTMLDialogElement.prototype, 'close', { configurable: true, value: function(this: HTMLDialogElement) { this.removeAttribute('open') } })
  vi.mocked(listingsApi.registryFacts).mockResolvedValue({ documentId: 1, registrySnapshotId: 3, owners: [], rights: [], ownershipNeedsReview: true })
  vi.mocked(listingsApi.buildingRegister).mockResolvedValue({ history: [] })
  vi.mocked(listingsApi.one).mockResolvedValue(listing)
  vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, status: 'PARSED' } as RegistryProcessing)
  vi.mocked(listingsApi.safetyResult).mockRejectedValue(missing)
  vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({ executionStatus: 'NOT_REQUESTED', resolutionStatus: null } as BuildingIdentification)
})
afterEach(() => { cleanup(); delete window.kakao; vi.restoreAllMocks(); vi.resetAllMocks() })

describe('safety analysis state', () => {
  it('keeps edit/delete available after analysis and makes the building-file entry discoverable', async () => {
    vi.mocked(listingsApi.one).mockResolvedValue({ ...listing, status: 'SAFETY_ANALYZED' })
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    open()
    expect(await screen.findByRole('link', { name: '매물 수정' })).toBeTruthy()
    expect(screen.getByRole('button', { name: '매물 삭제' })).toBeEnabled()
    const summary = screen.getByRole('region', { name: '안전성 분석 점검 현황' })
    expect(within(summary).queryByRole('button')).toBeNull()
    expect(within(summary).queryByRole('link')).toBeNull()
    expect(within(screen.getByRole('region', { name: '매물 정보' })).getByRole('button', { name: '매물 삭제' })).toBeEnabled()
    expect(screen.getByRole('region', { name: '건축물대장 분석' })).toHaveAttribute('id', 'building-register-files')
    expect(await screen.findByRole('button', { name: '등기사항증명서 분석' })).toBeDisabled()
    expect(listingsApi.startProcessing).not.toHaveBeenCalled()
  })
  it('uploads and analyzes a valid attached PDF only after consent, keeping existing results', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(listingsApi.uploadAndAnalyzeRegistry).mockResolvedValue({ listingId: 8, documentId: 7, status: 'PROCESSING' } as RegistryProcessing)
    open()
    const button = await screen.findByRole('button', { name: '등기사항증명서 분석' })
    expect(button).toBeDisabled()
    const input = screen.getByLabelText('등기사항증명서 PDF 파일')
    fireEvent.change(input, { target: { files: [new File(['not a pdf'], 'bad.txt', { type: 'text/plain' })] } })
    await screen.findByText(/PDF 형식만 등록/)
    expect(button).toBeDisabled()
    fireEvent.change(input, { target: { files: [new File(['%PDF-1.7\nsynthetic'], 'synthetic.pdf', { type: 'application/pdf' })] } })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    await waitFor(() => expect(listingsApi.uploadAndAnalyzeRegistry).toHaveBeenCalledWith(8, expect.any(File), expect.any(String)))
    expect(listingsApi.startProcessing).not.toHaveBeenCalled()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it('does not upload the selected file when external analysis consent is declined', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    open()
    const button = await screen.findByRole('button', { name: '등기사항증명서 분석' })
    fireEvent.change(screen.getByLabelText('등기사항증명서 PDF 파일'), { target: { files: [new File(['%PDF-1.7\nfixture'], 'fixture.pdf', { type: 'application/pdf' })] } })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    expect(listingsApi.uploadAndAnalyzeRegistry).not.toHaveBeenCalled()
  })
  it('shows an empty result before the first document analysis', async () => {
    vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, status: 'UPLOADED' } as RegistryProcessing)
    open()
    expect(await within(await screen.findByRole('region', { name: '등기사항증명서 분석' })).findByText('분석내용 없음')).toBeTruthy()
  })
  it('omits the deferred co-owner section and warnings while preserving the rights', async () => {
    vi.mocked(listingsApi.registryFacts).mockResolvedValue({ documentId: 1, registrySnapshotId: 3, owners: [
      { name: '합성소유자', ownershipShare: null, ownershipType: 'OWNER' },
      { name: '숨김공유자', ownershipShare: '12분의 1', ownershipType: 'CO_OWNER' },
    ], rights: [{ type: 'LEASEHOLD', amount: 200000000, priority: 2, registeredAt: null, status: 'ACTIVE' }], ownershipNeedsReview: true })
    open()
    const facts = await screen.findByRole('region', { name: '등기사항증명서 분석 내역' })
    expect(await within(facts).findByText('합성소유자')).toBeTruthy()
    expect(facts.textContent).not.toContain('공유자')
    expect(facts.textContent).not.toContain('12분의 1')
    expect(within(facts).getByText('전세권')).toBeTruthy()
  })
  it('labels a corporate owner without exposing its registration number', async () => {
    vi.mocked(listingsApi.registryFacts).mockResolvedValue({ documentId: 1, registrySnapshotId: 3, owners: [
      { name: '합성 테스트법인', entityType: 'CORPORATION', ownershipShare: null, ownershipType: 'OWNER' },
    ], rights: [], ownershipNeedsReview: false })
    open()
    const facts = await screen.findByRole('region', { name: '등기사항증명서 분석 내역' })
    expect(await within(facts).findByText('소유자 법인명')).toBeTruthy()
    expect(within(facts).getByText('법인등록번호')).toBeTruthy()
    expect(within(facts).getByText('식별정보 보호로 표시하지 않음')).toBeTruthy()
  })

  it('keeps owner details while excluding deferred co-owner details', async () => {
    vi.mocked(listingsApi.registryFacts).mockResolvedValue({ documentId: 1, registrySnapshotId: 3, owners: [
      { name: '합성소유자', ownershipShare: null, ownershipType: 'OWNER', priorityNumber: '3-1', ownerAddress: '합성주소', acquisitionAmount: 123456789 },
      { name: '합성공유자', ownershipShare: '12분의 1', ownershipType: 'CO_OWNER', priorityNumber: '5', ownerAddress: '공유자합성주소' },
    ], rights: [], ownershipNeedsReview: true })
    open()
    const facts = await screen.findByRole('region', { name: '등기사항증명서 분석 내역' })
    expect(await within(facts).findByText('3-1')).toBeTruthy()
    expect(within(facts).getByText('123,456,789원')).toBeTruthy()
    expect(within(facts).getAllByText('소유자 성명')).toHaveLength(1)
    expect(within(facts).queryByText('공유자합성주소')).toBeNull()
  })
  it.each(['PARSED', 'FAILED'] as const)('clears the selected PDF and disables the button when analysis is %s', async status => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(listingsApi.uploadAndAnalyzeRegistry).mockResolvedValue({ listingId: 8, documentId: 7, status } as RegistryProcessing)
    open()
    const button = await screen.findByRole('button', { name: '등기사항증명서 분석' })
    const input = screen.getByLabelText('등기사항증명서 PDF 파일') as HTMLInputElement
    fireEvent.change(input, { target: { files: [new File(['%PDF-1.7\nfixture'], 'fixture.pdf', { type: 'application/pdf' })] } })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    await waitFor(() => expect(listingsApi.uploadAndAnalyzeRegistry).toHaveBeenCalledTimes(1))
    await waitFor(() => expect(screen.getByText('등기사항증명서 PDF 선택')).toBeTruthy())
    expect(button).toBeDisabled()
    expect(input.value).toBe('')
  })
  it('clears the PDF when a later status refresh reports analysis completion', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(listingsApi.uploadAndAnalyzeRegistry).mockResolvedValue({ listingId: 8, documentId: 7, status: 'PROCESSING' } as RegistryProcessing)
    open()
    const button = await screen.findByRole('button', { name: '등기사항증명서 분석' })
    fireEvent.change(screen.getByLabelText('등기사항증명서 PDF 파일'), { target: { files: [new File(['%PDF-1.7\nfixture'], 'fixture.pdf', { type: 'application/pdf' })] } })
    await waitFor(() => expect(button).toBeEnabled())
    fireEvent.click(button)
    await waitFor(() => expect(listingsApi.uploadAndAnalyzeRegistry).toHaveBeenCalledTimes(1))
    expect(screen.getByText('fixture.pdf')).toBeTruthy()
    vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, documentId: 7, status: 'PARSED' } as RegistryProcessing)
    fireEvent.click(await screen.findByRole('button', { name: '상태 새로고침' }))
    await waitFor(() => expect(screen.queryByText('fixture.pdf')).toBeNull())
    expect(screen.getByText('등기사항증명서 PDF 선택')).toBeTruthy()
    expect(button).toBeDisabled()
  })
  it('does not display a superseded score containing the deferred assessment', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue({ ...safety, reanalysisAvailable: true, reanalysisReason: 'ANALYSIS_VERSION_CHANGED', analysis: { ...safety.analysis, factors: [{ id: 1, factorType: 'CO_OWNER_ISSUE', description: '과거 평가', score: 30, severity: 'HIGH' }] } } as SafetyResult)
    open()
    expect(await screen.findByText(/이전 평가 기준의 결과입니다/)).toBeTruthy()
    expect(screen.queryByRole('region', { name: '안전성 분석 결과' })).toBeNull()
    expect(screen.getByRole('button', { name: '새 기준으로 재분석' })).toBeTruthy()
  })
  it('shows saved address differences in the failure card and one dismissible error dialog', async () => {
    vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, documentId: 17, status: 'FAILED', errorCode: 'REGISTRY_ADDRESS_MISMATCH', processedAt: '2026-10-26T00:00:00', addressMismatch: { registeredAddress: '서울 테스트로 101호', lotAddress: '서울 테스트로 102호', roadAddress: null } } as RegistryProcessing)
    open()
    const dialog = await screen.findByRole('alertdialog')
    expect(within(dialog).getByText('등록한 주소')).toBeTruthy()
    expect(within(dialog).getByText('문서에서 인식한 지번주소')).toBeTruthy()
    expect(dialog.querySelectorAll('mark').length).toBeGreaterThan(0)
    const card = screen.getByRole('alert', { name: '등기사항증명서 분석 실패 안내' })
    expect(card).toHaveTextContent('REGISTRY_ADDRESS_MISMATCH')
    fireEvent.click(within(dialog).getByRole('button', { name: '확인' }))
    await waitFor(() => expect(screen.queryByRole('alertdialog')).toBeNull())
    fireEvent.click(screen.getByRole('button', { name: '상태 새로고침' }))
    expect(screen.queryByRole('alertdialog')).toBeNull()
    expect(card).toHaveTextContent('해당 매물의 등기사항증명서 PDF를 다시 선택')
    expect(listingsApi.uploadAndAnalyzeRegistry).not.toHaveBeenCalled()
  })
  it.each(['PROCESSING', 'FAILED'] as const)('does not start safety analysis while the latest registry document is %s', async status => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue({ ...safety, reanalysisAvailable: true, reanalysisReason: 'LISTING_CHANGED' })
    vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, documentId: 22, status } as RegistryProcessing)
    open()
    const section = await screen.findByRole('region', { name: '안전성 분석 실행' })
    expect(within(section).getByRole('button', { name: '안전성 분석' })).toBeDisabled()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })

  it('selects an earlier registry document and displays its completion state and own analysis', async () => {
    vi.mocked(listingsApi.registryHistory).mockResolvedValue([
      { id: 22, listingId: 8, originalFilename: 'latest.pdf', status: 'FAILED', sizeBytes: 120, createdAt: '2026-10-26T00:00:00' },
      { id: 21, listingId: 8, originalFilename: 'earlier.pdf', status: 'PARSED', sizeBytes: 110, createdAt: '2026-10-25T00:00:00', processedAt: '2026-10-25T01:00:00' },
    ] as import('../../types/api').RegistryDocument[])
    open()
    const history = await screen.findByRole('combobox', { name: '문서 이력' })
    fireEvent.change(history, { target: { value: '21' } })
    expect(await screen.findByText('파일 분석 완료')).toBeTruthy()
    expect(screen.getByText(/earlier.pdf/)).toBeTruthy()
    expect(screen.getByRole('button', { name: 'PDF 다운로드' })).toBeTruthy()
    await waitFor(() => expect(listingsApi.registryFacts).toHaveBeenCalledWith(8, 21))
    expect(listingsApi.uploadAndAnalyzeRegistry).not.toHaveBeenCalled()
  })
  it('shows exactly eight ordered checks, with five optional features explicitly marked as planned', async () => {
    open()
    const list = await screen.findByRole('list', { name: '안전성 점검 항목' })
    const rows = within(list).getAllByRole('listitem')
    expect(rows).toHaveLength(8)
    expect(rows.map(row => within(row).getByRole('heading').textContent)).toEqual([
      '등기사항증명서 분석', '건축물대장 분석', '전입세대열람원 확인',
      '전세가율 및 시세 분석', '계약정보 등록 및 표준계약서 생성', '소유자·임대인 신원 및 인감증명서 확인',
      '관리비 및 세금·체납 정보 확인', '안전성 분석',
    ])
    for(const row of rows.slice(2, 7)) {
      expect(within(row).getByText('준비 중')).toBeTruthy()
      expect(within(row).getByText('선택')).toBeTruthy()
      expect(within(row).queryByRole('button')).toBeNull()
    }
    expect(within(list).queryByText(/사용승인서/)).toBeNull()
    expect(await screen.findByRole('button', { name: '안전성 분석 시작' })).toBeTruthy()
  })
  it.each([
    { status: 'PARSED', stale: false, optional: true },
    { status: 'PARSED', stale: true, optional: false },
    { status: 'FAILED', stale: false, optional: false },
  ] as const)('marks API lookup as optional only for a current completed document: %j', async ({ status, stale, optional }) => {
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({ executionStatus: 'COMPLETED', resolutionStatus: 'MATCHED' } as BuildingIdentification)
    const latest = { id: 1, status, stale, files: [], attemptCount: 1, createdAt: '2026-10-26T01:00:00Z',
      result: { entries: [{ fileIndex: 1, page: 1, documentType: 'GENERAL', violationStatus: 'NO_MARK_VISIBLE', evidence: [] }], checks: [], violationMentioned: false, conclusion: 'DOCUMENTS_COMPARED' } } as BuildingRegisterReview
    vi.mocked(listingsApi.buildingRegister).mockResolvedValue({ latest, history: [] })
    open()
    const apiCard = await screen.findByRole('region', { name: '건축물대장 분석 상태' })
    expect(Boolean(within(apiCard).queryByText('건축물대장 문서 분석이 완료되어 추가 조회는 선택사항입니다.'))).toBe(optional)
    expect(within(apiCard).getByRole('button', { name: '건축물대장 조회' })).toBeEnabled()
    if (optional) {
      expect(screen.getByRole('img', { name: '건축물대장 분석: 분석 완료' })).toBeTruthy()
      expect(screen.queryByText('변경 전 자료')).toBeNull()
    }
    expect(listingsApi.retryBuildingIdentification).not.toHaveBeenCalled()
  })
  it('separates a successful unit identification from complete building-register review', async () => {
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'COMPLETED', resolutionStatus: 'MATCHED', reason: null,
    } as BuildingIdentification)
    open()
    expect(await screen.findByRole('img', { name: '건축물대장 분석: 호실 식별 완료' })).toBeTruthy()
    expect(screen.queryByRole('img', { name: '건축물대장 분석: 완료' })).toBeNull()
    expect(screen.getByText(/대장 파일을 등록하면 관련 기재를 추가로 확인/)).toBeTruthy()
  })
  it('marks an old result as needing reanalysis without executing analysis automatically', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue({ ...safety, reanalysisAvailable: true, reanalysisReason: 'REGISTRY_CHANGED' })
    open()
    expect(await screen.findByRole('img', { name: '안전성 분석: 재분석 필요' })).toBeTruthy()
    expect(screen.queryByRole('img', { name: '안전성 분석: 완료' })).toBeNull()
    expect(screen.getByRole('button', { name: '새 기준으로 재분석' })).toBeTruthy()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it('labels saved analysis as registry-based and keeps unimplemented checks out of the score', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    const { container } = open()
    await screen.findByRole('region', { name: '안전성 분석 결과' })
    expect(screen.getByText(/이번 위험 점수는 등기사항증명서·거래조건/)).toBeTruthy()
    expect(container.textContent).not.toContain('등기부')
  })
  it('shows a static waiting step before analysis starts', async () => {
    const { container } = open()
    expect(await screen.findByRole('button', { name: '안전성 분석 시작' })).toBeTruthy()
    expect(screen.queryByRole('img', { name: '매물정보와 등기사항증명서 등록: 완료' })).toBeNull()
    expect(screen.getByRole('img', { name: '등기사항증명서 분석: 완료' })).toBeTruthy()
    expect(screen.getByRole('img', { name: '안전성 분석: 대기' })).toBeTruthy()
    expect(container.querySelector('svg.lucide-loader-circle')).toBeNull()
    expect(screen.getByText('등기사항증명서 분석이 완료되었습니다. 아래 안전성 분석 영역에서 분석을 시작할 수 있습니다.')).toBeTruthy()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it('shows progress only during the request and displays the result automatically when completed', async () => {
    let finish!: (value: SafetyResult) => void
    vi.mocked(listingsApi.analyze).mockImplementation(() => new Promise(resolve => { finish = resolve }))
    const { container } = open()
    fireEvent.click(await screen.findByRole('button', { name: '안전성 분석 시작' }))
    expect(await screen.findByText('안전성 분석을 진행하고 있습니다.')).toBeTruthy()
    expect(screen.getByRole('img', { name: '안전성 분석: 진행 중' })).toBeTruthy()
    expect(container.querySelector('svg.lucide-loader-circle')).toBeTruthy()
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    finish(safety)
    expect(await screen.findByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    await waitFor(() => expect(container.querySelector('svg.lucide-loader-circle')).toBeNull())
    expect(screen.getByRole('img', { name: '안전성 분석: 완료' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: '안전성 분석 시작' })).toBeNull()
  })
  it('distinguishes a waiting registry step from a failed registry step', async () => {
    vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, status: 'UPLOADED' } as RegistryProcessing)
    const view = open()
    expect(await screen.findByRole('img', { name: '등기사항증명서 분석: 대기' })).toBeTruthy()
    view.unmount()
    vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, status: 'FAILED' } as RegistryProcessing)
    open()
    expect(await screen.findByRole('img', { name: '등기사항증명서 분석: 오류' })).toBeTruthy()
  })
  it('shows an analysis error icon when the analysis request fails', async () => {
    vi.mocked(listingsApi.analyze).mockRejectedValue(new Error('합성 분석 실패'))
    open()
    fireEvent.click(await screen.findByRole('button', { name: '안전성 분석 시작' }))
    expect(await screen.findByRole('img', { name: '안전성 분석: 오류' })).toBeTruthy()
    expect(screen.getByText('안전성 분석에 실패했습니다. 다시 시도해 주세요.')).toBeTruthy()
  })
  it('shows saved results directly without a redundant result button or another analysis', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    open()
    expect(await screen.findByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    expect(screen.queryByRole('button', { name: '안전성 분석 결과 보기' })).toBeNull()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
    expect(screen.queryByRole('button', { name: '안전성 분석 시작' })).toBeNull()
  })
  it('keeps the original filter and page in the back link even after loading the detail URL directly', async () => {
    open('/listings/8?filter=completed&page=2')
    const back = await screen.findByRole('link', { name: '내 매물' })
    expect(back.getAttribute('href')).toBe('/listings?filter=completed&page=2')
  })
  it('shows Korean risk names, formatted amounts and detailed labels', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue({ ...safety, factorCount: 2, analysis: { ...safety.analysis, factors: [
      { id: 1, riskAnalysisId: 2, factorType: 'MORTGAGE', severity: 'HIGH', score: 30, value: '300000000', description: 'MORTGAGE 등기 금액 300000000원', createdAt: '2026-10-16T00:00:00' },
      { id: 2, riskAnalysisId: 2, factorType: 'SEIZURE', severity: 'CRITICAL', score: 50, value: 'PROVISIONAL_SEIZURE', description: '활성 가압류가 존재합니다. 권리자: 테스트기관', createdAt: '2026-10-16T00:00:00' },
    ] } })
    const { container } = open()
    expect(await screen.findByRole('heading', { name: '가압류' })).toBeTruthy()
    expect(screen.getByRole('heading', { name: '근저당권' })).toBeTruthy()
    expect(screen.getByText('300,000,000원')).toBeTruthy()
    expect(screen.getByText('근저당권 등기 금액 300,000,000원')).toBeTruthy()
    expect(screen.getByText('권리 종류')).toBeTruthy()
    expect(screen.getByText('30점')).toBeTruthy()
    expect(container.textContent).not.toContain('MORTGAGE')
    expect(container.textContent).not.toContain('PROVISIONAL_SEIZURE')
  })
  it('refreshes saved data without starting a new analysis', async () => {
    open()
    await screen.findByRole('button', { name: '안전성 분석 시작' })
    const reads = vi.mocked(listingsApi.safetyResult).mock.calls.length
    fireEvent.click(await screen.findByRole('button', { name: '상태 새로고침' }))
    await waitFor(() => expect(vi.mocked(listingsApi.safetyResult).mock.calls.length).toBeGreaterThan(reads))
    expect(listingsApi.analyze).not.toHaveBeenCalled()
    expect(listingsApi.startProcessing).not.toHaveBeenCalled()
  })
  it.each(['PENDING', 'RUNNING'] as const)('shows building progress while %s', async (executionStatus) => {
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({ executionStatus, resolutionStatus: null } as BuildingIdentification)
    open()
    const bar = await screen.findByRole('progressbar', { name: '건축물 식별 진행 상황' })
    expect(bar.hasAttribute('aria-valuenow')).toBe(false)
    expect((screen.getByRole('button', { name: '건축물대장 조회' }) as HTMLButtonElement).disabled).toBe(true)
  })
  it('allows API lookup without either document and sends query-only corrections', async () => {
    vi.mocked(listingsApi.processing).mockRejectedValue(missing)
    vi.mocked(listingsApi.retryBuildingIdentification).mockResolvedValue({ executionStatus: 'INPUT_INCOMPLETE', resolutionStatus: null } as BuildingIdentification)
    const selected = { address: listing.address, userSelectedType: 'J' as const, roadAddress: '', jibunAddress: listing.address,
      bcode: '1171010100', sigunguCode: '11710', noSelected: 'N' as const }
    window.kakao = { Postcode: class {
      constructor(private options: ConstructorParameters<NonNullable<typeof window.kakao>['Postcode']>[0]) {}
      open() { this.options.oncomplete(selected) }
    } }
    open()
    const card = await screen.findByRole('region', { name: '건축물대장 분석 상태' })
    expect(within(card).getByText(/문서 분석 없이 조회 가능합니다/)).toBeTruthy()
    expect(within(card).getByRole('button', { name: '건축물대장 조회' })).toBeEnabled()
    fireEvent.click(within(card).getByRole('button', { name: '조회 주소검색' }))
    fireEvent.change(within(card).getByLabelText('조회용 상세주소 (동·층·호)'), { target: { value: '160동 지상 1층 101호' } })
    fireEvent.click(within(card).getByRole('button', { name: '건축물대장 조회' }))
    await waitFor(() => expect(listingsApi.retryBuildingIdentification).toHaveBeenCalledWith(8, {
      addressSearchEvidence: { selectedType: 'J', selectedAddress: listing.address, jibunAddress: listing.address,
        bcode: '1171010100', sigunguCode: '11710', noSelected: 'N' }, detailAddress: '160동 지상 1층 101호',
    }))
    expect(listingsApi.uploadAndAnalyzeRegistry).not.toHaveBeenCalled()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })

  it('offers a manual lookup for an earlier disabled request without triggering it on render', async () => {
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({ executionStatus: 'DISABLED', resolutionStatus: null } as BuildingIdentification)
    open()
    expect(await screen.findByRole('button', { name: '건축물대장 조회' })).toBeEnabled()
    expect(listingsApi.retryBuildingIdentification).not.toHaveBeenCalled()
  })
  it('explains missing confirmed address evidence without showing active progress', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'INPUT_INCOMPLETE', reason: 'CONFIRMED_JIBUN_REQUIRED', resolutionStatus: null,
    } as BuildingIdentification)
    const { container } = open()
    expect(await screen.findByText(/주소검색으로 확정한 지번·법정동 정보가 없어/)).toBeTruthy()
    expect(screen.queryByRole('progressbar', { name: '건축물 식별 진행 상황' })).toBeNull()
    expect(screen.getByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    expect(container.textContent).not.toContain('CONFIRMED_JIBUN_REQUIRED')
  })
  it('shows building call failure separately from an unchanged risk result', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'CALL_FAILED', resolutionStatus: null, reason: 'INTEGRATION_UNAVAILABLE',
    } as BuildingIdentification)
    open()
    expect(await screen.findByText('건축물대장 조회에 실패했습니다. 현재 식별 결과가 없습니다.')).toBeTruthy()
    expect(screen.getByText('위험도 낮음')).toBeTruthy()
  })
  it('distinguishes unverified identification from a failed call', async () => {
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'COMPLETED', resolutionStatus: 'UNVERIFIABLE',
    } as BuildingIdentification)
    open()
    expect(await screen.findByText('건축물대장 자료로는 해당 호를 확정할 수 없습니다.')).toBeTruthy()
    expect(screen.queryByText('건축물대장 조회에 실패했습니다. 현재 식별 결과가 없습니다.')).toBeNull()
  })
  it('explains an XML response failure without hiding saved results or exposing internal codes', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'CALL_FAILED', resolutionStatus: null, reason: 'BASIS_PAGE_1_MALFORMED_RESPONSE_XML',
    } as BuildingIdentification)
    const { container } = open()
    expect(await screen.findByText(/건축물대장 응답 형식을 해석하지 못해/)).toBeTruthy()
    expect(screen.getByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    expect(screen.getByText('등기사항증명서 분석이 완료되었습니다.')).toBeTruthy()
    expect(container.textContent).not.toContain('MALFORMED_RESPONSE')
    expect(listingsApi.analyze).not.toHaveBeenCalled()
    expect(listingsApi.startProcessing).not.toHaveBeenCalled()
  })
  it('explains an empty response and retains saved results and the retry button', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'CALL_FAILED', resolutionStatus: null, reason: 'EXPOS_PAGE_1_EMPTY_RESPONSE',
    } as BuildingIdentification)
    const { container } = open()
    expect(await screen.findByText(/공급기관 응답 내용이 비어 있어/)).toBeTruthy()
    expect(screen.getByRole('button', { name: '건축물대장 조회' })).toBeTruthy()
    expect(screen.getByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    expect(screen.getByText('등기사항증명서 분석이 완료되었습니다.')).toBeTruthy()
    expect(container.textContent).not.toContain('EMPTY_RESPONSE')
    expect(listingsApi.startProcessing).not.toHaveBeenCalled()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it('retries only the building lookup and keeps saved safety results while pending', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({ executionStatus: 'CALL_FAILED', resolutionStatus: null } as BuildingIdentification)
    const pending = { executionStatus: 'PENDING', resolutionStatus: null } as BuildingIdentification
    vi.mocked(listingsApi.retryBuildingIdentification).mockImplementation(async () => {
      vi.mocked(listingsApi.buildingIdentification).mockResolvedValue(pending)
      return pending
    })
    open()
    fireEvent.click(await screen.findByRole('button', { name: '건축물대장 조회' }))
    expect(await screen.findByText('건축물대장 조회를 기다리고 있습니다.')).toBeTruthy()
    expect(listingsApi.retryBuildingIdentification).toHaveBeenCalledWith(8, undefined)
    expect((screen.getByRole('button', { name: '건축물대장 조회' }) as HTMLButtonElement).disabled).toBe(true)
    expect(screen.getByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    expect(listingsApi.startProcessing).not.toHaveBeenCalled()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it('discloses an inferred floor rather than presenting it as an explicit match', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'COMPLETED', resolutionStatus: 'MATCHED',
      reason: 'FLOOR_INFERRED_FROM_UNIT_NUMBER', managementNumber: 'synthetic-unit',
    } as BuildingIdentification)
    const { container } = open()
    expect(await screen.findByText('호수로 추정한 층을 적용해 일치하는 호를 확인했습니다.')).toBeTruthy()
    expect(screen.getByText('호수로 추정한 층을 적용해 일치하는 호를 확인했습니다.').className).toContain('bg-amber-50')
    expect(screen.queryByText(/추정에 기반한 결과이며/)).toBeNull()
    expect(screen.queryByText(/마지막 두 자리/)).toBeNull()
    expect((screen.getByRole('button', { name: '건축물대장 조회' }) as HTMLButtonElement).disabled).toBe(false)
    expect(container.textContent).not.toContain('FLOOR_INFERRED_FROM_UNIT_NUMBER')
    expect(screen.getByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it.each(['UNVERIFIABLE', 'AMBIGUOUS', 'NOT_FOUND', 'MATCHED'] as const)(
    'offers another building lookup for every completed result: %s', async (resolutionStatus) => {
      vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
        executionStatus: 'COMPLETED', resolutionStatus,
        reason: resolutionStatus === 'AMBIGUOUS' ? 'FLOOR_INFERRED_FROM_UNIT_NUMBER' : null,
      } as BuildingIdentification)
      open()
      await screen.findByText('건축물대장 조회 완료')
      expect((screen.getByRole('button', { name: '건축물대장 조회' }) as HTMLButtonElement).disabled).toBe(false)
      expect(screen.queryByText('호수로 추정한 층을 적용해 일치하는 호를 확인했습니다.')).toBeNull()
    })
  it('explains a conflicting inference without claiming a match', async () => {
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      executionStatus: 'COMPLETED', resolutionStatus: 'UNVERIFIABLE', reason: 'FLOOR_INFERENCE_CONFLICT',
    } as BuildingIdentification)
    open()
    expect(await screen.findByText(/호수로 추정한 층과 등록·등기사항증명서의 층 정보가 달라/)).toBeTruthy()
    expect(screen.getByRole('button', { name: '건축물대장 조회' })).toBeTruthy()
  })
  it('explains missing supplier floor data without exposing internal codes or changing saved analysis', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    vi.mocked(listingsApi.buildingIdentification).mockResolvedValue({
      id: 2, executionStatus: 'COMPLETED', resolutionStatus: 'UNVERIFIABLE',
      reason: 'CANDIDATE_FLOOR_NUMBER_UNAVAILABLE',
    } as BuildingIdentification)
    const { container } = open()
    expect(await screen.findByText(/공급기관 층 정보 부족으로 확인 불가/)).toBeTruthy()
    expect(screen.getByRole('button', { name: '건축물대장 조회' })).toBeTruthy()
    expect(screen.getByText('건축물대장 조회 완료')).toBeTruthy()
    expect(screen.getByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    expect(screen.getByText('위험도 낮음')).toBeTruthy()
    expect(screen.getByText('등기사항증명서 분석이 완료되었습니다.')).toBeTruthy()
    expect(container.textContent).not.toContain('CANDIDATE_FLOOR_NUMBER_UNAVAILABLE')
    expect(screen.queryByText('건축물대장 조회에 실패했습니다. 현재 식별 결과가 없습니다.')).toBeNull()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
    expect(listingsApi.startProcessing).not.toHaveBeenCalled()
  })
})
