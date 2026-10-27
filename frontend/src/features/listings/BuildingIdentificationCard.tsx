import { useState } from 'react'
import { Button, ErrorNotice, input, panel } from '../../components/ui'
import type { AddressSearchEvidence, BuildingIdentification, BuildingLookupInput, Listing } from '../../types/api'

const executionText: Record<BuildingIdentification['executionStatus'], string> = {
  NOT_REQUESTED: '매물주소와 상세주소로 건축물대장을 조회할 수 있습니다. 문서 분석 없이 조회 가능합니다.',
  PENDING: '건축물대장 조회를 기다리고 있습니다.',
  RUNNING: '건축물대장을 조회하고 있습니다.',
  COMPLETED: '건축물대장 조회가 완료되었습니다.',
  INPUT_INCOMPLETE: '조회에 필요한 정보가 부족합니다. 조회 정보 보완에서 주소검색과 지상·지하·층·호를 확인해 주세요.',
  INPUT_CONFLICT: '조회 주소 또는 동·층·호 표기가 충돌합니다. 조회 정보 보완에서 확인해 주세요.',
  CALL_FAILED: '건축물대장 조회에 실패했습니다. 현재 식별 결과가 없습니다.',
  DISABLED: '공공 API 조회를 아직 실행하지 않았습니다. 아래 버튼으로 조회할 수 있습니다.',
  SUPERSEDED: '주소 또는 등기사항증명서 변경으로 이전 조회가 오래된 상태입니다.',
}
const resolutionText: Record<NonNullable<BuildingIdentification['resolutionStatus']>, string> = {
  MATCHED: '건축물대장에서 일치하는 호를 확인했습니다.',
  NOT_FOUND: '전체 조회에서 일치하는 호를 찾지 못했습니다.',
  AMBIGUOUS: '일치 가능성이 있는 호가 여러 건입니다. 추가 확인이 필요합니다.',
  UNVERIFIABLE: '건축물대장 자료로는 해당 호를 확정할 수 없습니다.',
}

