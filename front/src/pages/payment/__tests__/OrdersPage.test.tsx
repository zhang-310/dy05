import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import OrdersPage from '../OrdersPage'
import { paymentApi } from '@/api/payment'

vi.mock('@/api/payment', () => ({
  paymentApi: {
    list: vi.fn(),
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
          status: 0,
          payTime: null,
          trackingNo: null,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads order list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <OrdersPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('订单管理')).toBeInTheDocument()

    await waitFor(() => {
      expect(paymentApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, orderNo: '', status: undefined, payType: undefined })
    })

    await waitFor(() => {
      expect(screen.getByText('P20260410001')).toBeInTheDocument()
      expect(screen.getByText('待支付')).toBeInTheDocument()
    })
  })
})
