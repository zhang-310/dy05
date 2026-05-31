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
      aiCall: { label: 'AI 调用额度', used: 120, total: 1000, resetTime: '按当前订阅周期' },
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
      expect(screen.getByText(/subscription\/check-quota/)).toBeInTheDocument()
    })

    expect(screen.getByTestId('payment-usage-quota-workbench')).toHaveAttribute('data-contract-scope', 'payment-usage-quota')
    expect(screen.getByTestId('payment-usage-quota-workbench')).toHaveAttribute('data-contract-status', 'ready')
    expect(screen.getByTestId('payment-usage-quota-workbench')).toHaveAttribute('data-ready-endpoint', '/payment/usage/quota')
    expect(screen.getByTestId('payment-usage-quota-workbench')).toHaveAttribute('data-support-endpoint', '/payment/subscription/check-quota')
    expect(screen.getByTestId('payment-usage-quota-workbench')).toHaveAttribute('data-unsupported-actions', 'quota-alert-config')
    expect(screen.getByTestId('payment-usage-quota-workbench')).not.toHaveAttribute('data-no-usage-quota-endpoint-call')
    expect(screen.getByTestId('payment-usage-quota-workbench')).toHaveAttribute('data-no-quota-alert-config-call', 'true')
    expect(screen.getByTestId('payment-usage-quota-workbench')).toHaveAttribute('data-no-mock-quota-dimensions', 'true')
    expect(screen.getByTestId('payment-usage-quota-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(screen.getByTestId('payment-usage-quota-contract-downgrade')).toHaveAttribute('data-contract-status', 'ready')

    const card = screen.getByTestId('payment-usage-quota-card')
    expect(card).toHaveAttribute('data-contract-status', 'ready')
    expect(card).toHaveAttribute('data-contract-endpoint', '/payment/usage/quota')
    expect(card).not.toHaveAttribute('data-degraded-endpoint')
    expect(card).toHaveAttribute('data-quota-key', 'aiCall')
    expect(card).toHaveAttribute('data-quota-limit-state', 'configured')
    expect(card).toHaveAttribute('data-quota-used', '120')
    expect(card).toHaveAttribute('data-quota-total', '1000')
    expect(card).toHaveAttribute('data-quota-percent', '12')
  })

  it('marks zero quota limits as unlimited instead of faking usage quota aggregation', async () => {
    vi.mocked(paymentApi.usageQuota).mockResolvedValueOnce({
      aiCall: { label: 'AI 调用额度', used: 8, total: 0, resetTime: '按当前订阅周期' },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <UsageQuotaPage />
      </MemoryRouter>,
    )

    const card = await screen.findByTestId('payment-usage-quota-card')
    expect(card).toHaveAttribute('data-quota-limit-state', 'unlimited')
    expect(card).toHaveAttribute('data-quota-total', '0')
    expect(screen.getByText('总量：不限')).toBeInTheDocument()
    expect(screen.getByText('未配置上限，当前仅展示消耗量。')).toBeInTheDocument()
  })

  it('shows retryable quota load error', async () => {
    vi.mocked(paymentApi.usageQuota).mockRejectedValueOnce(new Error('quota down'))

    renderWithProviders(
      <MemoryRouter>
        <UsageQuotaPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/额度加载失败/)).toBeInTheDocument()
    expect(screen.getByText(/quota down/)).toBeInTheDocument()
    expect(screen.getByText(/不会使用本地配额假数据兜底/)).toBeInTheDocument()
    expect(screen.getByTestId('payment-usage-quota-contract-downgrade')).toHaveAttribute('data-contract-status', 'ready')
  })

  it('marks empty quota response as real empty state without mock dimensions', async () => {
    vi.mocked(paymentApi.usageQuota).mockResolvedValueOnce({} as never)

    renderWithProviders(
      <MemoryRouter>
        <UsageQuotaPage />
      </MemoryRouter>,
    )

    const empty = await screen.findByTestId('payment-usage-quota-empty')
    expect(empty).toHaveAttribute('data-no-mock-quota-dimensions', 'true')
    expect(empty).toHaveTextContent('暂无额度数据')
    expect(empty).toHaveTextContent('/payment/usage/quota')
    expect(screen.queryByTestId('payment-usage-quota-card')).not.toBeInTheDocument()
  })
})
