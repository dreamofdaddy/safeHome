import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { RegisterListingPage } from './RegisterListingPage'
import { listingsApi } from './api'
import type { Listing } from '../../types/api'

vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ user: { id: 7 } }) }))
vi.mock('./api', () => ({ listingsApi: { one: vi.fn(), update: vi.fn(), register: vi.fn(), create: vi.fn() } }))
afterEach(() => { cleanup(); vi.clearAllMocks(); vi.unstubAllGlobals() })
const listing = { id: 9, registeredByUserId: 7, address: '서울 테스트로 1', detailAddress: '안전아파트 제202호', buildingName: '안전아파트', buildingType: 'APARTMENT', transactionType: 'JEONSE', depositAmount: 200000000, status: 'REGISTRY_FAILED', addressSearchEvidence: { selectedType: 'J', selectedAddress: '서울 테스트로 1', jibunAddress: '서울 테스트로 1', bcode: '1171010100', sigunguCode: '11710', noSelected: 'N' } } as Listing

function open(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/listings/new" element={<RegisterListingPage />} />
    <Route path="/listings/:id/edit" element={<RegisterListingPage />} />
    <Route path="/listings/:id" element={<p>수정 완료</p>} />
  </Routes></MemoryRouter></QueryClientProvider>)
}

describe('listing registration and correction', () => {
  it('defers registry attachments to the analysis screen without requiring a file', () => {
    const { container } = open('/listings/new')
    expect(screen.queryByLabelText('등기사항증명서 PDF 파일')).toBeNull()
    expect(screen.getByRole('button', { name: '매물 등록' })).toBeEnabled()
    expect(container.querySelector('input[type="file"]')).toBeNull()
    expect(listingsApi.register).not.toHaveBeenCalled()
  })
  it('creates only listing metadata and navigates to its detail', async () => {
    vi.mocked(listingsApi.create).mockResolvedValue(listing)
    vi.stubGlobal('kakao', { Postcode: class {
      constructor(private options: { oncomplete: (data: unknown) => void }) {}
      open() { this.options.oncomplete({ userSelectedType: 'J', jibunAddress: '서울 테스트로 1', bcode: '1171010100', sigunguCode: '11710', noSelected: 'N' }) }
    } })
    open('/listings/new')
    fireEvent.click(screen.getByRole('button', { name: '주소검색' }))
    fireEvent.change(screen.getByLabelText('보증금', { exact: false }), { target: { value: '10000000' } })
    fireEvent.click(screen.getByRole('button', { name: '매물 등록' }))
    await waitFor(() => expect(listingsApi.create).toHaveBeenCalledWith(expect.objectContaining({ address: '서울 테스트로 1', depositAmount: 10000000 })))
    expect(await screen.findByText('수정 완료')).toBeTruthy()
    expect(listingsApi.register).not.toHaveBeenCalled()
  })
  it('shows a separate detail address and building type before building name', () => {
    open('/listings/new')
    expect(screen.getByRole('textbox', { name: '상세주소' })).toBeTruthy()
    expect(screen.queryByLabelText('동·호수')).toBeNull()
    const type = screen.getByLabelText('건물 유형', { exact: false })
    const name = screen.getByLabelText('건물명')
    expect(type.compareDocumentPosition(name) & Node.DOCUMENT_POSITION_FOLLOWING).toBeTruthy()
  })
  it('loads and saves a failed listing without requiring another PDF', async () => {
    vi.mocked(listingsApi.one).mockResolvedValue(listing)
    vi.mocked(listingsApi.update).mockResolvedValue({ ...listing, detailAddress: '안전아파트 제203호' })
    open('/listings/9/edit')
    const detail = await screen.findByDisplayValue('안전아파트 제202호')
    expect(detail).toBeDisabled()
    fireEvent.change(screen.getByLabelText('보증금', { exact: false }), { target: { value: '300000000' } })
    fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
    await waitFor(() => expect(listingsApi.update).toHaveBeenCalledWith(9, expect.objectContaining({ address: '서울 테스트로 1', detailAddress: '안전아파트 제202호', depositAmount: 300000000 })))
    expect(await screen.findByText('수정 완료')).toBeTruthy()
  })
  it('locks basic property fields and preserves original address evidence when saving terms', async () => {
    vi.mocked(listingsApi.one).mockResolvedValue(listing)
    vi.mocked(listingsApi.update).mockResolvedValue(listing)
    open('/listings/9/edit')
    expect(await screen.findByDisplayValue('서울 테스트로 1')).toBeDisabled()
    expect(screen.getByRole('button', { name: '주소검색' })).toBeDisabled()
    expect(screen.getByLabelText('건물 유형', { exact: false })).toBeDisabled()
    expect(screen.getByLabelText('건물명')).toBeDisabled()
    fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
    await waitFor(() => expect(listingsApi.update).toHaveBeenCalledWith(9, expect.objectContaining({ addressSearchEvidence: listing.addressSearchEvidence, buildingName: listing.buildingName })))
  })
  it('can save trade terms on a legacy listing without address-search evidence', async () => {
    vi.mocked(listingsApi.one).mockResolvedValue({ ...listing, addressSearchEvidence: null })
    vi.mocked(listingsApi.update).mockResolvedValue(listing)
    open('/listings/9/edit')
    await screen.findByDisplayValue('서울 테스트로 1')
    fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
    await waitFor(() => expect(listingsApi.update).toHaveBeenCalledWith(9, expect.objectContaining({ addressSearchEvidence: null })))
  })
  it('requires address search before submission and makes the base address read-only', async () => {
    const { container } = open('/listings/new')
    expect((screen.getByRole('textbox', { name: '매물주소' }) as HTMLInputElement).readOnly).toBe(true)
    expect((screen.getByRole('textbox', { name: '상세주소' }) as HTMLInputElement).readOnly).toBe(false)
    fireEvent.submit(container.querySelector('form')!)
    expect(await screen.findByText(/주소검색에서 매물주소를 선택해 주세요/)).toBeTruthy()
    expect(listingsApi.register).not.toHaveBeenCalled()
  })
  it('rejects a search selection without confirmed cadastral information', async () => {
    vi.stubGlobal('kakao', { Postcode: class {
      constructor(private options: { oncomplete: (data: unknown) => void }) {}
      open() { this.options.oncomplete({ userSelectedType: 'R', roadAddress: '합성 테스트 도로', jibunAddress: '', bcode: '1171010100', sigunguCode: '11710', noSelected: 'Y' }) }
    } })
    open('/listings/new')
    fireEvent.click(screen.getByRole('button', { name: '주소검색' }))
    expect(await screen.findByText('선택한 주소를 확인할 수 없습니다. 다른 검색 결과를 선택해 주세요.')).toBeTruthy()
    expect((screen.getByRole('textbox', { name: '매물주소' }) as HTMLInputElement).value).toBe('')
    expect(listingsApi.register).not.toHaveBeenCalled()
  })
  it('does not render an edit form for someone else’s listing', async () => {
    vi.mocked(listingsApi.one).mockResolvedValue({ ...listing, registeredByUserId: 8 })
    open('/listings/9/edit')
    expect(await screen.findByText('본인이 등록한 매물만 수정할 수 있으며, 분석 중에는 완료 후 수정해 주세요.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: '수정 저장' })).toBeNull()
  })
})
