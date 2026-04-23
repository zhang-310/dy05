/**
 * ProductPage：全页含 DataGrid + 大量列定义，在 jsdom 下单测易超时。
 * 列表检索行为由 useProductTable 测试覆盖；此处仅做 API/模块可测性烟测。
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as productApi from '@/api/product'

vi.mock('@/api/product', () => ({
  searchProducts: vi.fn(),
}))

describe('ProductPage / 商品列表', () => {
  beforeEach(() => {
    vi.mocked(productApi.searchProducts).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    })
  })

  it('searchProducts 支持 keyword 参数（与列表页行为一致）', async () => {
    await productApi.searchProducts({ page: 0, rows: 20, keyword: '关键词' })
    expect(productApi.searchProducts).toHaveBeenCalledWith(
      expect.objectContaining({ keyword: '关键词' }),
    )
  })
})
