import { describe, expect, it } from 'vitest'
import type { DyProduct } from '@/api/product'
import {
  buildBatchExtractItems,
  getBatchExtractSummary,
  getProductPreviewUrl,
  getProductThumbUrl,
  toDisplayPct,
  toStorePct,
} from './productPageModel'

describe('productPageModel', () => {
  const products: Array<Pick<DyProduct, 'id' | 'productName'>> = [
    { id: 1, productName: '修护精华' },
    { id: 2, productName: '防晒喷雾' },
  ]

  it('builds CDN image urls consistently', () => {
    expect(getProductThumbUrl('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png@!80X80')
    expect(getProductPreviewUrl('https://cdn.example.com/a.png')).toBe('https://cdn.example.com/a.png@!300X250')
    expect(getProductThumbUrl()).toBe('')
  })

  it('converts profit margin between storage and display values', () => {
    expect(toDisplayPct(0.5566)).toBe(55.66)
    expect(toStorePct(55.66)).toBe(0.5566)
  })

  it('builds batch extract items from selected products', () => {
    expect(
      buildBatchExtractItems([1, 3], products),
    ).toEqual([
      { id: 1, name: '修护精华', status: 'pending' },
      { id: 3, name: '3', status: 'pending' },
    ])
  })

  it('summarizes batch extract progress', () => {
    expect(
      getBatchExtractSummary([
        { id: 1, name: 'A', status: 'done' },
        { id: 2, name: 'B', status: 'error' },
        { id: 3, name: 'C', status: 'pending' },
      ]),
    ).toEqual({
      doneCount: 1,
      errCount: 1,
      progress: 66.66666666666666,
    })
  })
})
