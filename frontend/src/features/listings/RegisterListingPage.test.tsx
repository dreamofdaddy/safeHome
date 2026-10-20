import { render, screen, fireEvent, waitFor, cleanup } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Routes, Route } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { RegisterListingPage } from './RegisterListingPage'
import { listingsApi } from './api'
import type { Listing } from '../../types/api'

vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ user: { id: 7 } }) }))
vi.mock('./api', () => ({ listingsApi: { one: vi.fn(), update: vi.fn(), register: vi.fn() } }))
afterEach(() => { cleanup(); vi.clearAllMocks() })
const listing = { id: 9, registeredByUserId: 7, address: '서울 테스트로 1', detailAddress: '안전아파트 제202호', buildingName: '안전아파트', buildingType: 'APARTMENT', transactionType: 'JEONSE', depositAmount: 200000000, status: 'REGISTRY_FAILED' } as Listing

function open(path: string) {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><Routes>
    <Route path="/listings/new" element={<RegisterListingPage />} />
    <Route path="/listings/:id/edit" element={<RegisterListingPage />} />
    <Route path="/listings/:id" element={<p>수정 완료</p>} />
  </Routes></MemoryRouter></QueryClientProvider>)
}

describe('listing registration and correction', () => {
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
    fireEvent.change(detail, { target: { value: '안전아파트 제203호' } })
    fireEvent.click(screen.getByRole('button', { name: '수정 저장' }))
    await waitFor(() => expect(listingsApi.update).toHaveBeenCalledWith(9, expect.objectContaining({ address: '서울 테스트로 1', detailAddress: '안전아파트 제203호' })))
    expect(await screen.findByText('수정 완료')).toBeTruthy()
  })
  it('does not render an edit form for someone else’s listing', async () => {
    vi.mocked(listingsApi.one).mockResolvedValue({ ...listing, registeredByUserId: 8 })
    open('/listings/9/edit')
    expect(await screen.findByText('본인이 등록한 판독 대기·실패 매물만 수정할 수 있습니다.')).toBeTruthy()
    expect(screen.queryByRole('button', { name: '수정 저장' })).toBeNull()
  })
})
