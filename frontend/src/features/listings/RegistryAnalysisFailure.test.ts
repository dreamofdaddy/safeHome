import { describe, expect, it } from 'vitest'
import { registryFailureMessage } from './RegistryAnalysisFailure'
describe('registry failure explanation', () => {
  it('does not present unsupported rights as poor document clarity', () => {
    const message = registryFailureMessage('REGISTRY_RIGHT_UNSUPPORTED')
    expect(message.title).toContain('권리')
    expect(message.description).not.toContain('선명도')
    expect(message.description).toBe('지원하지 않는 권리 유형이나 유효 상태가 추출되어 분석을 완료하지 못했습니다.')
  })
  it('does not infer poor clarity from unknown failures', () => {
    expect(registryFailureMessage('UNKNOWN').description).not.toMatch(/선명|해상도/)
  })
})
