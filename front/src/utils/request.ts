import axios, { AxiosRequestConfig } from 'axios'
import { buildLoginHref, getCurrentLocationForReturn } from '@/utils/login-redirect'
import { commercialDenialMessage, isCommercialDenial } from '@/utils/commercialError'

const instance = axios.create({
  baseURL: '/api/v1',
  timeout: 300000, // 5 分钟超时（AI 分析、视频采集等长时任务需要）
})

const AUTH_ERROR_CODES = new Set([2001, 2003])

export function isAuthBusinessCode(status: unknown): status is number {
  return typeof status === 'number' && AUTH_ERROR_CODES.has(status)
}

function redirectToLogin() {
  localStorage.removeItem('token')
  if (typeof window !== 'undefined' && window.location.pathname !== '/login') {
    window.location.href = buildLoginHref(getCurrentLocationForReturn())
  }
}

// Request interceptor: attach Bearer token
instance.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers = config.headers ?? {}
    config.headers['Authorization'] = `Bearer ${token}`
  }
  return config
})

// Response interceptor: unwrap RESTResult.data, handle auth errors
instance.interceptors.response.use(
  (response) => {
    const result = response.data
    // 业务体 status：200 为成功并解包 data（后端勿对无载荷接口返回 data=null+旧版 getSuccess(null) 会变为 204）
    if (result && result.status === 200) {
      return result.data
    }
    if (isAuthBusinessCode(result?.status)) {
      redirectToLogin()
    }
    if (isCommercialDenial(result?.status)) {
      const err = new Error(commercialDenialMessage(result?.status, result?.message)) as Error & { code?: number }
      err.code = result.status
      return Promise.reject(err)
    }
    return Promise.reject(new Error(result?.message ?? '请求失败'))
  },
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      redirectToLogin()
    }
    if (error.response?.status === 402) {
      const body = error.response?.data as { status?: number; message?: string } | undefined
      const err = new Error(commercialDenialMessage(body?.status, body?.message)) as Error & { code?: number }
      err.code = body?.status ?? 402
      return Promise.reject(err)
    }
    return Promise.reject(error)
  }
)

export const request = {
  post<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.post(url, data, config) as Promise<T>
  },
  get<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.get(url, config) as Promise<T>
  },
  put<T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig): Promise<T> {
    return instance.put(url, data, config) as Promise<T>
  },
  delete<T = unknown>(url: string, config?: AxiosRequestConfig): Promise<T> {
    return instance.delete(url, config) as Promise<T>
  },
}

export default request
