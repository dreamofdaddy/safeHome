import { Home, LogOut } from 'lucide-react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useAuth } from '../features/auth/AuthProvider'
import { cx } from './ui'

export function AppLayout() {
  const { user, logout } = useAuth()
  const navigate = useNavigate()
  return <div className="flex min-h-screen flex-col bg-[#f5f7f5] text-ink">
    <header className="sticky top-0 z-20 border-b border-slate-200 bg-white/95 backdrop-blur"><div className="mx-auto flex h-[72px] max-w-[1180px] items-center gap-8 px-5"><NavLink to="/listings" className="flex items-center gap-2.5 text-xl font-extrabold tracking-tight text-brand-900"><span className="grid size-9 place-items-center rounded-xl bg-brand-600 text-white"><Home size={21} /></span>SafeHome</NavLink>{user && !user.passwordChangeRequired && <nav className="flex h-full items-center gap-6 text-sm text-slate-500"><NavLink to="/listings" className={({ isActive }) => cx('flex h-full items-center border-b-2 border-transparent', isActive && 'border-brand-600 font-bold text-brand-700')}>내 매물</NavLink><NavLink to="/listings/new" className={({ isActive }) => cx('flex h-full items-center border-b-2 border-transparent', isActive && 'border-brand-600 font-bold text-brand-700')}>매물 등록</NavLink></nav>}<div className="ml-auto flex items-center gap-3 text-xs">{user ? <><span className="hidden font-bold sm:inline">{user.name}님</span><button className="flex items-center gap-1 font-bold text-slate-500 hover:text-brand-700" onClick={async () => { await logout(); navigate('/login') }}><LogOut size={14} /> 로그아웃</button></> : <NavLink className="font-bold text-brand-700" to="/login">로그인</NavLink>}</div></div></header>
    <main className="mx-auto w-full max-w-[1180px] flex-1 px-5"><Outlet /></main>
    <footer className="mx-auto mt-16 flex w-full max-w-[1180px] gap-4 border-t border-slate-200 px-5 py-6 text-xs text-slate-400"><span className="font-bold text-slate-500">SafeHome</span><span>안전한 거래를 위한 첫 번째 확인</span></footer>
  </div>
}
