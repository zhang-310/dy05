import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import SubscriptionPage from '../SubscriptionPage'
import { paymentApi } from '@/api/payment'

vi.mock('@/api/payment', () => ({
  paymentApi: {
    subscriptionCurrent: vi.fn(),
    subscriptionPlans: vi.fn(),
    usageQuota: vi.fn(),
    subscriptionUpgrade: vi.fn(),
    list: vi.fn(),
    refundSearch: vi.fn(),
    refundCreate: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('SubscriptionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(paymentApi.subscriptionCurrent).mockResolvedValue({
      id: 1,
      userId: 1,
      plan: 'pro',
      planCode: 'pro',
      planName: '专业版',
      status: 'active',
      expireTime: '2026-12-31 00:00:00',
      createTime: '2026-01-01 00:00:00',
    } as never)
    vi.mocked(paymentApi.subscriptionPlans).mockResolvedValue([
      { planCode: 'free', planName: '免费版', price: 0, features: ['基础额度'] },
      { planCode: 'pro', planName: '专业版', price: 299, features: ['AI 生成', '知识库'] },
    ] as never)
    vi.mocked(paymentApi.usageQuota).mockResolvedValue({
      aiCallUsed: 120,
      aiCallLimit: 1000,
      aiCallLast7d: 70,
    } as never)
    vi.mocked(paymentApi.list).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 } as never)
    vi.mocked(paymentApi.refundSearch).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 } as never)
  })

  it('loads subscription info and renders real downgrade copy', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(paymentApi.subscriptionCurrent).toHaveBeenCalled()
      expect(paymentApi.subscriptionPlans).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getAllByText('专业版').length).toBeGreaterThan(0)
    })
    const workbench = screen.getByTestId('payment-subscription-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'payment-subscription')
    expect(workbench).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/payment/subscription/current'))
    expect(workbench).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/payment/refund/create'))
    expect(workbench).toHaveAttribute('data-unsupported-actions', expect.stringContaining('renew-cashier'))
    expect(workbench).toHaveAttribute('data-no-local-subscription-fallback', 'true')
    expect(workbench).toHaveAttribute('data-no-refund-approval-actions', 'true')
    expect(screen.getByTestId('payment-subscription-plan-workbench')).toHaveAttribute('data-no-local-plan-fallback', 'true')
    expect(screen.getByTestId('payment-subscription-plan-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(screen.getByText(/续费、发票、账单下载和配额包购买后端接口尚未落库/)).toBeInTheDocument()
    expect(screen.getByText('订阅读取')).toBeInTheDocument()
    expect(screen.getByText('续费收银台')).toBeInTheDocument()
  })

  it('uses a theme-aware current plan surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <SubscriptionPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    const currentPlanSurface = await screen.findByTestId('subscription-current-plan-surface')
    expect(currentPlanSurface).not.toHaveStyle({ backgroundColor: 'rgb(227, 242, 253)' })
  })

  it('shows retryable subscription load error', async () => {
    vi.mocked(paymentApi.subscriptionCurrent).mockRejectedValueOnce(new Error('subscription down'))

    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/订阅接口异常/)).toBeInTheDocument()
    expect(screen.getByText(/subscription down/)).toBeInTheDocument()
  })

  it('quota tab uses quota downgrade message without alert config calls', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('tab', { name: '配额监控' }))

    expect(await screen.findByTestId('payment-subscription-quota-workbench')).toHaveAttribute('data-no-quota-alert-config-call', 'true')
    expect(screen.getByTestId('payment-subscription-quota-workbench')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/payment/usage/quota'))
    expect(screen.getByTestId('payment-subscription-quota-workbench')).toHaveAttribute('data-degraded-endpoints', 'quota-alert-config')
    expect(screen.getByTestId('payment-subscription-quota-workbench')).toHaveAttribute('data-no-mock-quota-dimensions', 'true')
    expect(screen.getByTestId('payment-subscription-quota-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(await screen.findByText(/`\/payment\/usage\/quota` 已接入真实用量汇总/)).toBeInTheDocument()
    expect(screen.getByText('配额预警')).toBeInTheDocument()
    expect(paymentApi.usageQuota).toHaveBeenCalled()
  })

  it('orders tab shows invoice and export downgrade as capability cards', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('tab', { name: '订单历史' }))

    expect(await screen.findByTestId('payment-subscription-orders-workbench')).toHaveAttribute('data-no-local-order-fallback', 'true')
    expect(screen.getByTestId('payment-subscription-orders-workbench')).toHaveAttribute('data-unsupported-actions', 'order-export,invoice-apply')
    expect(screen.getByTestId('payment-subscription-orders-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(await screen.findByText(/订单导出和发票申请接口尚未接入/)).toBeInTheDocument()
    expect(screen.getByText('订单导出')).toBeInTheDocument()
    expect(screen.getByText('发票申请')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: '导出未接入' })).not.toBeInTheDocument()
  })

  it('keeps selected plan dialog open when upgrade fails', async () => {
    vi.mocked(paymentApi.subscriptionUpgrade).mockRejectedValueOnce(new Error('upgrade denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    const upgradeButtons = await screen.findAllByRole('button', { name: '升级到此套餐' })
    fireEvent.click(upgradeButtons[0])
    fireEvent.click(screen.getByRole('button', { name: '确认升级' }))

    expect(await screen.findByText(/升级套餐失败：upgrade denied/)).toBeInTheDocument()
    expect(screen.getByTestId('payment-subscription-upgrade-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('payment-subscription-upgrade-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    const dialog = screen.getByRole('dialog', { name: '确认升级套餐' })
    expect(within(dialog).getAllByText(/\/payment\/subscription\/upgrade/).length).toBeGreaterThan(0)
    expect(within(dialog).getByText(/selectedPlan=free/)).toBeInTheDocument()
    expect(within(dialog).getByText(/currentPlan=pro/)).toBeInTheDocument()
    expect(dialog).toBeInTheDocument()
  })

  it('keeps refund form values when refund create fails', async () => {
    vi.mocked(paymentApi.refundCreate).mockRejectedValueOnce(new Error('refund rejected') as never)

    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('tab', { name: '售后中心' }))
    fireEvent.click(await screen.findByRole('button', { name: '申请退款' }))
    const dialog = await screen.findByRole('dialog', { name: '申请售后退款' })
    fireEvent.change(within(dialog).getByRole('spinbutton', { name: /订单 ID/ }), { target: { value: '18' } })
    fireEvent.change(within(dialog).getByRole('spinbutton', { name: /申请退款金额/ }), { target: { value: '19.9' } })
    fireEvent.change(within(dialog).getByRole('textbox', { name: /退款原因/ }), { target: { value: '重复购买' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '提交申请' }))

    expect(await screen.findByText(/退款申请失败：refund rejected/)).toBeInTheDocument()
    expect(screen.getByTestId('payment-refund-create-error')).toHaveAttribute('data-input-retained', 'true')
    expect(within(dialog).getAllByText(/\/payment\/refund\/create/).length).toBeGreaterThan(0)
    expect(within(dialog).getByText(/orderId=18/)).toBeInTheDocument()
    expect(within(dialog).getByText(/amount=19.9/)).toBeInTheDocument()
    expect(within(dialog).getByText(/reason=重复购买/)).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('18')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('19.9')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('重复购买')).toBeInTheDocument()
  })

  it('marks refund search as ready contract while refund admin actions stay hidden', async () => {
    vi.mocked(paymentApi.refundSearch).mockResolvedValueOnce({
      total: 1,
      list: [
        {
          id: 8,
          refundNo: 'R20260522001',
          transactionNo: 'T20260522001',
          amount: 39.9,
          status: 'PENDING',
          reason: '重复购买',
          createTime: '2026-05-22 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 10,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <SubscriptionPage />
      </MemoryRouter>,
    )

    fireEvent.click(await screen.findByRole('tab', { name: '售后中心' }))

    const workbench = await screen.findByTestId('subscription-refund-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'payment-refund')
    expect(workbench).toHaveAttribute('data-refund-create-endpoint', '/payment/refund/create')
    expect(workbench).toHaveAttribute('data-refund-search-endpoint', '/payment/refund/search')
    expect(workbench).toHaveAttribute('data-no-local-refund-fallback', 'true')
    expect(workbench).toHaveAttribute('data-unsupported-actions', 'invoice-apply,invoice-list,invoice-download')
    expect(workbench).toHaveAttribute('data-no-refund-admin-actions', 'true')

    const downgrade = screen.getByTestId('payment-refund-search-downgrade')
    expect(downgrade).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(downgrade).toHaveAttribute('data-contract-status', 'ready')
    expect(downgrade).toHaveAttribute('data-contract-endpoint', '/payment/refund/search')
    expect(downgrade).toHaveTextContent('/payment/refund/search 已接入真实退款分页')
    expect(downgrade).toHaveTextContent('不补本地退款单或审批状态')

    const grid = await screen.findByTestId('payment-refund-search-grid')
    expect(grid).toHaveAttribute('data-contract-status', 'ready')
    expect(grid).toHaveAttribute('data-contract-endpoint', '/payment/refund/search')
    expect(grid).toHaveAttribute('data-refund-row-count', '1')
    expect(await screen.findByText('R20260522001')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '申请退款' }))
    const form = await screen.findByTestId('payment-refund-create-form')
    expect(form).toHaveAttribute('data-contract-status', 'ready')
    expect(form).toHaveAttribute('data-contract-endpoint', '/payment/refund/create')

    const capabilityCards = screen.getAllByTestId('payment-capability-card')
    expect(capabilityCards.some(card => card.textContent?.includes('退款申请') && card.getAttribute('data-contract-status') === 'ready')).toBe(true)
    expect(capabilityCards.some(card => card.textContent?.includes('退款列表') && card.getAttribute('data-contract-status') === 'ready')).toBe(true)
  })
})
