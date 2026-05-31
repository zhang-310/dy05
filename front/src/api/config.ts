import request from '@/utils/request'
import { normalizePage } from '@/utils/response-normalize'

export interface SysConfig {
  id: number
  configKey: string
  configValue?: string
  valueType?: string
  isSensitive?: number
  configType?: string
  configName?: string
}
export interface ConfigQuery { page?: number; rows?: number; configKey?: string; configType?: string; keyword?: string }
export interface ConfigSave {
  id?: number
  configKey: string
  configValue?: string
  valueType?: string
  isSensitive?: number
  configType?: string
  configName?: string
}

export const configApi = {
  list: (params: ConfigQuery) => request.post<unknown>('/config/list', params).then((raw) => normalizePage<SysConfig, SysConfig>(raw, row => row, params.page ?? 0, params.rows ?? 20)),
  get: (key: string) => request.post<SysConfig>('/config/get', { key }),
  save: (params: ConfigSave) => request.post<void>('/config/save', params),
  delete: (id: number) => request.post<void>('/config/delete', { id }),
}
