import axios, { AxiosError } from 'axios'
import type { ApiResponse } from '../../types/api'

const TOKEN_KEY = 'safehome.accessToken'
export const authToken = {
  get: () => sessionStorage.getItem(TOKEN_KEY),
  set: (value: string) => sessionStorage.setItem(TOKEN_KEY, value),
  clear: () => sessionStorage.removeItem(TOKEN_KEY),
}

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '/api',
  timeout: 30_000,
})

apiClient.interceptors.request.use((config) => {
  const token = authToken.get()
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

export async function unwrap<T>(request: Promise<{ data: ApiResponse<T> }>): Promise<T> {
  try {
    const { data } = await request
    if (!data.success) throw new Error(data.error?.message || '요청을 완료하지 못했습니다.')
    return data.data
  } catch (error) {
    if (error instanceof AxiosError) {
      const body = error.response?.data as ApiResponse<never> | undefined
      const wrapped = new Error(body?.error?.message || (error.response ? `요청을 완료하지 못했습니다. (${error.response.status})` : '서버에 연결할 수 없습니다.'))
      Object.assign(wrapped, { status: error.response?.status, code: body?.error?.code })
      throw wrapped
    }
    throw error
  }
}

export const errorStatus = (error: unknown) => (error as { status?: number })?.status
