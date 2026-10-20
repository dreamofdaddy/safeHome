import { apiClient, unwrap } from '../../lib/api/client'
import type { LoginResponse, User } from '../../types/api'

export const authApi = {
  signup: (body: { email: string; password: string; name: string; phone?: string }) => unwrap<User>(apiClient.post('/auth/signup', body)),
  login: (body: { email: string; password: string }) => unwrap<LoginResponse>(apiClient.post('/auth/login', body)),
  me: () => unwrap<User>(apiClient.get('/auth/me')),
  changePassword: (body: { currentPassword: string; newPassword: string }) => unwrap<User>(apiClient.post('/auth/change-password', body)),
  logout: () => apiClient.post('/auth/logout'),
}
