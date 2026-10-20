import { describe, expect, it } from 'vitest'
import { formatRiskText, groupedNumber, presentRiskFactor } from './riskPresentation'
import type { RiskFactor } from '../types/api'

describe('risk information presentation', () => {
  it('formats currency without changing dates or losing precision', () => {
    expect(formatRiskText('2026-10-16 MORTGAGE 300000000원, PROVISIONAL_SEIZURE 1200000 KRW')).toBe('2026-10-16 근저당권 300,000,000원, 가압류 1,200,000원')
    expect(groupedNumber('9007199254740993.00')).toBe('9,007,199,254,740,993')
    expect(groupedNumber('1234567.50')).toBe('1,234,567.5')
  })
  it('distinguishes currency, priority and ownership ratios', () => {
    const factor = { factorType: 'MORTGAGE', value: '300000000' } as RiskFactor
    expect(presentRiskFactor(factor).valueText).toBe('300,000,000원')
    expect(presentRiskFactor({ ...factor, factorType: 'PRIORITY_RIGHT', value: '2' }).valueText).toBe('2순위')
    expect(presentRiskFactor({ ...factor, factorType: 'CO_OWNER_ISSUE', value: '100.00' }).valueText).toBe('100%')
    expect(presentRiskFactor({ ...factor, factorType: 'SEIZURE', value: 'PROVISIONAL_SEIZURE' }).title).toBe('가압류')
  })
  it('does not invent missing values or expose unknown codes', () => {
    expect(presentRiskFactor({ factorType: 'MORTGAGE' } as RiskFactor).valueText).toBe('확인된 값 없음')
    expect(presentRiskFactor({ factorType: 'NEW_CODE', value: 'UNKNOWN_CODE' } as RiskFactor)).toMatchObject({ title: '기타 위험요소', valueText: '추가 확인 필요' })
  })
})
