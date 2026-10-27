import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, ArrowLeft, Check, Search } from 'lucide-react'
import { Link, useNavigate, useParams, useLocation } from 'react-router-dom'
import { Button, ErrorNotice, Field, PageHeading, input, panel } from '../../components/ui'
import { formatMoney, parseMoney } from '../../lib/inputFormat'
import { useAuth } from '../auth/AuthProvider'
import { ownsListing } from '../../lib/format'
import type { AddressSearchEvidence, Listing, ListingMetadata, TransactionType } from '../../types/api'
import { listingsApi } from './api'

type PostcodeData = { address: string; userSelectedType: 'R' | 'J'; roadAddress: string;
  jibunAddress: string; bcode: string; sigunguCode: string; noSelected: 'Y' | 'N' }
type KakaoPostcode = new (options: { oncomplete: (data: PostcodeData) => void }) => { open: () => void }
declare global { interface Window { kakao?: { Postcode: KakaoPostcode } } }

export function RegisterListingPage() {
  const { id } = useParams(); const { user } = useAuth()
  const existing = useQuery({ queryKey: ['listing', Number(id)], queryFn: () => listingsApi.one(Number(id)), enabled: Boolean(id) })
  if (id && existing.isPending) return <p>매물 정보를 불러오는 중입니다…</p>
  if (id && (existing.error || !existing.data)) return <ErrorNotice error={existing.error || new Error('매물을 찾을 수 없습니다.')} />
  if (id && existing.data && (!ownsListing(existing.data, user) || existing.data.status === 'REGISTRY_PROCESSING')) return <p>본인이 등록한 매물만 수정할 수 있으며, 분석 중에는 완료 후 수정해 주세요.</p>
  return <ListingForm key={id || 'new'} initial={id ? existing.data : undefined} />
}

