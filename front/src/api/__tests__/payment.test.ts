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

  it('maps order list query to backend enum status payload and normalizes rows', async () => {
    mockPost.mockResolvedValue({
      total: 1,
      list: [{
        id: 1,
        orderNo: 'P2026',
        amount: '99',
        status: 'PENDING_PAYMENT',
        paymentMethod: 'manual',
        trackingNumber: 'SF001',
        createdAt: '2026-05-20T10:00:00',
      }],
      pageNum: 0,
      pageSize: 20,
    })

    const result = await paymentApi.list({ page: 0, rows: 20, status: 'PENDING_PAYMENT', payType: 'wechat', orderNo: 'ignored' })

    expect(mockPost).toHaveBeenCalledWith('/payment/order/list', {
      page: 0,
      rows: 20,
      status: 'PENDING_PAYMENT',
    })
    expect(result.list[0]).toMatchObject({
      status: 'PENDING_PAYMENT',
      payType: 'manual',
      trackingNo: 'SF001',
      createTime: '2026-05-20T10:00:00',
    })
  })

  it('normalizes wrapped order page variants', async () => {
    mockPost.mockResolvedValue({
      data: {
        records: [{
          id: 2,
          orderNo: 'P2027',
          amount: '188',
          status: 'paid',
          payType: 'wechat',
          trackingNo: 'YD001',
          createTime: '2026-05-21 10:00:00',
        }],
        totalElements: 1,
        page: 0,
        size: 20,
      },
    })

    const result = await paymentApi.list({ page: 0, rows: 20 })

    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
      list: [{ orderNo: 'P2027', status: 'PAID', payType: 'wechat', trackingNo: 'YD001' }],
    })
  })

  it('gets order by order number and normalizes wrapped backend response', async () => {
    mockPost.mockResolvedValue({
      result: {
        order: {
          id: '7',
          orderNo: 'P20260410007',
          amount: '299',
          status: 'paid',
          paymentMethod: 'wechat',
          paidAt: '2026-04-10 11:00:00',
          trackingNumber: 'SF007',
          createdAt: '2026-04-10 10:59:00',
        },
      },
    })

    const result = await paymentApi.getByOrderNo('P20260410007')

    expect(mockPost).toHaveBeenCalledWith('/payment/order/getByOrderNo', { orderNo: 'P20260410007' })
    expect(result).toMatchObject({
      id: 7,
      orderNo: 'P20260410007',
      amount: 299,
      status: 'PAID',
      payType: 'wechat',
      trackingNo: 'SF007',
      createTime: '2026-04-10 10:59:00',
    })
  })

  it('create posts backend OrderSaveVO shape with actualAmount fallback', async () => {
    mockPost.mockResolvedValue(1)
    await paymentApi.create({ orderNo: 'P2026', productId: 7, quantity: 1, amount: 99 })
    expect(mockPost).toHaveBeenCalledWith('/payment/order/create', {
      orderNo: 'P2026',
      productId: 7,
      quantity: 1,
      amount: 99,
      actualAmount: 99,
    })
  })

  it('status mutations use backend orderId and user supplied payment proof keys', async () => {
    mockPost.mockResolvedValue(undefined)

    await paymentApi.confirmPayment(5, { transactionId: ' TXN-USER-5 ', paymentMethod: ' manual-transfer ' })
    await paymentApi.ship(5, 'SF001')
    await paymentApi.complete(5)
    await paymentApi.cancel(5)

    expect(mockPost).toHaveBeenNthCalledWith(1, '/payment/order/confirmPayment', {
      orderId: '5',
      transactionId: 'TXN-USER-5',
      paymentMethod: 'manual-transfer',
    })
    expect(mockPost).toHaveBeenNthCalledWith(2, '/payment/order/ship', {
      orderId: '5',
      trackingNumber: 'SF001',
    })
    expect(mockPost).toHaveBeenNthCalledWith(3, '/payment/order/complete', { orderId: 5 })
    expect(mockPost).toHaveBeenNthCalledWith(4, '/payment/order/cancel', { orderId: 5 })
  })

  it('normalizes subscription and queries all backend plan variants', async () => {
    mockPost
      .mockResolvedValueOnce({
        id: 1,
        userId: 1,
        plan: 'pro',
        status: 'active',
        startedAt: '2026-01-01 00:00:00',
        expiresAt: '2026-02-01 00:00:00',
      })
      .mockResolvedValueOnce({ features: ['免费'], price: '免费' })
      .mockResolvedValueOnce({ features: ['专业'], price: '¥299/月' })
      .mockResolvedValueOnce({ features: ['企业'], price: '¥999/月' })

    await expect(paymentApi.subscriptionCurrent()).resolves.toMatchObject({
      planCode: 'pro',
      planName: '专业版',
      createTime: '2026-01-01 00:00:00',
      expireTime: '2026-02-01 00:00:00',
    })
    await expect(paymentApi.subscriptionPlans()).resolves.toHaveLength(3)
    expect(mockPost).toHaveBeenCalledWith('/payment/subscription/plans', { plan: 'free' })
    expect(mockPost).toHaveBeenCalledWith('/payment/subscription/plans', { plan: 'pro' })
    expect(mockPost).toHaveBeenCalledWith('/payment/subscription/plans', { plan: 'enterprise' })
  })

  it('usage quota calls backend usage summary and normalizes legacy quota shape', async () => {
    mockPost.mockResolvedValue({ plan: 'pro', metric: 'aiGenerations', limit: 50, currentUsage: 7, allowed: true })

    await expect(paymentApi.usageQuota()).resolves.toMatchObject({
      aiCall: { label: 'AI 调用额度', used: 7, total: 50 },
      metric: 'aiGenerations',
    })
    expect(mockPost).toHaveBeenCalledWith('/payment/usage/quota', {})
  })

  it('normalizes quota aliases for page models', async () => {
    mockPost.mockResolvedValue({ data: { metric: 'aiGenerations', total: 80, used: 8, last7d: 7, allowed: true } })

    await expect(paymentApi.usageQuota()).resolves.toMatchObject({
      aiCallUsed: 8,
      aiCallLimit: 80,
      aiCallLast7d: 7,
      aiCall: { used: 8, total: 80 },
    })
  })

  it('normalizes wrapped refund page variants', async () => {
    mockPost.mockResolvedValue({
      data: {
        rows: [{ id: 3, orderId: 2, refundNo: 'R2026', amount: '19.9', reason: '重复购买', status: 'PENDING', createTime: '2026-05-22' }],
        totalRecords: 1,
      },
    })

    const result = await paymentApi.refundSearch({ page: 0, rows: 10 })

    expect(result).toMatchObject({
      total: 1,
      list: [{ refundNo: 'R2026', amount: 19.9, reason: '重复购买' }],
    })
  })

  it('invoice APIs fail explicitly because backend is not implemented', async () => {
    await expect(paymentApi.invoiceApply()).rejects.toThrow(/发票申请 后端接口尚未接入/)
    expect(mockPost).not.toHaveBeenCalled()
  })
})
