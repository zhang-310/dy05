import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface SlangEntry {
  id: number; term: string; definition: string; examples: string
  category: string; status: number; createTime: string
}
export interface SlangQuery { page?: number; rows?: number; term?: string; category?: string; status?: number }
export interface SlangSave { id?: number; term: string; definition: string; examples?: string; category?: string; status?: number }

export const slangApi = {
  list: (params: SlangQuery) => request.post<PageResult<SlangEntry>>('/slangdict/list', params),
  save: (params: Partial<SlangSave>) => request.post<void>('/slangdict/save', params),
  delete: (id: number) => request.post<void>('/slangdict/delete', { id }),
  enable: (id: number) => request.post<void>('/slangdict/enable', { id }),
  disable: (id: number) => request.post<void>('/slangdict/disable', { id }),
}
