import type { DyProduct } from '@/api/product'

export interface BatchExtractItem {
  id: number
  name: string
  status: 'pending' | 'loading' | 'done' | 'error'
}

function appendCdnSuffix(url: string | undefined, suffix: string): string {
  return url ? `${url}${suffix}` : ''
}

export function getProductThumbUrl(url?: string): string {
  return appendCdnSuffix(url, '@!80X80')
}

export function getProductPreviewUrl(url?: string): string {
  return appendCdnSuffix(url, '@!300X250')
}

export function toDisplayPct(value?: number): number {
  return value != null ? Math.round(value * 100 * 100) / 100 : 0
}

export function toStorePct(value: number): number {
  return Math.round((value / 100) * 10000) / 10000
}

export function buildBatchExtractItems(
  ids: number[],
  products: Array<Pick<DyProduct, 'id' | 'productName'>>,
): BatchExtractItem[] {
  return ids.map((id) => {
    const product = products.find((item) => item.id === id)
    return {
      id,
      name: product?.productName ?? String(id),
      status: 'pending',
    }
  })
}

export function getBatchExtractSummary(items: BatchExtractItem[]) {
  const doneCount = items.filter((item) => item.status === 'done').length
  const errCount = items.filter((item) => item.status === 'error').length
  const progress = items.length > 0 ? ((doneCount + errCount) / items.length) * 100 : 0

  return { doneCount, errCount, progress }
}
