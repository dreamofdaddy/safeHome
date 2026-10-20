import { useState, type FormEvent } from 'react'
import { Navigate, useNavigate } from 'react-router-dom'
import { KeyRound } from 'lucide-react'
import { Button, ErrorNotice, Field, input } from '../../components/ui'
import { isStrongPassword } from '../../lib/inputFormat'
import { useAuth } from './AuthProvider'
import { authApi } from './api'

export function ChangePasswordPage() {
  const { user, ready, logout } = useAuth()
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>()

  if (!ready) return <div className="grid min-h-[60vh] place-items-center text-sm text-slate-500">로그인 상태를 확인하는 중입니다…</div>
  if (!user) return <Navigate to="/login" replace />

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setBusy(true); setError(undefined)
    const values = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>
    try {
      if (!isStrongPassword(values.newPassword)) throw new Error('새 비밀번호는 12자 이상이며 영문, 숫자, 특수문자를 모두 포함해야 합니다.')
      if (values.newPassword !== values.confirmPassword) throw new Error('새 비밀번호 확인이 일치하지 않습니다.')
      if (values.currentPassword === values.newPassword) throw new Error('현재 비밀번호와 다른 비밀번호를 입력해 주세요.')
      await authApi.changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword })
      await logout().catch(() => undefined)
      navigate('/login', { replace: true, state: { passwordChanged: true } })
    } catch (caught) { setError(caught) } finally { setBusy(false) }
  }

  return <section className="mx-auto grid min-h-[calc(100vh-175px)] max-w-md place-items-center py-10"><div className="w-full rounded-2xl border border-slate-200 bg-white p-7 shadow-soft sm:p-9"><KeyRound className="mb-5 text-brand-600" /><p className="text-xs font-extrabold tracking-[.15em] text-brand-500">PASSWORD UPDATE</p><h1 className="mt-2 text-2xl font-extrabold">비밀번호 변경</h1><p className="mt-2 text-xs leading-6 text-slate-500">비밀번호 사용 기간이 3개월을 경과했습니다. 변경 후 새 비밀번호로 다시 로그인해 주세요.</p><form onSubmit={submit} className="mt-7 space-y-4"><Field label="현재 비밀번호" required><input className={input} name="currentPassword" type="password" autoComplete="current-password" required /></Field><Field label="새 비밀번호" required><input className={input} name="newPassword" type="password" autoComplete="new-password" minLength={12} maxLength={72} required placeholder="12자 이상 · 영문+숫자+특수문자" /></Field><Field label="새 비밀번호 확인" required><input className={input} name="confirmPassword" type="password" autoComplete="new-password" minLength={12} maxLength={72} required /></Field><p className="text-[11px] leading-5 text-slate-500">현재 또는 직전 비밀번호와 동일한 비밀번호는 사용할 수 없습니다.</p><ErrorNotice error={error} /><Button busy={busy} className="w-full" type="submit">비밀번호 변경</Button></form></div></section>
}
