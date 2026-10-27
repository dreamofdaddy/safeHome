import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { BuildingRegisterCard } from './BuildingRegisterCard'
import { listingsApi } from './api'
import type { BuildingRegisterEnvelope, BuildingRegisterReview } from '../../types/api'

vi.mock('./api', () => ({ listingsApi: { uploadBuildingRegister: vi.fn(), uploadAndAnalyzeBuildingRegister: vi.fn(), startBuildingRegister: vi.fn(), buildingRegisterReview: vi.fn(), buildingRegisterFile: vi.fn() } }))
afterEach(() => { cleanup(); vi.restoreAllMocks(); vi.resetAllMocks() })
const uploaded: BuildingRegisterReview = { id: 1, status: 'UPLOADED', files: [{ index: 1, name: 'building.pdf', sizeBytes: 100, pages: 1 }], stale: false, attemptCount: 0, createdAt: '2026-10-22T00:00:00Z' }
function open(latest?: BuildingRegisterReview, buildingType = 'APARTMENT') {
  const data: BuildingRegisterEnvelope = { latest, history: latest ? [{ id: latest.id, status: latest.status, stale: false, createdAt: latest.createdAt }] : [] }
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><BuildingRegisterCard listingId={49} buildingType={buildingType} data={data} /></QueryClientProvider>)
}
describe('building register PDF workflow', () => {
  it('offers the official issue guide without triggering upload or AI parsing', () => {
    open()
    expect(screen.getByRole('link', { name: '세움터 발급 안내' })).toHaveAttribute('href', 'https://www.eais.go.kr/moct/bci/aaa01/BCIAAA01V01')
    expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeDisabled()
    expect(listingsApi.startBuildingRegister).not.toHaveBeenCalled()
  })
  it('rejects non-PDF selections before upload', async () => {
    open()
    fireEvent.change(screen.getByLabelText('해당 호실 전유부 PDF 파일 (필수)'), { target: { files: [new File(['not pdf'], 'fake.pdf', { type: 'application/pdf' })] } })
    expect(await screen.findByRole('alert')).toHaveTextContent('건축물대장 파일은 PDF 형식만')
    expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeDisabled()
    expect(listingsApi.uploadBuildingRegister).not.toHaveBeenCalled()
  })
  it('uploads and starts analysis together only after a file selection and consent', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(listingsApi.uploadAndAnalyzeBuildingRegister).mockResolvedValue({ ...uploaded, status: 'PROCESSING' })
    open()
    const file = new File(['%PDF-1.7\nsynthetic'], 'building.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('해당 호실 전유부 PDF 파일 (필수)'), { target: { files: [file] } })
    await waitFor(() => expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeEnabled())
    expect(listingsApi.uploadAndAnalyzeBuildingRegister).not.toHaveBeenCalled()
    fireEvent.click(screen.getByRole('button', { name: '건축물대장 분석' }))
    await waitFor(() => expect(listingsApi.uploadAndAnalyzeBuildingRegister).toHaveBeenCalledWith(49, [file], expect.any(String)))
    expect(listingsApi.uploadBuildingRegister).not.toHaveBeenCalled()
    expect(listingsApi.startBuildingRegister).not.toHaveBeenCalled()
  })
  it('does not transmit files when analysis consent is declined', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(false)
    open(uploaded)
    fireEvent.change(screen.getByLabelText('해당 호실 전유부 PDF 파일 (필수)'), { target: { files: [new File(['%PDF-1.7'], 'selected.pdf', { type: 'application/pdf' })] } })
    await waitFor(() => expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeEnabled())
    fireEvent.click(screen.getByRole('button', { name: '건축물대장 분석' }))
    expect(listingsApi.uploadAndAnalyzeBuildingRegister).not.toHaveBeenCalled()
  })
  it('shows a progress bar while processing without an invented percentage', () => {
    open({ ...uploaded, status: 'PROCESSING' })
    expect(screen.getByRole('progressbar', { name: '건축물대장 분석 진행 중' })).not.toHaveAttribute('aria-valuenow')
    expect(screen.queryByRole('button', { name: '건축물대장 분석 시작' })).toBeNull()
    expect(listingsApi.startBuildingRegister).not.toHaveBeenCalled()
  })
  it('accepts an exclusive register alone and labels the positive finding with its coverage', () => {
    open({ ...uploaded, status: 'PARSED', result: { conclusion: 'DOCUMENTS_COMPARED', violationMentioned: false,
      entries: [{ fileIndex: 1, page: 1, documentType: 'EXCLUSIVE', unitName: '202호', purpose: '아파트', violationStatus: 'NO_MARK_VISIBLE', evidence: [] }],
      checks: [{ entryIndex: 1, listingAddress: 'MATCHED', registryAddress: 'MATCHED', unit: 'MATCHED', apiUnit: 'MATCHED', purpose: 'MATCHED' }],
    } })
    expect(screen.getByText('위험요소가 발견되지 않았습니다.')).toBeTruthy()
    expect(screen.getByText(/선택 문서인 표제부는 미첨부 상태/)).toBeTruthy()
  })
  it('shows the no-risk message despite ordinary annotations and summary address comparisons', () => {
    open({ ...uploaded, status: 'PARSED', result: { conclusion: 'REVIEW_REQUIRED', violationMentioned: false,
      entries: [
        { fileIndex: 1, page: 1, documentType: 'EXCLUSIVE', violationStatus: 'NO_MARK_VISIBLE', evidence: [] },
        { fileIndex: 2, page: 1, documentType: 'SUMMARY', violationStatus: 'NO_MARK_VISIBLE', evidence: [{ page: 2, kind: 'AREA_CHANGE', text: '합성 면적 정정 기재' }] },
      ], checks: [{ entryIndex: 2, listingAddress: 'MATCHED', registryAddress: 'REVIEW_REQUIRED', unit: 'NOT_APPLICABLE', apiUnit: 'NOT_AVAILABLE', purpose: 'MATCHED' }],
    } })
    expect(screen.getByText('위험요소가 발견되지 않았습니다.')).toBeTruthy()
    expect(screen.queryByText('자료 간 비교 또는 문서 기재사항의 추가 확인이 필요합니다.')).toBeNull()
  })
  it('identifies the unreadable file and page instead of incorrectly claiming no risk', () => {
    open({ ...uploaded, status: 'PARSED', result: { conclusion: 'REVIEW_REQUIRED', violationMentioned: false,
      entries: [{ fileIndex: 1, page: 2, documentType: 'EXCLUSIVE', violationStatus: 'UNKNOWN', evidence: [] }], checks: [],
    } })
    expect(screen.getByText('위반 표시를 인식하지 못했습니다: 파일 1 · 2페이지.')).toBeTruthy()
    expect(screen.queryByText('위험요소가 발견되지 않았습니다.')).toBeNull()
  })

  it('shows evidence and missing violation marks as limited findings rather than a safety guarantee', () => {
    open({ ...uploaded, status: 'PARSED', result: { conclusion: 'DOCUMENTS_COMPARED', violationMentioned: false,
      entries: [{ fileIndex: 1, page: 1, documentType: 'EXCLUSIVE', unitName: '202호', purpose: '아파트', areaSquareMeters: 59.9, violationStatus: 'NO_MARK_VISIBLE', evidence: [] }],
      checks: [{ entryIndex: 1, listingAddress: 'MATCHED', registryAddress: 'REVIEW_REQUIRED', unit: 'MATCHED', apiUnit: 'DIFFERENT', purpose: 'MATCHED' }],
    } })
    expect(screen.getByText('제공된 페이지에서 표시 미확인')).toBeTruthy()
    expect(screen.getByText(/API 식별 호실: 불일치/)).toBeTruthy()
    expect(screen.getByText(/위반 표시 미확인은 위반 없음의 보증이 아니며/)).toBeTruthy()
    expect(screen.queryByText('위반건축물 아님')).toBeNull()
    expect(screen.getByText('위험요소가 발견되지 않았습니다.')).toBeTruthy()
  })
  it('does not submit optional certificates without the required exclusive register', async () => {
    open()
    fireEvent.change(screen.getByLabelText('표제부·총괄표제부 PDF 파일 (선택)'), { target: { files: [new File(['%PDF-1.7'], 'title.pdf', { type: 'application/pdf' })] } })
    await screen.findByText('title.pdf')
    expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeDisabled()
    expect(listingsApi.uploadBuildingRegister).not.toHaveBeenCalled()
  })
  it('submits the required and optional documents in one consented analysis request', async () => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(listingsApi.uploadAndAnalyzeBuildingRegister).mockResolvedValue({ ...uploaded, status: 'PROCESSING' })
    open()
    const exclusive = new File(['%PDF-1.7'], 'exclusive.pdf', { type: 'application/pdf' })
    const title = new File(['%PDF-1.7'], 'title.pdf', { type: 'application/pdf' })
    fireEvent.change(screen.getByLabelText('해당 호실 전유부 PDF 파일 (필수)'), { target: { files: [exclusive] } })
    await screen.findByText('exclusive.pdf')
    fireEvent.change(screen.getByLabelText('표제부·총괄표제부 PDF 파일 (선택)'), { target: { files: [title] } })
    await screen.findByText('title.pdf')
    fireEvent.click(screen.getByRole('button', { name: '건축물대장 분석' }))
    await waitFor(() => expect(listingsApi.uploadAndAnalyzeBuildingRegister).toHaveBeenCalledWith(49, [exclusive, title], expect.any(String)))
    expect(listingsApi.startBuildingRegister).not.toHaveBeenCalled()
  })
  it('applies the size limit to both inputs combined and preserves the required choice on rejection', async () => {
    open()
    const exclusive = new File(['%PDF-1.7'], 'exclusive.pdf', { type: 'application/pdf' })
    const title = new File(['%PDF-1.7'], 'title.pdf', { type: 'application/pdf' })
    Object.defineProperty(exclusive, 'size', { value: 6 * 1024 * 1024 })
    Object.defineProperty(title, 'size', { value: 6 * 1024 * 1024 })
    fireEvent.change(screen.getByLabelText('해당 호실 전유부 PDF 파일 (필수)'), { target: { files: [exclusive] } })
    await screen.findByText('exclusive.pdf')
    fireEvent.change(screen.getByLabelText('표제부·총괄표제부 PDF 파일 (선택)'), { target: { files: [title] } })
    expect(await screen.findByRole('alert')).toHaveTextContent('필수·선택 문서를 합쳐')
    expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: '선택 보완 문서 해제' }))
    expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeEnabled()
  })
  it.each(['PARSED', 'FAILED'] as const)('clears both file inputs after a terminal analysis response: %s', async status => {
    vi.spyOn(window, 'confirm').mockReturnValue(true)
    vi.mocked(listingsApi.uploadAndAnalyzeBuildingRegister).mockResolvedValue({ ...uploaded, status })
    open()
    fireEvent.change(screen.getByLabelText('해당 호실 전유부 PDF 파일 (필수)'), { target: { files: [new File(['%PDF-1.7'], 'selected.pdf', { type: 'application/pdf' })] } })
    await waitFor(() => expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeEnabled())
    fireEvent.click(screen.getByRole('button', { name: '건축물대장 분석' }))
    await waitFor(() => expect(screen.queryByText('selected.pdf')).toBeNull())
    expect(screen.getByRole('button', { name: '건축물대장 분석' })).toBeDisabled()
  })
  it('places document status above results and omits the removed history notice', () => {
    open({ ...uploaded, stale: true, status: 'PARSED', result: { conclusion: 'DOCUMENTS_COMPARED', violationMentioned: false, entries: [{ fileIndex: 1, page: 1, documentType: 'EXCLUSIVE', violationStatus: 'NO_MARK_VISIBLE', evidence: [] }], checks: [] } })
    const status = screen.getByRole('region', { name: '건축물대장 문서 상태' })
    const result = screen.getByRole('region', { name: '건축물대장 분석 결과' })
    expect(status.compareDocumentPosition(result) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.queryByText('이전 자료 또는 변경 전 비교 정보에 따른 이력입니다. 최신 자료 기준으로 확인해 주세요.')).toBeNull()
  })

  it('shows analysis results before the attachment form and has no registration button', () => {
    const { container } = open({ ...uploaded, status: 'PARSED', result: { conclusion: 'DOCUMENTS_COMPARED', violationMentioned: false, entries: [{ fileIndex: 1, page: 1, documentType: 'EXCLUSIVE', violationStatus: 'NO_MARK_VISIBLE', evidence: [] }], checks: [] } })
    const result = screen.getByRole('region', { name: '건축물대장 분석 결과' })
    const attachment = container.querySelector('input[type="file"]')!
    expect(result.compareDocumentPosition(attachment) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
    expect(screen.queryByRole('button', { name: '파일 등록' })).toBeNull()
  })
  it('uses a general register for detached and multi-family houses', () => {
    open(undefined, 'MULTI_FAMILY')
    expect(screen.getByLabelText('일반건축물대장 PDF 파일 (필수)')).toBeTruthy()
    expect(screen.getByText(/첨부하지 않아도 등기사항증명서 기반 안전성 분석/)).toBeTruthy()
  })
  it('requires replacement rather than paying to retry a known missing mandatory document', () => {
    open({ ...uploaded, status: 'FAILED', errorCode: 'EXCLUSIVE_REGISTER_REQUIRED' })
    expect(screen.getByRole('alert')).toHaveTextContent('필수 전유부가 확인되지 않았습니다')
    expect(screen.queryByRole('button', { name: '건축물대장 분석 다시 시도' })).toBeNull()
    expect(screen.queryByRole('checkbox')).toBeNull()
    expect(listingsApi.startBuildingRegister).not.toHaveBeenCalled()
  })
  it('flags an older parsed title-only review as missing the required document', () => {
    open({ ...uploaded, status: 'PARSED', result: { conclusion: 'DOCUMENTS_COMPARED', violationMentioned: false,
      entries: [{ fileIndex: 1, page: 1, documentType: 'TITLE', violationStatus: 'NO_MARK_VISIBLE', evidence: [] }], checks: [],
    } })
    expect(screen.getByText('필수 문서 보완 필요')).toBeTruthy()
    expect(screen.getByText(/해당 호실 전유부가 확인되지 않았습니다/)).toBeTruthy()
    expect(screen.queryByText(/자료 대조 일치 ·/)).toBeNull()
  })
  it('retains an exclusive violation mark with partial release evidence when the optional title is unmarked', () => {
    open({ ...uploaded, status: 'PARSED', result: { conclusion: 'VIOLATION_MENTIONED', violationMentioned: true,
      entries: [
        { fileIndex: 1, page: 1, documentType: 'EXCLUSIVE', violationStatus: 'MARKED', evidence: [{ page: 2, kind: 'VIOLATION', text: '위반 표기 일부 해제: A부분 시정, B부분 미시정' }] },
        { fileIndex: 2, page: 1, documentType: 'TITLE', violationStatus: 'NO_MARK_VISIBLE', evidence: [] },
      ], checks: [],
    } })
    expect(screen.getByText(/첨부 전유부에 위반 표시가 있습니다/)).toBeTruthy()
    expect(screen.getByRole('region', { name: '건축물대장 분석 결과' })).toHaveTextContent('위반 표기 일부 해제: A부분 시정, B부분 미시정')
    expect(screen.queryByText(/자료 대조 일치 ·/)).toBeNull()
  })

})
