import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface AbVariant {
  id: number
  experimentId: number
  variantName: string
  trafficRatio: number
  scriptStyle: string
  conversions: number
  exposures: number
  conversionRate: number
  isWinner: boolean
}

export interface AbExperiment {
  id: number
  experimentName: string
  description: string
  status: number          // 0=草稿 1=进行中 2=已暂停 3=已结束
  trafficSplit: number
  winnerVariantId: number | null
  startTime: string
  endTime: string
  createTime: string
  variants?: AbVariant[]
}

export interface AbQuery { page?: number; rows?: number; experimentName?: string; status?: number }

export interface AbExperimentResult {
  experimentId: number
  experimentName: string
  variants: AbVariant[]
  winnerVariantId: number | null
  totalExposures: number
  totalConversions: number
  overallConversionRate: number
}

export interface AbDailyTrend {
  date: string
  variantId: number
  variantName: string
  exposures: number
  conversions: number
  conversionRate: number
}

export const abtestApi = {
  list: (params: AbQuery) => request.post<PageResult<AbExperiment>>('/abtest/experiment/list', params),
  get: (id: number) => request.post<AbExperiment>('/abtest/experiment/get', { id }),
  save: (params: Partial<AbExperiment>) => request.post<void>('/abtest/experiment/save', params),
  delete: (id: number) => request.post<void>('/abtest/experiment/delete', { id }),
  updateStatus: (id: number, status: number) => request.post<void>('/abtest/experiment/update-status', { id, status }),
  setWinner: (experimentId: number, variantId: number) => request.post<void>('/abtest/experiment/set-winner', { experimentId, variantId }),
  result: (experimentId: number) => request.post<AbExperimentResult>('/abtest/experiment/result', { experimentId }),
  dailyTrend: (experimentId: number, days = 7) => request.post<AbDailyTrend[]>('/abtest/experiment/daily-trend', { experimentId, days }),

  // P1: 实验变体列表
  variantList: (experimentId: number) =>
    request.post<AbVariant[]>('/abtest/variant/list', { experimentId }),
  variantSave: (params: Partial<AbVariant>) => request.post<void>('/abtest/variant/save', params),
  variantDelete: (id: number) => request.post<void>('/abtest/variant/delete', { id }),

  // 便捷状态操作（语义化包装 updateStatus）
  start: (id: number) =>
    request.post<void>('/abtest/experiment/update-status', { id, status: 1 }),
  pause: (id: number) =>
    request.post<void>('/abtest/experiment/update-status', { id, status: 2 }),
  stop: (id: number) =>
    request.post<void>('/abtest/experiment/update-status', { id, status: 3 }),

  eventRecord: (params: { experimentId: number; variantId: number; eventType: string }) =>
    request.post<void>('/abtest/event/record', params),
}
