import request from '@/utils/request'

export interface ProductScriptVersion {
  id: number
  versionNumber: number
  scriptContent: string
  status: string
  score: number
  createTime: string
}

export function searchProductScriptVersions(data?: Record<string, unknown>) {
  return request.post<{ total: number; list: ProductScriptVersion[]; pageNum: number; pageSize: number }>(
    '/product/script-version/search',
    data || {}
  )
}

export function getCurrentProductScriptVersion(productId: number) {
  return request.post<ProductScriptVersion>('/product/script-version/current', { productId })
}

export function getProductScriptVersion(id: number) {
  return request.post<ProductScriptVersion>('/product/script-version/get', { id })
}

export function getProductScriptVersionHistory(productId: number) {
  return request.post<ProductScriptVersion[]>('/product/script-version/history', { productId })
}

export function recommendProductScriptVersions(productId: number, style?: string, topN?: number) {
  return request.post<ProductScriptVersion[]>(
    '/product/script-version/recommend',
    { productId, style, topN }
  )
}

export function diffProductScriptVersions(productId: number, versionA: number, versionB: number) {
  return request.post<{ added: string[]; removed: string[]; changed: string[] }>(
    '/product/script-version/diff',
    { productId, versionA, versionB }
  )
}

export function rollbackProductScriptVersion(
  productId: number,
  targetVersionNumber: number,
  editorId?: number,
  editorName?: string
) {
  return request.post<number>(
    '/product/script-version/rollback',
    { productId, targetVersionNumber, editorId, editorName }
  )
}

export function updateProductScriptVersionScore(productScriptId: number, score: number) {
  return request.post<void>('/product/script-version/update-score', { productScriptId, score })
}
