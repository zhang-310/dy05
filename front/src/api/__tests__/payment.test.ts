import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { paymentApi } from '../payment'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('payment API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts order query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await paymentApi.list({ page: 0, rows: 20, orderNo: 'P2026' })
    expect(mockPost).toHaveBeenCalledWith('/payment/order/list', { page: 0, rows: 20, orderNo: 'P2026' })
  })

  it('create posts order create payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await paymentApi.create({ productId: 7, amount: 99, payType: 'alipay' })
    expect(mockPost).toHaveBeenCalledWith('/payment/order/create', {
      productId: 7,
      amount: 99,
      payType: 'alipay',
    })
  })

  it('invoiceApply posts invoice application payload', async () => {
    mockPost.mockResolvedValue({ invoiceId: 1 })
    await paymentApi.invoiceApply(5, { type: 'company', title: '示例公司', taxNo: '123', email: 'a@b.com' })
    expect(mockPost).toHaveBeenCalledWith('/payment/invoice/apply', {
      orderId: 5,
      type: 'company',
      title: '示例公司',
      taxNo: '123',
      email: 'a@b.com',
    })
  })
})
