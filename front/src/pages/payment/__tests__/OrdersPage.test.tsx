import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor, fireEvent, within } from '@/test/utils'
import OrdersPage from '../OrdersPage'
import { paymentApi } from '@/api/payment'

vi.mock('@/api/payment', () => ({
  paymentApi: {
    list: vi.fn(),
    getByOrderNo: vi.fn(),
    confirmPayment: vi.fn(),
    ship: vi.fn(),
    complete: vi.fn(),
    cancel: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('OrdersPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(paymentApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          orderNo: 'P20260410001',
          amount: 99,
          payType: 'alipay',
          status: 'PENDING_PAYMENT',
          payTime: null,
          trackingNo: null,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(paymentApi.getByOrderNo).mockResolvedValue({
      id: 9,
      orderNo: 'P20260410009',
      amount: 299,
      payType: 'manual',
      status: 'PAID',
      payTime: '2026-04-10 11:00:00',
      trackingNo: null,
      createTime: '2026-04-10 10:59:00',
    } as never)
  })

  it('loads order list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '订单管理' })).toBeInTheDocument()
    expect(screen.getByTestId('payment-order-workbench')).toHaveAttribute('data-contract-scope', 'payment-order')
    expect(screen.getByTestId('payment-order-workbench')).toHaveAttribute(
      'data-ready-endpoints',
      '/payment/order/list,/payment/order/getByOrderNo,/payment/order/confirmPayment,/payment/order/ship,/payment/order/complete,/payment/order/cancel',
    )
    expect(screen.getByTestId('payment-order-workbench')).toHaveAttribute(
      'data-unsupported-actions',
      'payment-method-filter,order-export,invoice-apply',
    )
    expect(screen.getByTestId('payment-order-workbench')).toHaveAttribute('data-no-synthetic-transaction-id', 'true')
    expect(screen.getByTestId('payment-order-workbench')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getByTestId('payment-order-contract-downgrade')).toHaveAttribute('data-downgrade-tone', 'contract-gap')
    expect(screen.getByTestId('payment-order-contract-downgrade')).toHaveTextContent('订单导出、发票申请和支付方式筛选尚未落库')
    expect(screen.getByTestId('payment-order-contract-downgrade')).toHaveAttribute('data-no-synthetic-transaction-id', 'true')

    await waitFor(() => {
      expect(paymentApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, status: undefined })
    })

    await waitFor(() => {
      expect(screen.getByText('P20260410001')).toBeInTheDocument()
      expect(screen.getAllByText('待支付').length).toBeGreaterThan(0)
    })
  })

  it('shows retryable order list error', async () => {
    vi.mocked(paymentApi.list).mockRejectedValueOnce(new Error('orders down'))

    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/订单加载失败/)).toBeInTheDocument()
    expect(screen.getByText(/orders down/)).toBeInTheDocument()
    expect(screen.getByText(/endpoint=\/payment\/order\/list/)).toBeInTheDocument()
  })

  it('uses getByOrderNo for exact order number search', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('P20260410001')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('订单号'), { target: { value: ' P20260410009 ' } })
    fireEvent.click(screen.getByRole('button', { name: '查询' }))

    await waitFor(() => {
      expect(paymentApi.getByOrderNo).toHaveBeenCalledWith('P20260410009')
    })
    await waitFor(() => {
      expect(screen.getByText('P20260410009')).toBeInTheDocument()
    })
  })

  it('shows getByOrderNo endpoint error and keeps exact search input', async () => {
    vi.mocked(paymentApi.getByOrderNo).mockRejectedValueOnce(new Error('order not found') as never)

    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('P20260410001')).toBeInTheDocument()
    })

    fireEvent.change(screen.getByLabelText('订单号'), { target: { value: 'P404' } })
    fireEvent.click(screen.getByRole('button', { name: '查询' }))

    expect(await screen.findByText(/订单加载失败：order not found/)).toBeInTheDocument()
    expect(screen.getByText(/endpoint=\/payment\/order\/getByOrderNo/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('P404')).toBeInTheDocument()
  })

  it('opens confirm dialog before mutating order status', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('P20260410001')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(screen.getByText('确认支付')).toBeInTheDocument()
    const dialog = screen.getByRole('dialog', { name: '确认支付' })
    expect(screen.getByTestId('payment-confirm-proof-form')).toHaveAttribute('data-no-synthetic-transaction-id', 'true')
    expect(within(dialog).getByLabelText(/交易凭证/)).toBeInTheDocument()
    expect(within(dialog).getByLabelText(/支付方式/)).toHaveValue('manual')
    expect(screen.getByText(/不会生成 manual-\*/)).toBeInTheDocument()
  })

  it('submits user supplied payment proof when confirming payment fails and keeps the row unchanged', async () => {
    vi.mocked(paymentApi.confirmPayment).mockRejectedValueOnce(new Error('forbidden order') as never)

    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('P20260410001')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    const dialog = screen.getByRole('dialog', { name: '确认支付' })
    expect(within(dialog).getByText(/orderId=1/)).toBeInTheDocument()
    expect(within(dialog).getByText(/orderNo=P20260410001/)).toBeInTheDocument()
    fireEvent.change(within(dialog).getByLabelText(/交易凭证/), { target: { value: 'TXN-AUDIT-001' } })
    fireEvent.change(within(dialog).getByLabelText(/支付方式/), { target: { value: 'bank-transfer' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(paymentApi.confirmPayment).toHaveBeenCalledWith(1, {
        transactionId: 'TXN-AUDIT-001',
        paymentMethod: 'bank-transfer',
      })
    })
    expect(await screen.findByText(/确认支付失败：forbidden order/)).toBeInTheDocument()
    expect(screen.getAllByText(/\/payment\/order\/confirmPayment/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/orderId=1/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/orderNo=P20260410001/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/transactionId=TXN-AUDIT-001/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/paymentMethod=bank-transfer/).length).toBeGreaterThan(0)
    expect(screen.getByTestId('payment-order-action-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(screen.getByTestId('payment-order-action-error')).toHaveAttribute('data-no-synthetic-transaction-id', 'true')
    expect(screen.getByText('P20260410001')).toBeInTheDocument()
    expect(within(dialog).getByDisplayValue('TXN-AUDIT-001')).toBeInTheDocument()
  })

  it('keeps tracking number in dialog when shipping fails', async () => {
    vi.mocked(paymentApi.list).mockResolvedValueOnce({
      total: 1,
      list: [
        {
          id: 2,
          orderNo: 'P20260410002',
          amount: 199,
          payType: 'wechat',
          status: 'PAID',
          payTime: '2026-04-10 10:00:00',
          trackingNo: null,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(paymentApi.ship).mockRejectedValueOnce(new Error('tracking rejected') as never)

    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText('P20260410002')).toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '发货' }))
    expect(screen.getByText(/orderId=2/)).toBeInTheDocument()
    expect(screen.getByText(/orderNo=P20260410002/)).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('物流单号'), { target: { value: 'SF-KEEP' } })
    fireEvent.click(screen.getByRole('button', { name: '确认发货' }))

    expect(await screen.findByText(/发货失败：tracking rejected/)).toBeInTheDocument()
    expect(screen.getAllByText(/\/payment\/order\/ship/).length).toBeGreaterThan(0)
    expect(screen.getByText(/trackingNo=SF-KEEP/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('SF-KEEP')).toBeInTheDocument()
  })

  it('shows explicit downgrade for unsupported order filters', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/订单号精确检索来自.*payment\/order\/getByOrderNo/)).toBeInTheDocument()
    expect(screen.getByText('订单号检索')).toBeInTheDocument()
    expect(screen.getByText('支付方式筛选')).toBeInTheDocument()
    expect(screen.getByText(/OrderSearchVO 未提供 paymentMethod\/payType/)).toBeInTheDocument()
    expect(screen.getByText('导出/发票')).toBeInTheDocument()
    expect(screen.getAllByText('显式降级').length).toBeGreaterThanOrEqual(2)

    const cards = screen.getAllByTestId('payment-order-capability-card')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'order-list')).toHaveAttribute('data-contract-status', 'ready')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'order-state-machine')).toHaveAttribute('data-contract-status', 'ready')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'order-no-search')).toHaveAttribute('data-contract-endpoint', '/payment/order/getByOrderNo')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'payment-method-filter')).toHaveAttribute('data-contract-status', 'degraded')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'order-export,invoice-apply')).toHaveAttribute('data-contract-status', 'unsupported')
    expect(cards.find(card => card.getAttribute('data-contract-action') === 'order-export,invoice-apply')).toHaveAttribute('data-contract-endpoint', 'unavailable')
    expect(screen.queryByRole('button', { name: /导出/ })).not.toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /发票/ })).not.toBeInTheDocument()
  })
})
