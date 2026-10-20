import { cleanup, fireEvent, render, screen, waitFor, within } from '@testing-library/react'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { ListingsPage } from './ListingsPage'
import { listingsApi } from './api'
import type { Listing, SafetyResult } from '../../types/api'

vi.mock('../auth/AuthProvider', () => ({ useAuth: () => ({ user: { id: 7 } }) }))
vi.mock('./api', () => ({ listingsApi: { mine: vi.fn(), documents: vi.fn(), safetyResult: vi.fn(), analyze: vi.fn() } }))

const base = { registeredByUserId: 7, address: '서울 테스트로 1', transactionType: 'JEONSE', depositAmount: 200000000 } as Listing
const result = { analysis: { riskLevel: 'MEDIUM', riskScore: 42 }, reanalysisAvailable: false } as SafetyResult
beforeEach(() => {
  vi.mocked(listingsApi.mine).mockResolvedValue([
    { ...base, id: 1, buildingName: '대기 매물', status: 'READY_FOR_ANALYSIS' },
    { ...base, id: 2, buildingName: '완료 매물', status: 'SAFETY_ANALYZED' },
    { ...base, id: 3, registeredByUserId: 8, status: 'SAFETY_ANALYZED' },
  ])
  vi.mocked(listingsApi.documents).mockResolvedValue([])
  vi.mocked(listingsApi.safetyResult).mockResolvedValue(result)
})
afterEach(() => { cleanup(); vi.resetAllMocks() })
function open(path = '/') {
  return render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><MemoryRouter initialEntries={[path]}><ListingsPage /></MemoryRouter></QueryClientProvider>)
}

