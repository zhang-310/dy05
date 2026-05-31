import request from '@/utils/request'
import { normalizePage } from '@/utils/response-normalize'

export interface SlangEntry {
  id: number
  userId?: number
  phrase: string
  meaning?: string
  category?: string
  usageScene?: string
  example?: string
  source?: string
  useCount?: number
  status?: number
  createTime?: string
  updateTime?: string
  productIds?: number[]
}
export interface SlangQuery { page?: number; rows?: number; keyword?: string; category?: string; usageScene?: string; status?: number; productId?: number }
export interface SlangSave { id?: number; phrase: string; meaning?: string; category?: string; usageScene?: string; example?: string; source?: string; status?: number }

export const slangApi = {
  list: (params: SlangQuery) => request.post<unknown>('/slangdict/entry/search', params)
    .then(raw => normalizePage<SlangEntry, SlangEntry>(raw, row => row, Number(params.page ?? 0), Number(params.rows ?? 20))),
  save: (params: SlangSave) => request.post<number>('/slangdict/entry/save', params),
  delete: (id: number) => request.post<void>(`/slangdict/entry/delete?id=${encodeURIComponent(String(id))}`, {}),
  byProduct: (productId: number) => request.post<SlangEntry[]>(`/slangdict/entry/by-product?productId=${encodeURIComponent(String(productId))}`, {}),
  bindProduct: (entryId: number, productId: number) =>
    request.post<void>(`/slangdict/entry/bind-product?entryId=${encodeURIComponent(String(entryId))}&productId=${encodeURIComponent(String(productId))}`, {}),
  unbindProduct: (entryId: number, productId: number) =>
    request.post<void>(`/slangdict/entry/unbind-product?entryId=${encodeURIComponent(String(entryId))}&productId=${encodeURIComponent(String(productId))}`, {}),
  aiGenerate: (productId: number, count = 5) =>
    request.post<string[]>(`/slangdict/entry/ai-generate?productId=${encodeURIComponent(String(productId))}&count=${encodeURIComponent(String(count))}`, {}),
}
