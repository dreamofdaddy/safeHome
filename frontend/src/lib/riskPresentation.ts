import type { RiskFactor } from '../types/api'

const labels: Record<string, string> = {
  BUILDING_VIOLATION: '건축물대장 위반 표시', MORTGAGE: '근저당권', SEIZURE: '압류', PROVISIONAL_SEIZURE: '가압류', LEASEHOLD: '전세권',
  PRIORITY_RIGHT: '전세권·선순위 권리 확인', OWNERSHIP_MISMATCH: '소유자 정보 불일치', CO_OWNER_ISSUE: '소유관계·공유지분 확인 필요',
  TRUST: '신탁', AUCTION: '경매', OTHER: '기타 권리', ACTIVE: '현재 유효', INACTIVE: '말소·해지',
}

// Group the original decimal text without rounding monetary amounts or losing integer precision.
export function groupedNumber(value: string) {
  const clean = value.replaceAll(',', '').trim()
  if (!/^-?\d+(?:\.\d+)?$/.test(clean)) return value
  const [integer, decimal] = clean.split('.')
  const fraction = decimal?.replace(/0+$/, '')
  return BigInt(integer).toLocaleString('ko-KR') + (fraction ? `.${fraction}` : '')
}

export function formatRiskText(text: string) {
  return text.replaceAll('판독', '분석').replace(/등기부(?:\s*등본)?/g, '등기사항증명서').replace(/(-?\d[\d,]*(?:\.\d+)?)\s*(원|KRW)/g, (_, amount: string) => `${groupedNumber(amount)}원`)
    .replace(/\b(?:BUILDING_VIOLATION|PROVISIONAL_SEIZURE|OWNERSHIP_MISMATCH|CO_OWNER_ISSUE|PRIORITY_RIGHT|MORTGAGE|SEIZURE|LEASEHOLD|TRUST|AUCTION|INACTIVE|ACTIVE|OTHER)\b/g, code => labels[code])
}

export function presentRiskFactor(factor: RiskFactor) {
  const value = factor.factorType === 'CO_OWNER_ISSUE' ? undefined : factor.value?.trim()
  const title = factor.factorType === 'SEIZURE' && value === 'PROVISIONAL_SEIZURE' ? '가압류' : labels[factor.factorType] || '기타 위험요소'
  const info: Record<string, { explanation: string; valueLabel: string }> = {
    BUILDING_VIOLATION: { explanation: '첨부 건축물대장에서 위반건축물 표시가 확인되었습니다. 위반 대상·내용과 시정 여부를 확인해 주세요. 표제부·총괄표제부는 해당 호실과의 관련성 확인이 필요합니다.', valueLabel: '근거 문서' },
    TRUST: { explanation: '현재 유효한 신탁 등기가 분석되었습니다. 계약 권한과 신탁 관련 조건 확인이 필요한 매우 높은 위험요소입니다.', valueLabel: '권리 종류' },
    AUCTION: { explanation: '현재 유효한 경매 관련 등기가 분석되었습니다. 경매 진행 상태 확인이 필요한 매우 높은 위험요소입니다.', valueLabel: '권리 종류' },
    OTHER: { explanation: '현재 유효한 기타 등기 권리가 분석되었습니다. 표시된 원문 권리명과 내용을 확인해야 하며 안전한 권리로 간주하지 않습니다.', valueLabel: '원문 권리명' },
    MORTGAGE: { explanation: '현재 유효한 근저당권이 등기사항증명서에서 확인되었습니다. 표시 금액은 등기사항증명서 분석값이며 실제 남은 채무액과 다를 수 있습니다.', valueLabel: '등기 금액' },
    SEIZURE: { explanation: '현재 유효한 압류 또는 가압류가 등기사항증명서에서 확인되었습니다. 권리 종류와 분석된 권리자를 아래 설명에서 확인할 수 있습니다.', valueLabel: '권리 종류' },
    PROVISIONAL_SEIZURE: { explanation: '현재 유효한 가압류가 등기사항증명서에서 확인되었습니다.', valueLabel: '분석 정보' },
    PRIORITY_RIGHT: { explanation: '계약 기준일보다 먼저 등기된 유효한 권리를 확인한 결과입니다. 표시 순위는 등기사항증명서에서 분석한 순위입니다.', valueLabel: '등기 순위' },
    CO_OWNER_ISSUE: { explanation: '복수 소유자·공유지분 또는 불완전한 소유관계가 확인된 위험 검토 항목입니다. 해당 매물의 원문 지분, 임대인의 계약 권한과 필요한 동의·위임 서류를 확인해 주세요. 공용부분·대지권의 공유지분만으로 해당 매물의 공동소유를 단정하지 않으며 계약 무효·사기로 확정하지 않습니다.', valueLabel: '원문 소유지분' },
    OWNERSHIP_MISMATCH: { explanation: '등기상 소유자와 비교 대상 정보가 일치하는지 추가 확인이 필요한 항목입니다.', valueLabel: '분석 정보' },
  }
  const meta = info[factor.factorType] || { explanation: '등기사항증명서 분석 결과에서 추가 확인이 필요한 항목입니다.', valueLabel: '분석 정보' }
  let valueText = value ? formatRiskText(value) : '확인된 값 없음'
  if (value && /^-?\d+(?:\.\d+)?$/.test(value)) {
    const unit = factor.factorType === 'MORTGAGE' ? '원' : factor.factorType === 'CO_OWNER_ISSUE' ? '%' : factor.factorType === 'PRIORITY_RIGHT' ? '순위' : ''
    valueText = `${groupedNumber(value)}${unit}`
  } else if (value && labels[value]) valueText = labels[value]
  else if (value && /^[A-Z][A-Z0-9_]*$/.test(value)) valueText = '추가 확인 필요'
  return { title, ...meta, valueText }
}
