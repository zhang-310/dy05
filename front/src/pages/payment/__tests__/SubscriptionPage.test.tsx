import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import SubscriptionPage from '../SubscriptionPage'
import { paymentApi } from '@/api/payment'

vi.mock('@/api/payment', () => ({
  paymentApi: {
    subscriptionCurrent: vi.fn(),
    subscriptionPlans: vi.fn(),
    usageQuota: vi.fn(),
    subscriptionUpgrade: vi.fn(),
    invoiceApply: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('SubscriptionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(paymentApi.subscriptionCurrent).mockResolvedValue({
      id: 1,
      userId: 1,
      planCode: 'pro',
      planName: '专业版',
      status: 1,
      expireTime: '2026-12-31 00:00:00',
      createTime: '2026-01-01 00:00:00',
    } as never)
    vi.mocked(paymentApi.subscriptionPlans).mockResolvedValue([
      { planCode: 'pro', planName: '专业版', price: 699, features: ['AI 生成', '知识库'] },
    ] as never)
    vi.mocked(paymentApi.usageQuota).mockResolvedValue({
      aiCallUsed: 120,
      aiCallLimit: 1000,
      aiCallLast7d: 70,
    } as never)
  })

  it('loads subscription info and renders plan card', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(paymentApi.subscriptionCurrent).toHaveBeenCalled()
      expect(paymentApi.subscriptionPlans).toHaveBeenCalled()
      expect(paymentApi.usageQuota).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('🏆 专业版')).toBeInTheDocument()
      expect(screen.getByText('套餐选择与对比')).toBeInTheDocument()
    })
  })
})
