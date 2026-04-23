/**
 * product API 模块测试（mock request）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { searchProducts, productApi } from '../product'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('product API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('searchProducts calls post with correct path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await searchProducts({ page: 0, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/product/search', { page: 0, rows: 10 })
  })

  it('get calls post with id', async () => {
    mockPost.mockResolvedValue({ id: 1, productName: '测试商品', userId: 1, productCode: '', category: '', brand: '', price: 0, costPrice: 0, profitMarginPct: 0, inventory: 0, unit: '', weight: 0, mainImage: '', description: '', highlights: '', sellingPoints: '', status: 1, createTime: '', updateTime: '' })
    await productApi.get(1)
    expect(mockPost).toHaveBeenCalledWith('/product/get', { id: 1 })
  })

  it('save calls post with data', async () => {
    mockPost.mockResolvedValue(undefined)
    const data = { productName: '新商品', price: 100, status: 1 }
    await productApi.save(data)
    expect(mockPost).toHaveBeenCalledWith('/product/save', data)
  })

  it('delete calls post with id', async () => {
    mockPost.mockResolvedValue(undefined)
    await productApi.delete(1)
    expect(mockPost).toHaveBeenCalledWith('/product/delete', { id: 1 })
  })
})
