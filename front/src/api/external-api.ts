import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface ExternalApiConfig {
  id: number
  providerCode: string
  providerName: string
  category?: string
  baseUrl?: string
  apiKeyEncrypted?: string
  apiSecretEncrypted?: string
  isEnabled: boolean
  priority: number
  rateLimitPerMin?: number
  dailyQuota?: number
  monthlyQuota?: number
  lastHealthCheck?: string
  healthStatus?: string
  avgLatencyMs?: number
  successRatePct?: number
  extraConfig?: string
  createTime?: string
  updateTime?: string
}

export interface ExternalApiConfigSearchParams {
  page?: number
  rows?: number
  sortName?: string
  sortOrder?: 'asc' | 'desc'
  category?: string
  healthStatus?: string
  isEnabled?: boolean
  keyword?: string
}

export interface ExternalApiConfigSavePayload {
  id?: number
  providerCode: string
  providerName: string
  category?: string
  baseUrl?: string
  apiKey?: string
  apiSecret?: string
  isEnabled?: boolean
  priority?: number
  rateLimitPerMin?: number
  dailyQuota?: number
  monthlyQuota?: number
  extraConfig?: string
}

export interface ExternalApiConfigLookupPayload {
  providerCode: string
}

export interface ExternalApiCategoryPayload {
  category: string
}

export interface ExternalApiHealthStatusPayload {
  providerCode: string
  status?: string
  latencyMs?: number
  successRate?: number
}

export function searchExternalApiConfigs(data: ExternalApiConfigSearchParams = {}) {
  return request.post<PageResult<ExternalApiConfig>>('/system/external-api/list', data)
}
/** 按供应商编码获取配置（后端 body: { providerCode }） */
export function getExternalApiConfig(data: ExternalApiConfigLookupPayload) {
  return request.post<ExternalApiConfig>('/system/external-api/get', data)
}
export function saveExternalApiConfig(data: ExternalApiConfigSavePayload) {
  return request.post<ExternalApiConfig>('/system/external-api/save', data)
}
export function deleteExternalApiConfig(data: { id: number }) {
  return request.post<void>('/system/external-api/delete', data)
}
export function getExternalApisByCategory(data: ExternalApiCategoryPayload) {
  return request.post<ExternalApiConfig[]>('/system/external-api/by-category', data)
}
export function updateHealthStatus(data: ExternalApiHealthStatusPayload) {
  return request.post<void>('/system/external-api/health-status', data)
}
