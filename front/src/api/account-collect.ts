import request from '@/utils/request'

export interface CollectHistoryItem {
  id: number
  accountId: string
  platform: string
  collectType: string
  status: string
  dataCount: number
  createTime: string
}

export function collectAccountData(params: Record<string, unknown>) {
  return request.post<{ jobId: string; status: string }>('/account-collect/collect', params)
}

export function collectHistory(params?: Record<string, unknown>) {
  return request.post<{ total: number; list: CollectHistoryItem[] }>('/account-collect/history', params || {})
}

export function collectStats() {
  return request.post<{
    totalCollected: number
    todayCollected: number
    successRate: number
    lastCollectTime: string
  }>('/account-collect/stats', {})
}
