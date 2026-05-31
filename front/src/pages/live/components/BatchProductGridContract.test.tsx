import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { BatchProductDialog, type BatchSelectedProduct } from './BatchProductDialog'
import { ProductGridView } from './ProductGridView'
import { productApi } from '@/api/product'
import type { LiveProduct } from '@/api/live-product'

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
  },
}))

const libraryProducts = [
  { id: 201, productName: '屏障修护精华', price: 129, category: '护肤', mainImage: '', sellingPoints: '修护屏障' },
  { id: 202, productName: '轻盈防晒乳', price: 89, category: '防晒', mainImage: '', sellingPoints: '不泛白' },
]

const liveProducts: LiveProduct[] = [
  {
    id: 1,
    productId: 201,
    productName: '屏障修护精华',
    productType: 'hot,profit',
    imageUrl: '',
    price: 129,
    aiSellingPoints: '屏障修护、舒缓泛红',
    productCategory: '护肤',
    profitMarginPct: 42,
  },
  {
    id: 2,
    productId: 202,
    productName: '资料不完整商品',
    productType: 'flat',
    imageUrl: '',
    price: 0,
  },
] as LiveProduct[]

describe('BatchProductDialog and ProductGridView contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(productApi.list).mockResolvedValue({
      list: libraryProducts,
      total: 2,
      page: 0,
      rows: 50,
    } as never)
  })

  it('marks batch product dialog endpoints and empty selection disabled reason', async () => {
    renderWithProviders(
      <BatchProductDialog open onClose={vi.fn()} onConfirm={vi.fn()} existingProductIds={new Set()} />,
    )

    const dialog = await screen.findByTestId('live-select-batch-product-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-batch-product-library-dialog')
    expect(dialog).toHaveAttribute('data-ready-endpoints', '/product/search|/live/product/batch-add')
    expect(dialog).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-product-library-fallback'))
    expect(dialog).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(await screen.findAllByTestId('batch-product-library-row')).toHaveLength(2)
    expect(screen.getByTestId('batch-product-confirm-button')).toHaveAttribute('data-disabled-reason', 'empty-selection')
    expect(productApi.list).toHaveBeenCalledWith(expect.objectContaining({ page: 0, rows: 50 }))
  })

  it('selects, retags, removes, and confirms products through props payload', async () => {
    const onConfirm = vi.fn()
    renderWithProviders(
      <BatchProductDialog open onClose={vi.fn()} onConfirm={onConfirm} existingProductIds={new Set()} />,
    )

    fireEvent.click((await screen.findAllByTestId('batch-product-library-row'))[0])
    expect(screen.getByTestId('live-select-batch-product-dialog')).toHaveAttribute('data-selected-count', '1')
    expect(screen.getByTestId('batch-product-selected-row')).toHaveAttribute('data-product-id', '201')
    expect(screen.getByTestId('batch-product-confirm-button')).toHaveAttribute('data-disabled-reason', 'ready')

    fireEvent.click(screen.getAllByTestId('batch-product-selected-type-chip').find((chip) => chip.getAttribute('data-contract-product-type') === 'hot')!)
    expect(screen.getByTestId('batch-product-selected-row')).toHaveAttribute('data-product-type', 'hot')

    fireEvent.click(screen.getByTestId('batch-product-confirm-button'))
    expect(onConfirm).toHaveBeenCalledWith([
      expect.objectContaining<Partial<BatchSelectedProduct>>({
        productId: 201,
        productName: '屏障修护精华',
        productType: 'hot',
        price: 129,
      }),
    ])

    fireEvent.click(screen.getByTestId('batch-product-selected-remove-button'))
    await waitFor(() => {
      expect(screen.getByTestId('live-select-batch-product-dialog')).toHaveAttribute('data-selected-count', '0')
    })
    expect(screen.getByTestId('batch-product-selected-empty-state')).toBeInTheDocument()
  })

  it('shows product library errors and true empty states without local fallback', async () => {
    vi.mocked(productApi.list).mockRejectedValue(new Error('search down') as never)

    renderWithProviders(
      <BatchProductDialog open onClose={vi.fn()} onConfirm={vi.fn()} existingProductIds={new Set()} />,
    )

    expect(await screen.findByTestId('batch-product-search-error')).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(screen.getByTestId('live-select-batch-product-dialog')).toHaveAttribute('data-load-state', 'error')
    expect(screen.queryByTestId('batch-product-library-row')).not.toBeInTheDocument()

    vi.mocked(productApi.list).mockResolvedValue({ list: [], total: 0, page: 0, rows: 50 } as never)
    renderWithProviders(
      <BatchProductDialog open onClose={vi.fn()} onConfirm={vi.fn()} existingProductIds={new Set()} />,
    )

    expect(await screen.findByTestId('batch-product-library-empty-state')).toHaveAttribute('data-no-local-product-library-fallback', 'true')
  })

  it('marks product grid as readonly and delegates card clicks to props', () => {
    const onProductClick = vi.fn()
    const counts = new Map([[201, 3]])
    renderWithProviders(
      <ProductGridView
        products={liveProducts}
        highlightedProductId={201}
        productScriptCounts={counts}
        onProductClick={onProductClick}
      />,
    )

    const root = screen.getByTestId('product-grid-view')
    expect(root).toHaveAttribute('data-contract-scope', 'live-product-grid-readonly')
    expect(root).toHaveAttribute('data-ready-endpoints', '/live/product/by-session|/live/script/by-session')
    expect(root).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(root).toHaveAttribute('data-click-owner', 'onProductClick-prop')
    expect(screen.getByTestId('product-grid-active-card-surface')).toHaveAttribute('data-script-count', '3')
    expect(screen.getAllByTestId('product-grid-type-chip')).toHaveLength(3)
    expect(screen.getByTestId('product-grid-missing-fields-banner-surface')).toHaveAttribute('data-missing-fields', expect.stringContaining('卖点'))

    fireEvent.click(screen.getAllByTestId('product-grid-card-action')[1])
    expect(onProductClick).toHaveBeenCalledWith(202)
  })

  it('keeps product grid empty state visible without local products', () => {
    renderWithProviders(<ProductGridView products={[]} />)

    expect(screen.getByTestId('product-grid-view')).toHaveAttribute('data-product-count', '0')
    expect(screen.getByTestId('product-grid-empty-state')).toHaveAttribute('data-contract-scope', 'live-product-grid-empty')
    expect(screen.getByTestId('product-grid-empty-state')).toHaveAttribute('data-no-local-product-list-fallback', 'true')
    expect(screen.queryByTestId('product-grid-card-surface')).not.toBeInTheDocument()
  })
})
