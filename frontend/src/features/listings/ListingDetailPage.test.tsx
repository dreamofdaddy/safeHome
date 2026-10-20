import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ListingDetailPage } from './ListingDetailPage'
import { listingsApi } from './api'
import type { Listing, RegistryProcessing, SafetyResult } from '../../types/api'

vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ user: { id: 7 } }) }))
vi.mock('./api', () => ({ listingsApi: { one: vi.fn(), processing: vi.fn(), safetyResult: vi.fn(), analyze: vi.fn(), startProcessing: vi.fn(), remove: vi.fn() } }))
vi.mock('recharts', () => ({ Cell: () => null, Pie: () => null, PieChart: () => null, ResponsiveContainer: () => null }))

const listing = { id: 8, registeredByUserId: 7, address: '서울 테스트로 1', buildingType: 'HOUSE', transactionType: 'JEONSE', depositAmount: 200000000, status: 'REGISTRY_READY' } as Listing
const safety: SafetyResult = { listingId: 8, listingStatus: 'SAFETY_ANALYZED', summary: '분석 완료', highestSeverity: null, factorCount: 0, latestRegistrySnapshotId: 3, reanalysisAvailable: false, reanalysisReason: 'UP_TO_DATE', analysis: { id: 2, propertyId: 2, registrySnapshotId: 3, analysisVersion: '1', policyVersion: '1', riskLevel: 'LOW', riskScore: 0, analyzedAt: '2026-10-15T16:00:00', factors: [] } }
const missing = Object.assign(new Error('결과 없음'), { status: 404 })

function open(path = '/listings/8') {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false, retryDelay: 0 } } })
  return render(<QueryClientProvider client={client}><MemoryRouter initialEntries={[path]}><Routes><Route path="/listings/:id" element={<ListingDetailPage />} /></Routes></MemoryRouter></QueryClientProvider>)
}
beforeEach(() => {
  vi.mocked(listingsApi.one).mockResolvedValue(listing)
  vi.mocked(listingsApi.processing).mockResolvedValue({ listingId: 8, status: 'PARSED' } as RegistryProcessing)
  vi.mocked(listingsApi.safetyResult).mockRejectedValue(missing)
})
afterEach(() => { cleanup(); vi.resetAllMocks() })

describe('safety analysis state', () => {
  it('shows a static waiting step before analysis starts', async () => {
    const { container } = open()
    expect(await screen.findByRole('button', { name: '안전성 분석 시작' })).toBeTruthy()
    expect(container.querySelector('svg.lucide-loader-circle')).toBeNull()
    expect(screen.getByText('등기부 판독이 완료되었습니다. 아래 버튼으로 안전성 분석을 시작해 주세요.')).toBeTruthy()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it('shows progress only during the request and displays the result automatically when completed', async () => {
    let finish!: (value: SafetyResult) => void
    vi.mocked(listingsApi.analyze).mockImplementation(() => new Promise(resolve => { finish = resolve }))
    const { container } = open()
    fireEvent.click(await screen.findByRole('button', { name: '안전성 분석 시작' }))
    expect(await screen.findByText('안전성 분석을 진행하고 있습니다.')).toBeTruthy()
    expect(container.querySelector('svg.lucide-loader-circle')).toBeTruthy()
    vi.mocked(listingsApi.safetyResult).mockResolvedValue(safety)
    finish(safety)
    expect(await screen.findByRole('region', { name: '안전성 분석 결과' })).toBeTruthy()
    await waitFor(() => expect(container.querySelector('svg.lucide-loader-circle')).toBeNull())
    expect(screen.queryByRole('button', { name: '안전성 분석 시작' })).toBeNull()
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
})
