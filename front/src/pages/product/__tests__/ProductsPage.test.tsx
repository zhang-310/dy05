import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import ProductsPage from '../ProductsPage'
import { productApi } from '@/api/product'

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    scriptGenerate: vi.fn(),
  },
  exportProductToShortVideo: vi.fn(),
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(productApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          productName: '修护精华',
          category: '护肤',
          brand: '示例品牌',
          price: 199,
          costPrice: 80,
          profitMarginPct: 0.55,
          inventory: 120,
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads products and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(productApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('修护精华')).toBeInTheDocument()
      expect(screen.getByText('护肤')).toBeInTheDocument()
    })
  })
})
