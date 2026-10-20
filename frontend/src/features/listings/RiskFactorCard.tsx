import { riskLabel } from '../../lib/format'
import { formatRiskText, groupedNumber, presentRiskFactor } from '../../lib/riskPresentation'
import type { RiskFactor } from '../../types/api'

export function RiskFactorCard({ factor }: { factor: RiskFactor }) {
  const info = presentRiskFactor(factor)
  return <article className="rounded-xl border border-slate-200 p-4 sm:p-5">
    <div className="flex items-center justify-between gap-3"><h4 className="text-sm font-extrabold">{info.title}</h4><span className="rounded-md bg-slate-50 px-2.5 py-1 text-[11px] font-bold" style={{ color: factor.severity === 'LOW' ? '#27876c' : factor.severity === 'MEDIUM' ? '#9a6c15' : '#bb3d2b' }}>심각도 {riskLabel[factor.severity]}</span></div>
    <p className="mt-3 break-words text-sm leading-6 text-slate-700">{formatRiskText(factor.description)}</p>
    <dl className="mt-4 grid gap-3 rounded-lg bg-slate-50 p-3 text-xs sm:grid-cols-2"><div><dt className="text-slate-500">{info.valueLabel}</dt><dd className="mt-1 break-words font-bold text-slate-800">{info.valueText}</dd></div><div><dt className="text-slate-500">위험도 평가 점수</dt><dd className="mt-1 font-bold text-slate-800">{groupedNumber(String(factor.score))}점</dd></div></dl>
    <p className="mt-3 text-xs leading-6 text-slate-500">{info.explanation}</p>
  </article>
}
