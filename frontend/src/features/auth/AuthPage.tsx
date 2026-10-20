import { useState, type FormEvent } from 'react'
import { Navigate, Link, useLocation, useNavigate } from 'react-router-dom'
import { ShieldCheck } from 'lucide-react'
import { authApi } from './api'
import { useAuth } from './AuthProvider'
import { Button, ErrorNotice, Field, input } from '../../components/ui'
import { formatPhone, isStrongPassword, isValidEmail } from '../../lib/inputFormat'

export function AuthPage({ mode = 'login' }: { mode?: 'login' | 'signup' }) {
  const signup = mode === 'signup'
  const { user, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<unknown>()
  const [phone, setPhone] = useState('')
  if (user) return <Navigate to={user.passwordChangeRequired ? '/change-password' : '/listings'} replace />

  const submit = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault(); setBusy(true); setError(undefined)
    const values = Object.fromEntries(new FormData(event.currentTarget)) as Record<string, string>
    try {
      if (signup) {
        if (!isValidEmail(values.email)) throw new Error('정확한 이메일 주소를 입력해 주세요. 예: name@example.com')
        if (!isStrongPassword(values.password)) throw new Error('비밀번호는 12자 이상이며 영문, 숫자, 특수문자를 모두 포함해야 합니다.')
        if (phone && !/^010-\d{4}-\d{4}$/.test(phone)) throw new Error('휴대전화번호 11자리를 입력해 주세요.')
        await authApi.signup({ email: values.email.trim(), password: values.password, name: values.name, phone })
        navigate('/login', { state: { created: true } })
      } else {
        const loggedIn = await login(values.email, values.password)
        navigate(loggedIn.passwordChangeRequired ? '/change-password' : (location.state as { from?: string } | null)?.from || '/listings')
      }
    } catch (caught) { setError(caught) } finally { setBusy(false) }
  }

  return <section className="grid min-h-[calc(100vh-175px)] items-center gap-10 py-10 lg:grid-cols-[1fr_430px] lg:gap-20">
    <div className="max-w-xl"><span className="mb-4 block text-xs font-extrabold tracking-[.25em] text-brand-500">SAFEHOME</span><h1 className="text-4xl font-extrabold leading-tight tracking-[-.06em] text-ink sm:text-5xl">안심할 수 있는 집의 시작</h1><p className="mt-6 text-base leading-8 text-slate-500">매물과 등기부를 한곳에서 관리하고, 필요한 순간 직접 안전성을 확인하세요.</p><div className="mt-9 flex flex-wrap gap-2 text-xs font-bold text-brand-700"><span className="rounded-full bg-brand-50 px-3 py-2">01 매물 등록</span><span className="rounded-full bg-brand-50 px-3 py-2">02 등기부 확인</span><span className="rounded-full bg-brand-50 px-3 py-2">03 안전성 분석</span></div></div>
    <div className="rounded-2xl border border-slate-200 bg-white p-7 shadow-soft sm:p-9"><ShieldCheck className="mb-5 text-brand-600" /><p className="text-xs font-extrabold tracking-[.15em] text-brand-500">{signup ? '새 계정' : '다시 오신 것을 환영합니다'}</p><h2 className="mt-2 text-2xl font-extrabold">{signup ? '회원가입' : '로그인'}</h2><p className="mt-2 text-xs text-slate-500">{signup ? '안전한 거래를 위한 계정을 만들어 주세요.' : '내 매물과 분석 결과를 이어서 확인하세요.'}</p>{!signup && (location.state as { created?: boolean; passwordChanged?: boolean } | null)?.created && <div className="mt-4 rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700">회원가입이 완료되었습니다. 로그인해 주세요.</div>}{!signup && (location.state as { passwordChanged?: boolean } | null)?.passwordChanged && <div className="mt-4 rounded-lg bg-brand-50 px-4 py-3 text-sm text-brand-700">비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요.</div>}
      <form onSubmit={submit} className="mt-7 space-y-4">{signup && <><Field label="이름" required><input className={input} name="name" autoComplete="name" maxLength={100} required placeholder="홍길동" /></Field><Field label="휴대전화"><input className={input} name="phone" type="tel" inputMode="numeric" autoComplete="tel" maxLength={13} value={phone} onChange={(event) => setPhone(formatPhone(event.target.value))} placeholder="010-1234-1234" /></Field></>}<Field label="이메일" required><input className={input} name="email" type="email" inputMode="email" autoComplete="email" maxLength={254} required placeholder="name@example.com" /></Field>{signup && <p className="-mt-2 text-[11px] leading-5 text-slate-500">아이디와 도메인을 포함한 실제 이메일 형식으로 입력해 주세요.</p>}<Field label="비밀번호" required><input className={input} name="password" type="password" autoComplete={signup ? 'new-password' : 'current-password'} minLength={signup ? 12 : undefined} maxLength={72} required placeholder={signup ? '12자 이상 · 영문+숫자+특수문자' : '비밀번호를 입력해 주세요'} /></Field>{signup && <p className="-mt-2 text-[11px] leading-5 text-slate-500">12자 이상이며 영문 알파벳, 숫자, 특수문자를 각각 1개 이상 포함해야 합니다.</p>}<ErrorNotice error={error} /><Button busy={busy} className="mt-2 w-full" type="submit">{signup ? '계정 만들기' : '로그인'}</Button></form>
      <p className="mt-6 text-center text-xs text-slate-500">{signup ? <>이미 계정이 있으신가요? <Link className="font-bold text-brand-600" to="/login">로그인</Link></> : <>처음 방문하셨나요? <Link className="font-bold text-brand-600" to="/signup">회원가입</Link></>}</p>
    </div>
  </section>
}
