import { useQuery, useQueryClient } from '@tanstack/react-query'
import { Building2, Plus, RefreshCw } from 'lucide-react'
import { Link, useSearchParams } from 'react-router-dom'
import { Button, ErrorNotice, PageHeading } from '../../components/ui'
import { useAuth } from '../auth/AuthProvider'
import { documentStatusLabel, listingStatusLabel, ownsListing, transactionLabel, won } from '../../lib/format'
import { listingsApi } from './api'
import { ListingSafetyBadge } from './ListingSafetyBadge'

const PAGE_SIZE = 10
type ListingFilter = 'all' | 'pending' | 'completed'

export function ListingsPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const { user } = useAuth(); const queryClient = useQueryClient()
  const listings = useQuery({ queryKey: ['listings', 'mine'], queryFn: listingsApi.mine })
  const documents = useQuery({ queryKey: ['registry-documents', 'mine'], queryFn: listingsApi.documents })
  const items = (listings.data || []).filter((item) => ownsListing(item, user))
  const filter: ListingFilter = searchParams.get('filter') === 'completed' ? 'completed' : searchParams.get('filter') === 'pending' ? 'pending' : 'all'
  const filters = [
    { key: 'all' as const, label: '등록한 매물', count: items.length },
    { key: 'pending' as const, label: '안전성 분석 대기', count: items.filter(item => item.status !== 'SAFETY_ANALYZED').length },
    { key: 'completed' as const, label: '분석 완료', count: items.filter(item => item.status === 'SAFETY_ANALYZED').length },
  ]
  const filtered = items.filter(item => filter === 'all' || (filter === 'completed' ? item.status === 'SAFETY_ANALYZED' : item.status !== 'SAFETY_ANALYZED'))
  const totalPages = Math.max(1, Math.ceil(filtered.length / PAGE_SIZE))
  const requestedPage = Number(searchParams.get('page') || 1)
  const page = Math.min(Number.isSafeInteger(requestedPage) && requestedPage > 0 ? requestedPage : 1, totalPages)
  const visibleItems = filtered.slice((page - 1) * PAGE_SIZE, page * PAGE_SIZE)
  const pageStart = Math.max(1, Math.min(page - 2, totalPages - 4))
  const pageNumbers = Array.from({ length: Math.min(5, totalPages) }, (_, index) => pageStart + index)
  const changeFilter = (value: ListingFilter) => {
    const next = new URLSearchParams(searchParams)
    if (value === 'all') next.delete('filter'); else next.set('filter', value)
    next.delete('page')
    setSearchParams(next)
  }
  const changePage = (value: number) => {
    const next = new URLSearchParams(searchParams)
    if (value === 1) next.delete('page'); else next.set('page', String(value))
    setSearchParams(next)
  }
  const refresh = () => { void listings.refetch(); void documents.refetch(); void queryClient.invalidateQueries({ queryKey: ['safety-result'] }) }
  return <>
    <PageHeading eyebrow="MY SPACE" title="내 매물" description="등록한 집의 등기사항증명서 처리와 안전성 분석을 확인하세요." action={<Link to="/listings/new"><Button><Plus size={17} /> 새 매물 등록</Button></Link>} />
    <section className="mb-10 grid grid-cols-3 gap-2 sm:gap-4" aria-label="매물 현황">{filters.map(({ key, label, count }) => <button key={key} type="button" aria-pressed={filter === key} aria-label={`${label} ${count}개`} onClick={() => changeFilter(key)} className={`rounded-xl border px-3 py-5 text-left transition focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 sm:px-7 ${filter === key ? 'border-brand-500 bg-brand-50 shadow-sm' : 'border-slate-200 bg-white hover:border-brand-500/40 hover:bg-brand-50/50'}`}><strong className="block text-2xl text-brand-700 sm:inline sm:text-3xl">{count}</strong><span className="text-[10px] text-slate-500 sm:ml-3 sm:text-xs">{label}</span></button>)}</section>
    <section><div className="mb-4 flex items-center justify-between"><div><h2 className="text-xl font-extrabold">{filters.find(entry => entry.key === filter)?.label}</h2><p className="mt-1 text-xs text-slate-500" aria-live="polite">{filtered.length ? `전체 ${filtered.length}개 중 ${(page - 1) * PAGE_SIZE + 1}–${Math.min(page * PAGE_SIZE, filtered.length)}개` : '0개'} · 페이지당 {PAGE_SIZE}개</p></div><button onClick={refresh} className="flex items-center gap-1 text-xs font-bold text-brand-700"><RefreshCw size={14} /> 새로고침</button></div><ErrorNotice error={listings.error || documents.error} />
      {listings.isPending || documents.isPending ? <div className="py-20 text-center text-sm text-slate-500">매물 목록을 불러오는 중입니다…</div> : filtered.length ? <div className="grid gap-4 md:grid-cols-2">{visibleItems.map((item) => { const document = documents.data?.find((entry) => entry.listingId === item.id); return <Link key={item.id} to={`/listings/${item.id}${searchParams.size ? `?${searchParams.toString()}` : ''}`} className="rounded-2xl border border-slate-200 bg-white p-6 transition hover:-translate-y-0.5 hover:border-brand-500/40 hover:shadow-soft"><div className="flex items-center justify-between gap-3"><div className="flex flex-wrap items-center gap-2"><span className="rounded-md bg-brand-50 px-2.5 py-1 text-[11px] font-bold text-brand-700">{transactionLabel[item.transactionType]}</span>{item.status === 'SAFETY_ANALYZED' && <ListingSafetyBadge listingId={item.id} />}</div><span className="shrink-0 text-[11px] text-slate-500">● {listingStatusLabel[item.status]}</span></div><h3 className="mt-6 text-lg font-extrabold">{item.buildingName || item.address}</h3><p className="mt-2 text-xs text-slate-500">{item.address}{item.detailAddress && ` · ${item.detailAddress}`}</p><div className="mt-6 flex items-center justify-between border-t border-slate-100 pt-5"><strong className="text-sm">{won(item.transactionType === 'SALE' ? item.salePrice : item.depositAmount)}{item.transactionType === 'MONTHLY_RENT' && ` / ${won(item.monthlyRent)}`}</strong><span className="text-xs font-semibold text-brand-600">{item.status === 'SAFETY_ANALYZED' ? '분석 결과 보기' : document ? documentStatusLabel[document.status] : '문서 확인 중'} →</span></div></Link>})}</div> : items.length > 0 ? <div className="rounded-2xl border border-slate-200 bg-white px-5 py-16 text-center"><h3 className="text-lg font-extrabold">{filter === 'completed' ? '분석이 완료된 매물이 없습니다' : '분석 대기 중인 매물이 없습니다'}</h3><Button variant="secondary" className="mt-5" onClick={() => changeFilter('all')}>전체 매물 보기</Button></div> : <div className="rounded-2xl border border-slate-200 bg-white px-5 py-16 text-center"><span className="mx-auto grid size-16 place-items-center rounded-2xl bg-brand-50 text-brand-600"><Building2 size={31} /></span><h3 className="mt-5 text-lg font-extrabold">아직 등록한 매물이 없습니다</h3><p className="mt-2 text-sm text-slate-500">매물 정보와 등기사항증명서 PDF를 등록하면 분석을 시작할 수 있습니다.</p><Link className="mt-6 inline-block" to="/listings/new"><Button>첫 매물 등록하기</Button></Link></div>}
      {!listings.isPending && !documents.isPending && totalPages > 1 && <nav aria-label="매물 목록 페이지" className="mt-7 flex flex-wrap items-center justify-center gap-2"><Button variant="secondary" disabled={page === 1} onClick={() => changePage(page - 1)}>이전</Button>{pageNumbers.map(number => <button key={number} type="button" aria-label={`${number}페이지`} aria-current={number === page ? 'page' : undefined} onClick={() => changePage(number)} className={`grid size-11 place-items-center rounded-lg text-sm font-bold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand-500 ${number === page ? 'bg-brand-600 text-white' : 'border border-slate-200 bg-white text-slate-600 hover:bg-brand-50'}`}>{number}</button>)}<Button variant="secondary" disabled={page === totalPages} onClick={() => changePage(page + 1)}>다음</Button><span className="w-full text-center text-xs text-slate-500">{page} / {totalPages} 페이지</span></nav>}
    </section>
  </>
}
