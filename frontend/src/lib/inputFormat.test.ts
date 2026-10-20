import { describe, expect, it } from 'vitest'
import { formatMoney, formatPhone, isStrongPassword, isValidEmail, parseMoney } from './inputFormat'

describe('회원가입 입력 형식', () => {
  it('휴대전화번호에서 숫자만 남기고 하이픈을 표시한다', () => {
    expect(formatPhone('010a1234-1234')).toBe('010-1234-1234')
  })

  it('비밀번호 조합을 검사한다', () => {
    expect(isStrongPassword('SafeHome2026!')).toBe(true)
    expect(isStrongPassword('SafeHomeOnly!')).toBe(false)
    expect(isStrongPassword('12345678901!')).toBe(false)
  })

  it('도메인과 점 위치를 포함해 이메일 형식을 검사한다', () => {
    expect(isValidEmail('safe.home+test@example.co.kr')).toBe(true)
    expect(isValidEmail('user@example')).toBe(false)
    expect(isValidEmail('.user@example.com')).toBe(false)
    expect(isValidEmail('user..name@example.com')).toBe(false)
    expect(isValidEmail('user@-example.com')).toBe(false)
  })
})

describe('금액 입력 형식', () => {
  it('숫자만 유지하고 천 단위 쉼표를 표시한다', () => {
    expect(formatMoney('-1234567원')).toBe('1,234,567')
    expect(parseMoney('1,234,567')).toBe(1234567)
  })
})
