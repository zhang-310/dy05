import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface SysConfig { id: number; configKey: string; configValue: string; description: string; createTime: string }
export interface ConfigQuery { page?: number; rows?: number; configKey?: string }

export const configApi = {
  list: (params: ConfigQuery) => request.post<PageResult<SysConfig>>('/config/list', params),
  get: (id: number) => request.post<SysConfig>('/config/get', { id }),
  save: (params: Partial<SysConfig>) => request.post<void>('/config/save', params),
  delete: (id: number) => request.post<void>('/config/delete', { id }),
}
