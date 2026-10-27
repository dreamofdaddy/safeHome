import { useEffect, useRef } from 'react'
import type { RegistryProcessing } from '../../types/api'
import { Button } from '../../components/ui'
import { addressDifference } from './addressDifference'

export function registryFailureMessage(code?: string | null) {
  if (code === 'REGISTRY_ADDRESS_MISMATCH') return { title: '등기사항증명서 주소가 일치하지 않습니다', description: '등록된 매물주소와 문서에서 인식한 주소가 다릅니다. 아래 주소를 비교한 뒤 해당 매물의 등기사항증명서 PDF를 다시 선택해 주세요.' }
  if (code === 'REGISTRY_RIGHT_UNSUPPORTED') return { title: '등기 권리의 분류를 확정하지 못했습니다', description: '지원하지 않는 권리 유형이나 유효 상태가 추출되어 분석을 완료하지 못했습니다.' }
  if (code === 'REGISTRY_OWNER_UNCERTAIN') return { title: '소유자 정보를 확정하지 못했습니다', description: '추출한 소유자 유형 또는 식별정보가 검증 기준을 충족하지 못했습니다. 이 오류만으로 문서의 해상도가 부족하다고 판단할 수 없습니다.' }
  if (code === 'PARSER_PROVIDER_ERROR') return { title: 'AI 서비스 요청에 실패했습니다', description: '외부 AI 서비스가 요청을 정상 처리하지 못해 문서 분석 결과를 받지 못했습니다. 문서 해상도 문제로 확인된 것은 아닙니다.' }
  if (code === 'PARSER_TIMEOUT') return { title: '등기사항증명서 분석 시간이 초과되었습니다', description: '잠시 후 PDF를 다시 선택하여 분석해 주세요. 이전 분석 결과는 보존됩니다.' }
  return { title: '등기사항증명서 분석에 실패했습니다', description: '문서 분석을 완료하지 못했습니다. 아래 오류 코드와 서버 진단 기록을 확인해 주세요.' }
}
function FailureContent({ result }: { result: RegistryProcessing }) {
  const message = registryFailureMessage(result.errorCode)
  const evidence = result.addressMismatch
  const addresses = evidence ? [['문서에서 인식한 지번주소', evidence.lotAddress], ['문서에서 인식한 도로명주소', evidence.roadAddress]].filter((entry): entry is [string, string] => Boolean(entry[1])) : []
  return <><p className="text-sm leading-6">{message.description}</p>
    {evidence && addresses.map(([label, address]) => {
      const diff = addressDifference(evidence.registeredAddress, address)
      const render = (parts: typeof diff.first) => parts.map((part, index) => part.different ? <mark key={index} className="rounded bg-orange-200 px-0.5 font-bold text-red-950" title="다른 문자">{part.text}</mark> : <span key={index}>{part.text}</span>)
      return <dl key={label} className="mt-3 space-y-3 rounded-lg border border-red-100 bg-white p-4 text-xs leading-6"><div><dt className="font-bold text-slate-500">등록한 주소</dt><dd className="mt-1 whitespace-pre-wrap break-words text-slate-800">{render(diff.first)}</dd></div><div><dt className="font-bold text-slate-500">{label}</dt><dd className="mt-1 whitespace-pre-wrap break-words text-slate-800">{render(diff.second)}</dd></div></dl>
    })}
    {evidence && <p className="mt-2 text-xs leading-5">주황색은 두 주소의 문자 표기가 다른 부분입니다. 문서 인식 오류일 수도 있으니 PDF 원문과 함께 확인해 주세요.</p>}
    {result.errorCode === 'REGISTRY_ADDRESS_MISMATCH' && !evidence && <p className="mt-2 text-xs">이전 비교 기록을 확인할 수 없어 주소 차이를 표시하지 못했습니다.</p>}
    {result.errorCode && <p className="mt-3 text-[11px] text-slate-500">오류 코드: {result.errorCode}</p>}
  </>
}
export function RegistryAnalysisFailure({ result }: { result: RegistryProcessing }) {
  return <section role="alert" aria-label="등기사항증명서 분석 실패 안내" className="mt-4 rounded-xl border border-red-200 bg-red-50 p-5 text-red-900"><h3 className="mb-2 font-extrabold">{registryFailureMessage(result.errorCode).title}</h3><FailureContent result={result} /></section>
}
export function RegistryAnalysisFailureDialog({ result, open, onDismiss }: { result: RegistryProcessing; open: boolean; onDismiss: () => void }) {
  const dialog = useRef<HTMLDialogElement>(null)
  useEffect(() => {
    const element = dialog.current
    if (!element) return
    if (open && !element.open) { if (element.showModal) element.showModal(); else element.setAttribute('open', '') }
    if (!open && element.open) { if (element.close) element.close(); else element.removeAttribute('open') }
  }, [open])
  return <dialog ref={dialog} role="alertdialog" aria-modal="true" aria-labelledby="registry-failure-title" onClose={onDismiss} className="fixed inset-0 m-auto max-h-[85vh] w-[min(92vw,640px)] overflow-y-auto rounded-2xl border-0 bg-white p-6 text-slate-700 shadow-2xl backdrop:bg-slate-950/40"><h2 id="registry-failure-title" className="mb-4 text-lg font-extrabold text-red-800">{registryFailureMessage(result.errorCode).title}</h2><FailureContent result={result} /><div className="mt-5 flex justify-end"><Button onClick={onDismiss}>확인</Button></div></dialog>
}
