import type { RiskFactor } from '../types/api'

const labels: Record<string, string> = {
  MORTGAGE: '근저당권', SEIZURE: '압류', PROVISIONAL_SEIZURE: '가압류', LEASEHOLD: '전세권',
  PRIORITY_RIGHT: '선순위 권리', OWNERSHIP_MISMATCH: '소유자 정보 불일치', CO_OWNER_ISSUE: '공동소유 확인 필요',
  OTHER: '기타 권리', ACTIVE: '현재 유효', INACTIVE: '말소·해지',
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
  return text.replace(/(-?\d[\d,]*(?:\.\d+)?)\s*(원|KRW)/g, (_, amount: string) => `${groupedNumber(amount)}원`)
    .replace(/\b(?:PROVISIONAL_SEIZURE|OWNERSHIP_MISMATCH|CO_OWNER_ISSUE|PRIORITY_RIGHT|MORTGAGE|SEIZURE|LEASEHOLD|INACTIVE|ACTIVE|OTHER)\b/g, code => labels[code])
}

export function presentRiskFactor(factor: RiskFactor) {
  const value = factor.value?.trim()
  const title = factor.factorType === 'SEIZURE' && value === 'PROVISIONAL_SEIZURE' ? '가압류' : labels[factor.factorType] || '기타 위험요소'
  const info: Record<string, { explanation: string; valueLabel: string }> = {
    MORTGAGE: { explanation: '현재 유효한 근저당권이 등기부에서 확인되었습니다. 표시 금액은 등기부 판독값이며 실제 남은 채무액과 다를 수 있습니다.', valueLabel: '등기 금액' },
    SEIZURE: { explanation: '현재 유효한 압류 또는 가압류가 등기부에서 확인되었습니다. 권리 종류와 판독된 권리자를 아래 설명에서 확인할 수 있습니다.', valueLabel: '권리 종류' },
    PROVISIONAL_SEIZURE: { explanation: '현재 유효한 가압류가 등기부에서 확인되었습니다.', valueLabel: '판독 정보' },
    PRIORITY_RIGHT: { explanation: '계약 기준일보다 먼저 등기된 유효한 권리를 확인한 결과입니다. 표시 순위는 등기부에서 판독한 순위입니다.', valueLabel: '등기 순위' },
    CO_OWNER_ISSUE: { explanation: '공동소유 관련 확인이 필요한 항목입니다. 소유지분 합계만으로 공동소유자 전원의 동의 여부를 확인할 수는 없습니다.', valueLabel: '판독된 소유지분 합계' },
    OWNERSHIP_MISMATCH: { explanation: '등기상 소유자와 비교 대상 정보가 일치하는지 추가 확인이 필요한 항목입니다.', valueLabel: '판독 정보' },
  }
  const meta = info[factor.factorType] || { explanation: '등기부 판독 결과에서 추가 확인이 필요한 항목입니다.', valueLabel: '판독 정보' }
  let valueText = value ? formatRiskText(value) : '확인된 값 없음'
  if (value && /^-?\d+(?:\.\d+)?$/.test(value)) {
    const unit = factor.factorType === 'MORTGAGE' ? '원' : factor.factorType === 'CO_OWNER_ISSUE' ? '%' : factor.factorType === 'PRIORITY_RIGHT' ? '순위' : ''
    valueText = `${groupedNumber(value)}${unit}`
  } else if (value && labels[value]) valueText = labels[value]
  else if (value && /^[A-Z][A-Z0-9_]*$/.test(value)) valueText = '추가 확인 필요'
  return { title, ...meta, valueText }
}