describe('listing result badges', () => {
  it('passes the current filter and page to the detail link', async () => {
    vi.mocked(listingsApi.mine).mockResolvedValue(Array.from({ length: 11 }, (_, i) => ({ ...base, id: i + 1, buildingName: `완료 매물 ${i + 1}`, status: 'SAFETY_ANALYZED' })))
    open('/listings?filter=completed&page=2')
    const heading = await screen.findByText('완료 매물 11')
    expect(heading.closest('a')?.getAttribute('href')).toBe('/listings/11?filter=completed&page=2')
  })
  it('filters by the selected summary button while preserving overall counts', async () => {
    open()
    await screen.findByText('완료 매물')
    fireEvent.click(screen.getByRole('button', { name: '분석 완료 1개' }))
    expect(screen.queryByText('대기 매물')).toBeNull()
    expect(screen.getByText('완료 매물')).toBeTruthy()
    expect(screen.getByRole('button', { name: '분석 완료 1개' }).getAttribute('aria-pressed')).toBe('true')
    fireEvent.click(screen.getByRole('button', { name: '안전성 분석 대기 1개' }))
    expect(screen.getByText('대기 매물')).toBeTruthy()
    expect(screen.queryByText('완료 매물')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: '등록한 매물 2개' }))
    expect(screen.getByText('대기 매물')).toBeTruthy()
    expect(screen.getByText('완료 매물')).toBeTruthy()
  })
  it('shows ten cards per page and resets to the first page when filtering', async () => {
    vi.mocked(listingsApi.mine).mockResolvedValue(Array.from({ length: 23 }, (_, i) => ({ ...base, id: i + 1, buildingName: `매물 ${i + 1}`, status: 'READY_FOR_ANALYSIS' })))
    open()
    expect(await screen.findByText('매물 10')).toBeTruthy()
    expect(screen.queryByText('매물 11')).toBeNull()
    expect(screen.getByRole('button', { name: '이전' }).hasAttribute('disabled')).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: '다음' }))
    expect(screen.getByText('매물 11')).toBeTruthy()
    expect(screen.getByText('매물 20')).toBeTruthy()
    expect(screen.queryByText('매물 10')).toBeNull()
    fireEvent.click(screen.getByRole('button', { name: '3페이지' }))
    expect(screen.getByText('매물 23')).toBeTruthy()
    expect(screen.getByRole('button', { name: '다음' }).hasAttribute('disabled')).toBe(true)
    fireEvent.click(screen.getByRole('button', { name: '안전성 분석 대기 23개' }))
    expect(screen.getByText('매물 1')).toBeTruthy()
    expect(screen.queryByText('매물 23')).toBeNull()
    expect(screen.getByRole('button', { name: '1페이지' }).getAttribute('aria-current')).toBe('page')
  })
  it('shows an empty filter message and returns to all listings', async () => {
    vi.mocked(listingsApi.mine).mockResolvedValue([{ ...base, id: 1, buildingName: '대기 매물', status: 'READY_FOR_ANALYSIS' }])
    open()
    await screen.findByText('대기 매물')
    fireEvent.click(screen.getByRole('button', { name: '분석 완료 0개' }))
    expect(screen.getByText('분석이 완료된 매물이 없습니다')).toBeTruthy()
    fireEvent.click(screen.getByRole('button', { name: '전체 매물 보기' }))
    expect(screen.getByText('대기 매물')).toBeTruthy()
  })
  it.each(['READY_FOR_ANALYSIS', 'REGISTRY_PROCESSING', 'REGISTRY_FAILED', 'REGISTRY_READY'] as const)(
    'counts an unfinished %s listing alongside three completed listings', async (status) => {
      vi.mocked(listingsApi.mine).mockResolvedValue([
        { ...base, id: 1, status },
        ...[2, 3, 4].map(id => ({ ...base, id, status: 'SAFETY_ANALYZED' as const })),
        { ...base, id: 5, registeredByUserId: 8, status: 'READY_FOR_ANALYSIS' },
      ])
      open()
      await screen.findAllByText('위험도 보통 · 42점')
      const summary = within(screen.getByLabelText('매물 현황'))
      expect(summary.getByText('등록한 매물').parentElement?.querySelector('strong')?.textContent).toBe('4')
      expect(summary.getByText('안전성 분석 대기').parentElement?.querySelector('strong')?.textContent).toBe('1')
      expect(summary.getByText('분석 완료').parentElement?.querySelector('strong')?.textContent).toBe('3')
    },
  )
  it('shows the stored risk and score only for the owner’s completed listing', async () => {
    open()
    expect(await screen.findByText('위험도 보통 · 42점')).toBeTruthy()
    expect(screen.getByText('분석 결과 보기 →')).toBeTruthy()
    expect(listingsApi.safetyResult).toHaveBeenCalledTimes(1)
    expect(listingsApi.safetyResult).toHaveBeenCalledWith(2)
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
  it('shows an error without inventing a risk result when retrieval fails', async () => {
    vi.mocked(listingsApi.safetyResult).mockRejectedValue(new Error('서버 오류'))
    open()
    expect(await screen.findByText('분석 결과 조회 실패')).toBeTruthy()
    expect(screen.queryByText(/위험도 .*점/)).toBeNull()
  })
  it('marks outdated results and reloads the badge when refreshed', async () => {
    vi.mocked(listingsApi.safetyResult).mockResolvedValue({ ...result, reanalysisAvailable: true, reanalysisReason: 'POLICY_VERSION_CHANGED' })
    open()
    expect(await screen.findByText('이전 결과 · 재분석 필요')).toBeTruthy()
    vi.mocked(listingsApi.safetyResult).mockResolvedValue({ ...result, analysis: { ...result.analysis, riskLevel: 'HIGH', riskScore: 78 } })
    fireEvent.click(screen.getByRole('button', { name: '새로고침' }))
    await waitFor(() => expect(screen.getByText('위험도 높음 · 78점')).toBeTruthy())
    expect(screen.queryByText('이전 결과 · 재분석 필요')).toBeNull()
    expect(listingsApi.analyze).not.toHaveBeenCalled()
  })
})
