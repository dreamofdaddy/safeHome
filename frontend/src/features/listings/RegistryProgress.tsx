import { FileSearch, LoaderCircle } from 'lucide-react'

export function RegistryProgress() {
  return <div className="mt-5 rounded-2xl border border-brand-500/20 bg-gradient-to-br from-brand-50 to-white p-5" role="status" aria-live="polite">
    <div className="flex items-center gap-3"><span className="grid size-11 shrink-0 place-items-center rounded-xl bg-white text-brand-600 shadow-sm"><FileSearch size={23} /></span><div><p className="text-sm font-extrabold text-brand-900">등기사항증명서 내용을 확인하고 있습니다</p><p className="mt-1 text-xs leading-5 text-slate-500">주소와 소유자, 권리관계를 분석 중입니다.</p></div></div>
    <div className="mt-5 h-2.5 overflow-hidden rounded-full bg-brand-100" role="progressbar" aria-label="등기사항증명서 분석 진행 중"><div className="registry-progress-bar h-full w-2/5 rounded-full bg-gradient-to-r from-brand-500 via-brand-600 to-brand-500 shadow-sm" /></div>
    <div className="mt-3 flex items-center justify-between gap-3 text-xs"><span className="flex items-center gap-1.5 font-semibold text-brand-700"><LoaderCircle size={13} className="animate-spin motion-reduce:animate-none" />분석 진행 중</span><span className="text-slate-500">완료되면 자동으로 표시됩니다</span></div>
    <p className="mt-3 text-[11px] leading-5 text-slate-500">문서 분량에 따라 잠시 시간이 걸릴 수 있습니다.</p>
  </div>
}
