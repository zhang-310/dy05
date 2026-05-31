import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { ProductPanel } from './ProductPanel'
import { ProductQuickSwitchBar } from './ProductQuickSwitchBar'
import type { LiveProduct } from '@/api/live-product'
import type { ProductLibraryItemVO } from '@/types/product'

const products: LiveProduct[] = [
  { id: 1, sessionId: 18, productId: 101, productName: '屏障修护精华', productType: 'hot', position: 1, createTime: '' },
  { id: 2, sessionId: 18, productId: 102, productName: '轻盈防晒乳', productType: 'profit', position: 2, createTime: '' },
]

const libraryProducts: ProductLibraryItemVO[] = [
  { id: 201, productName: '胶原面膜', price: 79 },
  { id: 202, productName: '已添加商品', price: 59 },
]

function renderPanel(overrides?: Partial<React.ComponentProps<typeof ProductPanel>>) {
  const props: React.ComponentProps<typeof ProductPanel> = {
    products,
    sortedProducts: products,
    addProductOpen: false,
    productSearch: '',
    productList: libraryProducts,
    selectedProductToAdd: null,
    addProductTypeSelected: ['flat'],
    addProductTypeLoading: false,
    sorting: false,
    addedProductIds: new Set([202]),
    onAddProductOpen: vi.fn(),
    onProductSearchChange: vi.fn(),
    onSelectProductToAdd: vi.fn(),
    onAddProductTypeChange: vi.fn(),
    onConfirmAddProduct: vi.fn(),
    onAddProduct: vi.fn(),
    onRemoveProduct: vi.fn(),
    onMoveProduct: vi.fn(),
    onUpdateProductType: vi.fn(),
    ...overrides,
  }
  renderWithProviders(<ProductPanel {...props} />)
  return props
}

describe('ProductPanel and ProductQuickSwitchBar contracts', () => {
  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('marks ProductPanel as a props bridge without direct API writes', () => {
    const props = renderPanel()

    const root = screen.getByTestId('live-product-panel-root')
    expect(root).toHaveAttribute('data-contract-scope', 'live-product-panel-props-bridge')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/product/by-session'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/product/save'))
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('direct-api-request'))
    expect(root).toHaveAttribute('data-no-direct-api-request', 'true')
    expect(root).toHaveAttribute('data-product-count', '2')

    fireEvent.click(screen.getByTestId('live-product-panel-add-open-button'))
    fireEvent.click(screen.getAllByTestId('live-product-panel-move-down-button')[0])
    fireEvent.click(screen.getAllByTestId('live-product-panel-remove-button')[0])
    fireEvent.click(screen.getAllByTestId('live-product-panel-type-chip')[0])

    expect(props.onAddProductOpen).toHaveBeenCalledWith(true)
    expect(props.onMoveProduct).toHaveBeenCalledWith(products[0], 'down')
    expect(props.onRemoveProduct).toHaveBeenCalledWith(products[0])
    expect(props.onUpdateProductType).toHaveBeenCalledWith(products[0], expect.arrayContaining(['control']))
  })

  it('marks empty ProductPanel state without local product fallback', () => {
    renderPanel({ products: [], sortedProducts: [] })

    expect(screen.getByTestId('live-product-panel-root')).toHaveAttribute('data-product-count', '0')
    expect(screen.getByTestId('live-product-panel-empty')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.queryByTestId('live-product-panel-row')).not.toBeInTheDocument()
  })

  it('filters already-added library products and keeps add dialog props-only', () => {
    const props = renderPanel({ addProductOpen: true })

    const dialog = screen.getByTestId('live-product-panel-add-dialog')
    expect(dialog).toHaveAttribute('data-contract-scope', 'live-product-panel-library-selector')
    expect(dialog).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(dialog).toHaveAttribute('data-library-count', '1')
    expect(screen.getAllByTestId('live-product-panel-library-row')).toHaveLength(1)
    expect(screen.getByText('胶原面膜')).toBeInTheDocument()
    expect(screen.queryByText('已添加商品')).not.toBeInTheDocument()

    fireEvent.change(screen.getByTestId('live-product-panel-search-input'), { target: { value: '面膜' } })
    fireEvent.click(screen.getByTestId('live-product-panel-library-row'))

    expect(props.onProductSearchChange).toHaveBeenCalledWith('面膜')
    expect(props.onAddProduct).toHaveBeenCalledWith(libraryProducts[0])
  })

  it('shows library empty state without local library fallback', () => {
    renderPanel({ addProductOpen: true, addedProductIds: new Set([201, 202]) })

    expect(screen.getByTestId('live-product-panel-add-dialog')).toHaveAttribute('data-library-count', '0')
    expect(screen.getByTestId('live-product-panel-library-empty')).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(screen.queryByTestId('live-product-panel-library-row')).not.toBeInTheDocument()
  })

  it('confirms selected library product through parent callbacks and preserves type changes', () => {
    const props = renderPanel({
      addProductOpen: true,
      selectedProductToAdd: libraryProducts[0],
      addProductTypeSelected: ['flat'],
    })

    expect(screen.getByTestId('live-product-panel-add-dialog')).toHaveAttribute('data-selected-product-id', '201')
    expect(screen.getByTestId('live-product-panel-add-confirm-surface')).toHaveAttribute('data-contract-source', 'onConfirmAddProduct-prop')

    fireEvent.click(screen.getAllByTestId('live-product-panel-add-type-chip')[0])
    fireEvent.click(screen.getByTestId('live-product-panel-confirm-add-button'))
    fireEvent.click(screen.getByTestId('live-product-panel-add-back-button'))

    expect(props.onAddProductTypeChange).toHaveBeenCalled()
    expect(props.onConfirmAddProduct).toHaveBeenCalled()
    expect(props.onSelectProductToAdd).toHaveBeenCalledWith(null)
  })

  it('marks quick switch bar as props-only and handles empty context', () => {
    const onProductClick = vi.fn()
    renderWithProviders(
      <ProductQuickSwitchBar
        products={products as never}
        highlightedProductId={102}
        onProductClick={onProductClick}
      />,
    )

    expect(screen.getByTestId('product-quick-switch-bar')).toHaveAttribute('data-contract-scope', 'live-product-quick-switch-props')
    expect(screen.getByTestId('product-quick-switch-bar')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.getByTestId('product-quick-switch-active-chip')).toHaveAttribute('data-contract-product-id', '102')
    fireEvent.click(screen.getByTestId('product-quick-switch-active-chip'))
    expect(onProductClick).toHaveBeenCalledWith(102)
  })

  it('renders quick switch empty state instead of hiding fallback context', () => {
    renderWithProviders(
      <ProductQuickSwitchBar
        products={[]}
        highlightedProductId={null}
        onProductClick={vi.fn()}
      />,
    )

    expect(screen.getByTestId('product-quick-switch-bar')).toHaveAttribute('data-product-count', '0')
    expect(screen.getByTestId('product-quick-switch-empty')).toHaveAttribute('data-no-local-product-fallback', 'true')
  })
})
