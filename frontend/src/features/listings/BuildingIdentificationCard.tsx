import { panel } from '../../components/ui'
import type { BuildingIdentification } from '../../types/api'

const executionText: Record<BuildingIdentification['executionStatus'], string> = {
  NOT_REQUESTED: '등기부 판독 후 건축물 식별 상태를 확인할 수 있습니다.',
  PENDING: '건축물대장 조회를 기다리고 있습니다.',
  RUNNING: '건축물대장을 조회하고 있습니다.',
  COMPLETED: '건축물대장 조회가 완료되었습니다.',
  INPUT_INCOMPLETE: '식별에 필요한 주소·동·층·호 정보가 부족합니다.',
  INPUT_CONFLICT: '매물과 등기부의 주소·동·층·호 정보가 충돌합니다.',
  CALL_FAILED: '건축물대장 조회에 실패했습니다. 현재 식별 결과가 없습니다.',
  DISABLED: '건축물대장 조회 기능이 비활성화되어 있습니다.',
  SUPERSEDED: '주소 또는 등기부 변경으로 이전 조회가 오래된 상태입니다.',
}
const resolutionText: Record<NonNullable<BuildingIdentification['resolutionStatus']>, string> = {
  MATCHED: '건축물대장에서 일치하는 호를 확인했습니다.',
  NOT_FOUND: '전체 조회에서 일치하는 호를 찾지 못했습니다.',
  AMBIGUOUS: '일치 가능성이 있는 호가 여러 건입니다. 추가 확인이 필요합니다.',
  UNVERIFIABLE: '건축물대장 자료로는 해당 호를 확정할 수 없습니다.',
}

export function BuildingIdentificationCard({ result }: { result: BuildingIdentification }) {
  return <section className={panel} role="region" aria-label="건축물 식별 상태">
    <h2 className="text-xl font-extrabold">건축물 식별</h2>
    <p className="mt-3 text-sm text-slate-700">{result.executionStatus === 'COMPLETED' && result.resolutionStatus
      ? resolutionText[result.resolutionStatus] : executionText[result.executionStatus]}</p>
    {result.executionStatus === 'COMPLETED' && result.resolutionStatus === 'MATCHED' && result.managementNumber &&
      <p className="mt-2 text-xs text-slate-500">건축물대장 관리번호 {result.managementNumber}{result.unitName ? ` · ${result.unitName}` : ''}</p>}
    <p className="mt-3 text-xs text-slate-500">건축물 식별은 등기부 위험 점수·등급과 별도로 표시됩니다.</p>
  </section>
}
