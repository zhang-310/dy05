import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ProductReadinessPage from '../ProductReadinessPage'
import { productApi } from '@/api/product'

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
    readiness: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

const products = {
  total: 2,
  list: [
    { id: 1, productName: '修护精华', price: 199, status: 1 },
    { id: 2, productName: '保湿面霜', price: 99, status: 1 },
  ],
  pageNum: 0,
  pageSize: 200,
}

const readinessResult = {
  productId: 1,
  productName: '修护精华',
  overallScore: 72,
  readyForLive: false,
  items: [
    { dimension: '卖点完整', status: 'ok', message: '已补充', score: 85 },
    { dimension: '库存充足', status: 'warn', message: '库存偏低', score: 58 },
  ],
}

function renderPage(initialEntry = '/admin/product/readiness') {
  return renderWithProviders(
    <MemoryRouter initialEntries={[initialEntry]}>
      <ProductReadinessPage />
    </MemoryRouter>,
  )
}

describe('ProductReadinessPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(productApi.list).mockResolvedValue(products as never)
    vi.mocked(productApi.readiness).mockResolvedValue(readinessResult as never)
  })

  it('loads product options and runs readiness from in-page selector', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: '商品上播准备度' })).toBeInTheDocument()
    expect(screen.getByTestId('product-readiness-workbench')).toHaveAttribute('data-contract-scope', 'product-readiness')
    expect(screen.getByTestId('product-readiness-workbench')).toHaveAttribute('data-ready-endpoints', '/product/search|/product/readiness')
    expect(screen.getByTestId('product-readiness-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('auto-generate-product-script'))
    expect(screen.getByTestId('product-readiness-contract-alert')).toHaveAttribute('data-no-auto-script-generation', 'true')
    expect(screen.getByTestId('product-readiness-contract-alert')).toHaveAttribute('data-no-live-session-mutation', 'true')
    expect(screen.getByTestId('product-readiness-selector-card')).toHaveAttribute('data-contract-source', '/product/search')
    expect(screen.getByText(/\/product\/search/)).toBeInTheDocument()
    expect(screen.getByText('请选择商品后执行准备度检测。')).toBeInTheDocument()

    await waitFor(() => {
      expect(productApi.list).toHaveBeenCalledWith({ page: 0, rows: 200 })
    })
    expect(productApi.readiness).not.toHaveBeenCalled()

    await screen.findByRole('option', { name: '修护精华' })
    fireEvent.change(screen.getByLabelText('选择商品'), { target: { value: '1' } })

    await waitFor(() => {
      expect(productApi.readiness).toHaveBeenCalledWith(1)
    })
    expect(await screen.findByText('库存偏低')).toBeInTheDocument()
    expect(screen.getByTestId('product-readiness-result-card')).toHaveAttribute('data-contract-source', '/product/readiness')
    expect(screen.getAllByTestId('product-readiness-kpi-card')[1]).toHaveAttribute('data-contract-source', 'local-derived-from-readiness')
    expect(screen.getAllByTestId('product-readiness-check-item')[1]).toHaveAttribute('data-status', 'warn')
    expect(screen.getByText('商品仍有阻塞项，先处理检测项里的红色告警。')).toBeInTheDocument()
  })

  it('keeps productId deep links working', async () => {
    renderPage('/admin/product/readiness?productId=1')

    await waitFor(() => {
      expect(productApi.readiness).toHaveBeenCalledWith(1)
    })
    expect(screen.getByTestId('product-readiness-workbench')).toHaveAttribute('data-selected-from-url', 'true')
    await waitFor(() => {
      expect(screen.getAllByText('修护精华').length).toBeGreaterThan(0)
    })
    expect(screen.getByText('提醒项')).toBeInTheDocument()
  })

  it('shows retryable product-list and readiness errors', async () => {
    vi.mocked(productApi.list).mockRejectedValueOnce(new Error('product search failed') as never)
    vi.mocked(productApi.readiness).mockRejectedValueOnce(new Error('readiness failed') as never)

    renderPage('/admin/product/readiness?productId=1')

    expect(await screen.findByTestId('product-readiness-products-error')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.getByText('商品列表加载失败')).toBeInTheDocument()
    expect(screen.getByText(/product search failed/)).toBeInTheDocument()
    expect(await screen.findByTestId('product-readiness-error')).toHaveAttribute('data-no-static-readiness-fallback', 'true')
    expect(screen.getByText(/商品准备度加载失败/)).toBeInTheDocument()
    expect(screen.getByText(/readiness failed/)).toBeInTheDocument()
  })

  it('shows empty readiness items as backend contract degradation', async () => {
    vi.mocked(productApi.readiness).mockResolvedValueOnce({
      productId: 2,
      productName: '保湿面霜',
      overallScore: 88,
      readyForLive: true,
      items: [],
    } as never)

    renderPage('/admin/product/readiness?productId=2')

    expect(await screen.findByTestId('product-readiness-empty-items')).toHaveAttribute('data-no-static-readiness-fallback', 'true')
    expect(screen.getByTestId('product-readiness-items-card')).toHaveAttribute('data-item-count', '0')
    expect(screen.getByText(/后端未返回检测项/)).toBeInTheDocument()
  })
})