export function BuildingIdentificationCard({ result, onRetry, retrying, retryError, listing, documentAnalyzed = false }: {
  listing?: Listing; documentAnalyzed?: boolean; result: BuildingIdentification; onRetry?: (input?: BuildingLookupInput) => void; retrying?: boolean; retryError?: unknown;
}) {
  const [evidence, setEvidence] = useState<AddressSearchEvidence>()
  const [detail, setDetail] = useState(listing?.detailAddress || '')
  const [addressError, setAddressError] = useState<string>()
  const searchAddress = () => {
    if (!window.kakao?.Postcode) { setAddressError('주소검색을 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.'); return }
    new window.kakao.Postcode({ oncomplete: data => {
      const selectedAddress = data.userSelectedType === 'R' ? data.roadAddress : data.jibunAddress
      if (selectedAddress !== listing?.address || data.noSelected !== 'N' || !data.jibunAddress
          || !/^\d{10}$/.test(data.bcode) || !/^\d{5}$/.test(data.sigunguCode) || !data.bcode.startsWith(data.sigunguCode)) {
        setAddressError('등록된 매물주소와 동일한 주소를 선택해 주세요. 도로명·지번 선택도 확인해 주세요.'); return
      }
      setEvidence({ selectedType: data.userSelectedType, selectedAddress, jibunAddress: data.jibunAddress,
        bcode: data.bcode, sigunguCode: data.sigunguCode, noSelected: data.noSelected })
      setAddressError(undefined)
    } }).open()
  }
  const working = Boolean(retrying) || ['PENDING', 'RUNNING'].includes(result.executionStatus)
  const waiting = result.executionStatus === 'PENDING' && !retrying
  const completed = result.executionStatus === 'COMPLETED' && !working
  const needsAddressEvidence = result.executionStatus === 'INPUT_INCOMPLETE'
    && result.reason === 'CONFIRMED_JIBUN_REQUIRED'
  const missingFloor = completed && result.resolutionStatus === 'UNVERIFIABLE'
    && result.reason === 'CANDIDATE_FLOOR_NUMBER_UNAVAILABLE'
  const malformedResponse = result.executionStatus === 'CALL_FAILED'
    && /^(TITLE|EXPOS|BASIS)_PAGE_[1-9]\d{0,2}_MALFORMED_RESPONSE_XML$/.test(result.reason || '')
  const emptyResponse = result.executionStatus === 'CALL_FAILED'
    && /^(TITLE|EXPOS|BASIS)_PAGE_[1-9]\d{0,2}_EMPTY_RESPONSE$/.test(result.reason || '')
  const inferredFloor = completed && result.resolutionStatus === 'MATCHED'
    && result.reason === 'FLOOR_INFERRED_FROM_UNIT_NUMBER'
  const inferenceConflict = completed && result.reason === 'FLOOR_INFERENCE_CONFLICT'
  const canRetry = !working
  return <section className={panel} role="region" aria-label="건축물대장 분석 상태">
    <h2 className="text-xl font-extrabold">건축물대장 공공 API 조회</h2><p className="mt-2 text-xs leading-5 text-slate-500">조회 요청의 주소·동·층·호를 기준으로 한 공공 API 식별 결과입니다. 첨부한 대장 파일의 분석 결과와 구분해 확인합니다.</p>
    {documentAnalyzed && <p className="mt-3 rounded-lg bg-brand-50 p-3 text-sm text-brand-800">건축물대장 문서 분석이 완료되어 추가 조회는 선택사항입니다.</p>}
    {completed && <p className="mt-3 text-sm font-bold text-slate-700">건축물대장 조회 완료</p>}
    {!inferredFloor && <p className="mt-3 text-sm text-slate-700">{working
      ? waiting ? executionText.PENDING : executionText.RUNNING : completed && result.resolutionStatus
      ? resolutionText[result.resolutionStatus] : executionText[result.executionStatus]}</p>}
    {working && <div className="mt-4 rounded-xl border border-brand-500/20 bg-brand-50/50 p-4" role="status" aria-live="polite" aria-busy="true">
      <p className="text-sm font-bold text-brand-800">{waiting ? '건축물 식별 조회 대기' : '건축물 식별 진행 중'}</p>
      <div className="mt-3 h-2.5 overflow-hidden rounded-full bg-brand-100" role="progressbar" aria-label="건축물 식별 진행 상황" aria-valuetext={waiting ? '조회 대기 중' : '건축물대장 조회 및 동·층·호 비교 중'}>
        <div className="registry-progress-bar h-full w-2/5 rounded-full bg-gradient-to-r from-brand-500 via-brand-600 to-brand-500" />
      </div>
      <p className="mt-3 text-xs leading-5 text-slate-600">{waiting ? '조회가 시작되면 진행 상태가 자동으로 갱신됩니다.' : '건축물대장을 조회하고 동·층·호를 비교하고 있습니다. 완료되면 결과가 자동으로 표시됩니다.'}</p>
    </div>}
    {needsAddressEvidence && <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
      주소검색으로 확정한 지번·법정동 정보가 없어 건축물대장 조회를 시작하지 못했습니다. 주소를 직접 입력한 경우 주소검색으로 해당 정보를 보완해야 합니다. 등기사항증명서 분석과 안전성 분석 결과는 유지됩니다.
    </p>}
    {inferredFloor && <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
      호수로 추정한 층을 적용해 일치하는 호를 확인했습니다.
    </p>}
    {inferenceConflict && <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
      호수로 추정한 층과 등록·등기사항증명서의 층 정보가 달라 같은 호인지 확정할 수 없습니다. 추정만으로 다른 호라고 판정하지 않습니다.
    </p>}
    {missingFloor && <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
      공급기관 층 정보 부족으로 확인 불가: 건축물대장에 유효한 층번호가 없습니다. 지상·지하 구분과 호수 형식을 확인해 추정 가능한 경우 층을 추정하여 다시 검증할 수 있습니다.
    </p>}
    {emptyResponse && !working && <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
      공급기관 응답 내용이 비어 있어 조회를 완료하지 못했습니다. 자동 재시도 후에도 응답을 받지 못했습니다. 잠시 후 다시 시도해 주세요. 등기사항증명서 분석과 안전성 분석 결과는 유지됩니다.
    </p>}
    {malformedResponse && !working && <p className="mt-2 rounded-lg bg-amber-50 p-3 text-sm text-amber-900">
      건축물대장 응답 형식을 해석하지 못해 조회를 완료하지 못했습니다. 등기사항증명서 분석과 안전성 분석 결과는 유지됩니다.
    </p>}
    {result.executionStatus === 'COMPLETED' && result.resolutionStatus === 'MATCHED' && result.managementNumber &&
      <p className="mt-2 text-xs text-slate-500">건축물대장 관리번호 {result.managementNumber}{result.unitName ? ` · ${result.unitName}` : ''}</p>}
    {onRetry && <div className="mt-4">
      {listing && <details open={!listing.addressSearchEvidence} className="mb-4 rounded-lg border border-slate-200 p-3 text-xs">
        <summary className="cursor-pointer font-bold">조회 정보 보완 (주소검색·층·호)</summary>
        <p className="mt-2 text-slate-500">문서 분석은 필요하지 않습니다. 조회용 정보만 보완하며 매물정보는 변경하지 않습니다. 층은 호수에서 임의로 추정하지 않습니다.</p>
        <label className="mt-3 block">조회 주소<input className={input} value={listing.address} readOnly /></label>
        <Button className="mt-2" variant="secondary" disabled={working} onClick={searchAddress}>조회 주소검색</Button>
        <p className="mt-2 text-slate-500">{evidence || listing.addressSearchEvidence ? '주소검색 근거 확인됨' : '주소검색에서 등록된 매물주소와 동일한 주소를 선택해 주세요.'}</p>
        <label className="mt-3 block">조회용 상세주소 (동·층·호)<input className={input} maxLength={500} value={detail} disabled={working} placeholder="예: 101동 지상 2층 202호" onChange={e => setDetail(e.target.value)} /></label>
        <p className="mt-2 text-slate-500">지상·지하와 층·호수를 명시해 주세요. 주소검색 근거는 법정동·지번 조회에 필요합니다.</p>
        <ErrorNotice error={addressError ? new Error(addressError) : null} />
      </details>}
      <Button variant="secondary" busy={retrying} disabled={!canRetry} onClick={() => onRetry(evidence || detail !== (listing?.detailAddress || '') ? { addressSearchEvidence: evidence, detailAddress: detail } : undefined)}>건축물대장 조회</Button>
      <p className="mt-2 text-xs text-slate-500">건축물대장만 다시 조회합니다. 등기사항증명서 재분석이나 안전성 재분석은 실행하지 않습니다.</p>
    </div>}
    <ErrorNotice error={retryError} />
    <p className="mt-3 text-xs text-slate-500">건축물 식별은 등기사항증명서 위험 점수·등급과 별도로 표시됩니다.</p>
  </section>
}
