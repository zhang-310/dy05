import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import UsageQuotaPage from '../UsageQuotaPage'
import { paymentApi } from '@/api/payment'

vi.mock('@/api/payment', () => ({
  paymentApi: {
    usageQuota: vi.fn(),
  },
}))

describe('UsageQuotaPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(paymentApi.usageQuota).mockResolvedValue({
      aiCall: { label: 'AI 调用额度', used: 120, total: 1000, resetTime: '每日 00:00' },
      storage: { label: '存储额度', used: 30, total: 100, resetTime: '每月 1 日' },
    } as never)
  })

  it('loads quota map and renders quota cards', async () => {
    renderWithProviders(
      <MemoryRouter>
        <UsageQuotaPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(paymentApi.usageQuota).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('额度使用详情')).toBeInTheDocument()
      expect(screen.getByText('AI 调用额度')).toBeInTheDocument()
      expect(screen.getByText('存储额度')).toBeInTheDocument()
    })
  })
})
