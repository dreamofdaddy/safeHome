import type { ButtonHTMLAttributes, PropsWithChildren, ReactNode } from 'react'
import { LoaderCircle } from 'lucide-react'

export const cx = (...parts: Array<string | false | null | undefined>) => parts.filter(Boolean).join(' ')
export const panel = 'rounded-2xl border border-slate-200 bg-white p-6 shadow-sm sm:p-7'
export const input = 'mt-2 h-12 w-full rounded-lg border border-slate-200 bg-white px-3.5 text-sm text-ink outline-none transition focus:border-brand-500 focus:ring-4 focus:ring-brand-500/10'

export function Button({ children, busy, variant = 'primary', className, ...props }: ButtonHTMLAttributes<HTMLButtonElement> & { busy?: boolean; variant?: 'primary' | 'secondary' }) {
  return <button {...props} disabled={props.disabled || busy} className={cx('inline-flex min-h-12 items-center justify-center gap-2 rounded-lg px-5 text-sm font-bold transition disabled:cursor-not-allowed disabled:opacity-50', variant === 'primary' ? 'bg-brand-600 text-white shadow-lg shadow-brand-600/10 hover:bg-brand-700' : 'bg-brand-50 text-brand-700 hover:bg-brand-100', className)}>{busy && <LoaderCircle size={16} className="animate-spin" />}{children}</button>
}

export function Field({ label, required, children, className }: PropsWithChildren<{ label: string; required?: boolean; className?: string }>) {
  return <label className={cx('block text-xs font-bold text-slate-700', className)}>{label}{required && <span className="ml-1 text-red-500">*</span>}{children}</label>
}

export function PageHeading({ eyebrow, title, description, action, back }: { eyebrow: string; title: string; description: string; action?: ReactNode; back?: ReactNode }) {
  return <header className="flex flex-col items-start justify-between gap-5 py-9 sm:py-12 md:flex-row md:items-end"><div>{back}<span className="mb-3 block text-[11px] font-extrabold tracking-[.2em] text-brand-500">{eyebrow}</span><h1 className="text-3xl font-extrabold tracking-tight text-ink sm:text-4xl">{title}</h1><p className="mt-2 text-sm text-slate-500">{description}</p></div>{action}</header>
}

export function ErrorNotice({ error }: { error: unknown }) {
  if (!error) return null
  return <div role="alert" className="mt-4 rounded-lg bg-red-50 px-4 py-3 text-sm text-red-700">{error instanceof Error ? error.message : '요청을 완료하지 못했습니다.'}</div>
}