function ListingForm({ initial }: { initial?: Listing }) {
  const [transactionType, setTransactionType] = useState<TransactionType>(initial?.transactionType || 'JEONSE')
  const [address, setAddress] = useState(initial?.address || '')
  const [addressSearchEvidence, setAddressSearchEvidence] = useState<AddressSearchEvidence | null>(initial?.addressSearchEvidence || null)
  const [addressError, setAddressError] = useState<string>()
  const [amounts, setAmounts] = useState({ depositAmount: initial?.depositAmount ? formatMoney(String(initial.depositAmount)) : '', monthlyRent: initial?.monthlyRent ? formatMoney(String(initial.monthlyRent)) : '', salePrice: initial?.salePrice ? formatMoney(String(initial.salePrice)) : '' })
  const location = useLocation(); const listUrl = `/listings${location.search}`
  const navigate = useNavigate(); const queryClient = useQueryClient()
  const registration = useMutation({ mutationFn: (metadata: ListingMetadata) => (initial ? listingsApi.update(initial.id, metadata) : listingsApi.create(metadata)), onSuccess: async (listing) => { await queryClient.invalidateQueries({ queryKey: ['listings'] }); await queryClient.invalidateQueries({ queryKey: ['listing'] }); navigate(`/listings/${listing.id}${location.search}`) } })

  const searchAddress = () => {
    if (!window.kakao?.Postcode) { setAddressError('주소검색 서비스를 불러오지 못했습니다. 잠시 후 주소검색을 다시 시도해 주세요.'); return }
    setAddressError(undefined)
    new window.kakao.Postcode({
      oncomplete: (data) => {
        const selectedAddress = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress
        if (!selectedAddress || data.noSelected !== 'N' || !data.jibunAddress
          || !/^\d{10}$/.test(data.bcode) || !/^\d{5}$/.test(data.sigunguCode) || !data.bcode.startsWith(data.sigunguCode)) {
          setAddressError('선택한 주소를 확인할 수 없습니다. 다른 검색 결과를 선택해 주세요.')
          return
        }
        setAddress(selectedAddress)
        setAddressSearchEvidence({ selectedType: data.userSelectedType, selectedAddress,
          jibunAddress: data.jibunAddress || null, bcode: data.bcode,
          sigunguCode: data.sigunguCode, noSelected: data.noSelected })
        setAddressError('')
      },
    }).open()
  }

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); const values = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>
    if (registration.isPending) return
    if (!initial && (!addressSearchEvidence || addressSearchEvidence.noSelected !== 'N' || !addressSearchEvidence.jibunAddress)) {
      setAddressError('주소검색에서 매물주소를 선택해 주세요. 지번·법정동 정보가 확인된 주소만 등록할 수 있습니다.')
      return
    }
    if (!address.trim()) { setAddressError('주소검색에서 등기사항증명서와 동일한 주소를 선택해 주세요.'); return }
    const text = (name: string) => String(values[name] || '').trim() || null
    registration.mutate({ address: address.trim(), addressSearchEvidence, buildingName: initial ? initial.buildingName ?? null : text('buildingName'), buildingType: initial ? initial.buildingType : String(values.buildingType), detailAddress: initial ? initial.detailAddress ?? null : text('detailAddress'), transactionType, depositAmount: transactionType === 'SALE' ? null : parseMoney(amounts.depositAmount), monthlyRent: transactionType === 'MONTHLY_RENT' ? parseMoney(amounts.monthlyRent) : null, salePrice: transactionType === 'SALE' ? parseMoney(amounts.salePrice) : null })
  }

  const money = (name: keyof typeof amounts, label: string) => <Field label={label} required><input className={input} name={name} type="text" inputMode="numeric" value={amounts[name]} onChange={(event) => setAmounts((current) => ({ ...current, [name]: formatMoney(event.target.value) }))} maxLength={19} required placeholder="원 단위" /></Field>

  return <>
    <PageHeading eyebrow="NEW LISTING" title={initial ? "매물 수정" : "매물 등록"} description={initial ? "거래 유형과 금액 등 거래조건만 수정할 수 있습니다. 매물 기본정보는 변경할 수 없습니다." : "기본 주소와 상세주소를 합쳐 등기사항증명서와 비교합니다."} back={<Link to={listUrl} className="mb-5 flex items-center gap-1 text-xs text-slate-500 hover:text-brand-700"><ArrowLeft size={14} /> 내 매물</Link>} />
    <form onSubmit={submit} className="grid gap-5 lg:grid-cols-[minmax(0,1fr)_290px]"><div className="space-y-5"><FormSection step="01" title="매물 정보" description={initial ? "주소·상세주소·건물명·건물 유형은 변경할 수 없습니다." : "주소와 등기사항증명서 PDF의 부동산이 같은지 분석 과정에서 확인합니다."}><fieldset disabled={Boolean(initial)} aria-label="매물 기본정보" className="disabled:opacity-70"><div className="grid gap-5 sm:grid-cols-2"><Field className="sm:col-span-2" label="매물주소" required><div className="mb-3 flex items-start gap-3 rounded-xl border border-amber-300 bg-amber-50 px-4 py-3 text-amber-950"><AlertTriangle size={20} className="mt-0.5 shrink-0 text-amber-600" /><div><strong className="block text-sm">등기사항증명서에 기재된 주소와 동일한 주소를 선택해 주세요.</strong><span className="mt-1 block text-xs font-normal leading-5 text-amber-800">지번 또는 도로명 주소를 선택하고, 나머지 건물명·동·층·호수는 상세주소에 입력해 주세요.</span></div></div><div className="flex gap-2"><input className={`${input} mt-0`} name="address" aria-label="매물주소" value={address} readOnly aria-readonly="true" maxLength={500} required placeholder="주소검색 버튼으로 주소를 선택해 주세요" /><Button type="button" variant="secondary" className="shrink-0" onClick={searchAddress}><Search size={16} /> 주소검색</Button></div>{addressError && <p className="mt-2 text-[11px] text-red-600">{addressError}</p>}<p className="mt-2 text-[11px] font-normal leading-5 text-slate-500">주소는 직접 입력할 수 없습니다. 주소검색으로 선택해 주세요. 등록할 때 상세주소는 아래에 직접 입력합니다.</p></Field><Field className="sm:col-span-2" label="상세주소"><input className={input} name="detailAddress" aria-label="상세주소" defaultValue={initial?.detailAddress || ''} maxLength={500} placeholder="예: 안전아파트 101동 제2층 제202호" /><p className="mt-2 text-xs font-normal text-slate-500">등기사항증명서에 적힌 상세주소를 입력해 주세요. 상세주소가 없으면 비워두세요.</p></Field><Field label="건물 유형" required><select className={input} name="buildingType" defaultValue={initial?.buildingType || 'APARTMENT'} required><option value="APARTMENT">아파트</option><option value="OFFICETEL">오피스텔</option><option value="VILLA">빌라</option><option value="DETACHED_HOUSE">단독주택</option><option value="MULTI_FAMILY">다가구주택</option><option value="MULTI_UNIT">다세대주택</option><option value="OTHER">기타</option></select></Field><Field label="건물명"><input className={input} name="buildingName" defaultValue={initial?.buildingName || ''} maxLength={200} placeholder="예: 안전아파트" /></Field></div></fieldset></FormSection>
      <FormSection step="02" title="거래 조건" description="금액은 숫자로 입력하면 천 단위 쉼표가 자동으로 표시됩니다."><div className="grid gap-5 sm:grid-cols-2"><Field className="sm:col-span-2" label="거래 유형" required><select className={input} name="transactionType" value={transactionType} onChange={(event) => setTransactionType(event.target.value as TransactionType)}><option value="JEONSE">전세</option><option value="MONTHLY_RENT">월세</option><option value="SALE">매매</option></select></Field>{transactionType !== 'SALE' && money('depositAmount', '보증금')}{transactionType === 'MONTHLY_RENT' && money('monthlyRent', '월세')}{transactionType === 'SALE' && money('salePrice', '매매가')}</div></FormSection>
      <ErrorNotice error={registration.error} /></div>
      <aside><div className={`${panel} sticky top-24`}><span className="text-[11px] font-extrabold tracking-[.2em] text-brand-500">CHECKLIST</span><h3 className="mt-3 font-extrabold">등록 전 확인</h3><ul className="mt-5 space-y-4 text-xs text-slate-600">{['매물주소가 등기사항증명서와 일치하나요?', '거래 금액이 정확한가요?', '건물 유형과 상세주소가 정확한가요?'].map((item) => <li key={item} className="flex gap-2"><Check size={14} className="shrink-0 text-brand-500" />{item}</li>)}</ul><p className="my-5 border-t border-slate-100 pt-5 text-xs leading-6 text-slate-500">등기사항증명서는 매물 등록 후 안전성 분석 화면에서 첨부하고 분석합니다.</p><Button busy={registration.isPending} className="w-full" type="submit">{initial ? '수정 저장' : '매물 등록'}</Button></div></aside>
    </form>
  </>
}

function FormSection({ step, title, description, children }: { step: string; title: string; description: string; children: React.ReactNode }) {
  return <section className={panel}><div className="mb-6 flex items-center gap-4"><span className="grid size-9 place-items-center rounded-lg bg-brand-50 text-xs font-extrabold text-brand-700">{step}</span><div><h2 className="font-extrabold">{title}</h2><p className="mt-1 text-xs text-slate-500">{description}</p></div></div>{children}</section>
}
