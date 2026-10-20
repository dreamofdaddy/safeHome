import { createContext, useContext, useEffect, useMemo, useState, type PropsWithChildren } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { authToken } from '../../lib/api/client'
import type { User } from '../../types/api'
import { authApi } from './api'

interface AuthValue {
  user: User | null; ready: boolean;
  login: (email: string, password: string) => Promise<User>;
  logout: () => Promise<void>;
}
const AuthContext = createContext<AuthValue | null>(null)

export function AuthProvider({ children }: PropsWithChildren) {
  const [user, setUser] = useState<User | null>(null)
  const [ready, setReady] = useState(() => !authToken.get())
  const queryClient = useQueryClient()

  useEffect(() => {
    if (!authToken.get()) return
    authApi.me().then(setUser).catch(() => authToken.clear()).finally(() => setReady(true))
  }, [])

  const value = useMemo<AuthValue>(() => ({
    user, ready,
    login: async (email, password) => {
      const result = await authApi.login({ email, password })
      authToken.set(result.accessToken)
      setUser(result.user)
      return result.user
    },
    logout: async () => {
      try { if (authToken.get()) await authApi.logout() } finally {
        authToken.clear(); setUser(null); queryClient.clear()
      }
    },
  }), [queryClient, ready, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const value = useContext(AuthContext)
  if (!value) throw new Error('AuthProvider가 필요합니다.')
  return value
}
