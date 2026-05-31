import type { LiveScript } from '@/api/live'
import type { LiveProduct } from '@/api/live-product'

function numberValue(value: unknown): number | null {
  if (value == null || value === '') return null
  const n = Number(value)
  return Number.isFinite(n) ? n : null
}

function compareNullableNumber(a: number | null, b: number | null): number {
  if (a == null && b == null) return 0
  if (a == null) return 1
  if (b == null) return -1
  return a - b
}

export function compareLiveProducts(a: LiveProduct, b: LiveProduct): number {
  return compareNullableNumber(numberValue(a.position ?? a.sortOrder), numberValue(b.position ?? b.sortOrder))
    || compareNullableNumber(numberValue(a.id), numberValue(b.id))
    || compareNullableNumber(numberValue(a.productId), numberValue(b.productId))
    || String(a.productName ?? '').localeCompare(String(b.productName ?? ''), 'zh-Hans')
}

export function sortLiveProducts(products: LiveProduct[]): LiveProduct[] {
  return [...products].sort(compareLiveProducts)
}

export function compareLiveScripts(a: LiveScript, b: LiveScript): number {
  return compareNullableNumber(numberValue(a.sequenceNo ?? a.sortOrder), numberValue(b.sequenceNo ?? b.sortOrder))
    || compareNullableNumber(numberValue(a.executionTime), numberValue(b.executionTime))
    || compareNullableNumber(numberValue(a.id), numberValue(b.id))
}

export function sortLiveScripts(scripts: LiveScript[]): LiveScript[] {
  return [...scripts].sort(compareLiveScripts)
}

