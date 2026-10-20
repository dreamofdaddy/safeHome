import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthProvider'

export function AuthGuard() {
  const { user, ready } = useAuth()
  const location = useLocation()
  if (!ready) return <div className="grid min-h-[60vh] place-items-center text-sm text-slate-500">로그인 상태를 확인하는 중입니다…</div>
  if (!user) return <Navigate to="/login" replace state={{ from: location.pathname }} />
  if (user.passwordChangeRequired) return <Navigate to="/change-password" replace />
  return <Outlet />
}
