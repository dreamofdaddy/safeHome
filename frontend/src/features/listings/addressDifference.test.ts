import { describe, it, expect } from 'vitest'
import { addressDifference } from './addressDifference'
describe('address difference presentation', () => {
  it('highlights only changed characters and preserves both original strings', () => {
    const diff = addressDifference('서울 테스트로 101호', '서울 테스트로 102호')
    expect(diff.first.filter(p => p.different).map(p => p.text).join('')).toBe('1')
    expect(diff.second.filter(p => p.different).map(p => p.text).join('')).toBe('2')
    expect(diff.first.map(p => p.text).join('')).toBe('서울 테스트로 101호')
    expect(diff.second.map(p => p.text).join('')).toBe('서울 테스트로 102호')
  })
  it('keeps matching text unmarked and handles absent or additional text', () => {
    expect(addressDifference('동일 주소', '동일 주소').first.every(p => !p.different)).toBe(true)
    expect(addressDifference('', '주소').second).toEqual([{ text: '주소', different: true }])
  })
})
