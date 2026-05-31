import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, screen, waitFor } from '@testing-library/react'
import { renderWithProviders } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { SelectTabContent, reorderLiveProductRelationIdsForDrag } from './SelectTabContent'
import { BatchProductDialog } from './BatchProductDialog'
import { ProductGridView } from './ProductGridView'
import { PRODUCT_TYPE_COLOR_MAP, PRODUCT_TYPE_COLOR_TONES, PRODUCT_TYPE_OPTIONS, getProductTypeThemeColor } from './constants'
import { productApi } from '@/api/product'
import { liveApi } from '@/api/live'
import { createTheme } from '@mui/material/styles'

const toast = vi.hoisted(() => vi.fn())
const refetchProducts = vi.hoisted(() => vi.fn())
const coreDataState = vi.hoisted(() => ({
  products: [
    {
      id: 1,
      productId: 101,
      productName: '高保湿修护精华',
      productType: 'hot',
      price: 129,
      imageUrl: '',
      aiSellingPoints: '强修护',
      productCategory: '护肤',
    },
    {
      id: 2,
      productId: 102,
      productName: '轻盈防晒乳',
      productType: 'profit',
      price: 89,
      imageUrl: '',
      aiSellingPoints: '不泛白',
      productCategory: '防晒',
    },
  ] as Array<{
    id: number
    productId: number
    productName: string
    productType: string
    price: number
    imageUrl: string
    aiSellingPoints: string
    productCategory: string
  }>,
  scripts: [{ id: 1, productId: 101, scriptContent: '修护卖点话术' }] as Array<{ id: number; productId: number; scriptContent: string }>,
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
  },
}))

vi.mock('@/api/live', () => ({
  liveApi: {
    productDelete: vi.fn(),
    productBatchAdd: vi.fn(),
    productAiSortSuggest: vi.fn(),
    productBatchSort: vi.fn(),
  },
}))

vi.mock('../contexts', () => ({
  useCoreData: () => ({
    products: coreDataState.products,
    scripts: coreDataState.scripts,
    refetchProducts,
  }),
}))

function renderDark(ui: React.ReactElement) {
  window.localStorage.setItem('dy-theme-mode', 'dark')
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

describe('live product selection surfaces', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    coreDataState.products = [
      {
        id: 1,
        productId: 101,
        productName: '高保湿修护精华',
        productType: 'hot',
        price: 129,
        imageUrl: '',
        aiSellingPoints: '强修护',
        productCategory: '护肤',
      },
      {
        id: 2,
        productId: 102,
        productName: '轻盈防晒乳',
        productType: 'profit',
        price: 89,
        imageUrl: '',
        aiSellingPoints: '不泛白',
        productCategory: '防晒',
      },
    ]
    coreDataState.scripts = [{ id: 1, productId: 101, scriptContent: '修护卖点话术' }]
    vi.mocked(productApi.list).mockResolvedValue({
      list: [
        { id: 201, productName: '无图口红', price: 59, category: '彩妆', mainImage: '' },
      ],
      total: 1,
      page: 0,
      rows: 50,
    } as never)
  })

  it('uses a theme-aware selected product highlight in dark mode', () => {
    renderDark(<SelectTabContent sessionId={18} />)

    fireEvent.click(screen.getAllByText('高保湿修护精华')[0])

    expect(screen.getByTestId('select-product-highlight-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })
  })

  it('marks the selection workbench contract and keeps filtering as a view-only empty state', () => {
    renderDark(<SelectTabContent sessionId={18} onNext={vi.fn()} />)

    const root = screen.getByTestId('live-select-tab-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'live-product-selection-sorting')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/live/product/batch-add'))
    expect(root).toHaveAttribute('data-context-endpoints', '/live/session/get|/live/product/by-session|/live/script/by-session')
    expect(root).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-ai-sort-fallback'))
    expect(root).toHaveAttribute('data-product-count', '2')
    expect(root).toHaveAttribute('data-script-count', '1')
    expect(screen.getByTestId('live-select-toolbar')).toHaveAttribute('data-contract-source', expect.stringContaining('/live/ai/sort-suggest'))

    fireEvent.click(screen.getAllByTestId('live-select-type-filter-chip')[0])

    expect(screen.getByTestId('live-select-tab-workbench')).toHaveAttribute('data-active-type-filter', 'hot')
    expect(screen.getAllByTestId('product-grid-card-surface')).toHaveLength(1)
    expect(screen.queryByText('当前筛选没有商品')).not.toBeInTheDocument()
  })

  it('marks an empty live product context without injecting local products', () => {
    coreDataState.products = []
    coreDataState.scripts = []

    renderDark(<SelectTabContent sessionId={18} />)

    expect(screen.getByTestId('live-select-tab-workbench')).toHaveAttribute('data-product-count', '0')
    expect(screen.getByTestId('live-select-empty-state')).toHaveAttribute('data-no-local-product-list-fallback', 'true')
    expect(screen.queryByTestId('product-grid-card-surface')).not.toBeInTheDocument()
  })

  it('does not create a local AI sort suggestion when /live/ai/sort-suggest fails', async () => {
    vi.mocked(liveApi.productAiSortSuggest).mockRejectedValue(new Error('sort down'))

    renderDark(<SelectTabContent sessionId={18} />)

    fireEvent.click(screen.getByTestId('sort-strategy-ai-button'))

    expect(await screen.findByTestId('live-select-ai-sort-error')).toHaveAttribute('data-no-local-ai-sort-fallback', 'true')
    expect(screen.getByTestId('live-select-tab-workbench')).toHaveAttribute('data-ai-sort-state', 'error')
    expect(screen.queryByTestId('live-select-ai-sort-suggestion')).not.toBeInTheDocument()
    expect(liveApi.productBatchSort).not.toHaveBeenCalled()
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('/live/ai/sort-suggest AI 排品失败'), 'error')
  })

  it('applies only backend AI sort suggestions and reports batch-sort failures without local reordering', async () => {
    vi.mocked(liveApi.productAiSortSuggest).mockResolvedValue({
      productIds: [102, 101, 999],
      reason: '优先利润款承接，再用爆品拉转化',
    })
    vi.mocked(liveApi.productBatchSort).mockRejectedValue(new Error('sort write down'))

    renderDark(<SelectTabContent sessionId={18} />)

    fireEvent.click(screen.getByTestId('sort-strategy-ai-button'))
    expect(await screen.findByTestId('live-select-ai-sort-suggestion')).toHaveAttribute('data-ai-sort-fallback', 'false')

    fireEvent.click(screen.getByRole('button', { name: '应用' }))

    await waitFor(() => {
      expect(liveApi.productBatchSort).toHaveBeenCalledWith(18, [2, 1])
    })
    expect(await screen.findByTestId('live-select-ai-sort-error')).toHaveTextContent('/live/product/batch-sort 应用 AI 排序失败')
    expect(refetchProducts).not.toHaveBeenCalled()
  })

  it('maps manual drag sorting to live product relation ids and disables filtered drag writes', () => {
    expect(reorderLiveProductRelationIdsForDrag(coreDataState.products, 1, 2)).toEqual([2, 1])
    expect(reorderLiveProductRelationIdsForDrag(coreDataState.products, '1', '2')).toEqual([2, 1])
    expect(reorderLiveProductRelationIdsForDrag(coreDataState.products, 1, 1)).toEqual([1, 2])

    renderDark(<SelectTabContent sessionId={18} />)

    expect(screen.getByTestId('live-select-list-surface')).toHaveAttribute('data-drag-sort-enabled', 'true')
    expect(screen.getAllByTestId('live-select-product-drag-handle')[0]).toHaveAttribute('data-contract-source', '/live/product/batch-sort')

    fireEvent.click(screen.getAllByTestId('live-select-type-filter-chip')[0])
    expect(screen.getByTestId('live-select-list-surface')).toHaveAttribute('data-drag-sort-enabled', 'false')
    expect(screen.getByTestId('live-select-drag-sort-hint')).toHaveAttribute('data-contract-source', '/live/product/batch-sort')
  })

  it('submits batch add through live product endpoint and keeps dialog open on failure', async () => {
    vi.mocked(liveApi.productBatchAdd).mockRejectedValue(new Error('batch add down'))

    renderDark(<SelectTabContent sessionId={18} />)

    fireEvent.click(screen.getByTestId('live-select-batch-add-button'))
    expect(await screen.findByTestId('live-select-batch-product-dialog')).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    fireEvent.click((await screen.findAllByText('无图口红'))[0])
    fireEvent.click(screen.getByTestId('batch-product-confirm-button'))

    await waitFor(() => {
      expect(liveApi.productBatchAdd).toHaveBeenCalledWith(18, [{
        productId: 201,
        productName: '无图口红',
        productType: 'flat',
        imageUrl: '',
        price: 59,
      }])
    })
    expect(screen.getByTestId('live-select-batch-product-dialog')).toBeInTheDocument()
    expect(toast).toHaveBeenCalledWith(expect.stringContaining('/live/product/batch-add 批量添加商品失败'), 'error')
  })

  it('shows product library search errors without local product fallback', async () => {
    vi.mocked(productApi.list).mockRejectedValue(new Error('search down'))

    renderDark(<SelectTabContent sessionId={18} />)

    fireEvent.click(screen.getByTestId('live-select-batch-add-button'))

    expect(await screen.findByTestId('batch-product-search-error')).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(screen.getByTestId('batch-product-library-list')).toHaveAttribute('data-no-local-product-library-fallback', 'true')
    expect(screen.queryByTestId('batch-product-library-row')).not.toBeInTheDocument()
  })

  it('uses a theme-aware product grid image placeholder in dark mode', () => {
    renderDark(
      <ProductGridView
        products={[
          {
            id: 1,
            productId: 101,
            productName: '无图商品',
            productType: 'hot',
            imageUrl: '',
          },
        ]}
      />,
    )

    expect(screen.getByTestId('product-grid-image-placeholder-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })
  })

  it('uses theme-aware active card and missing-field banner in dark mode', () => {
    renderDark(
      <ProductGridView
        highlightedProductId={101}
        products={[
          {
            id: 1,
            productId: 101,
            productName: '资料不完整商品',
            productType: 'hot',
            imageUrl: '',
          },
        ]}
      />,
    )

    expect(screen.getByTestId('product-grid-active-card-surface')).toHaveAttribute('data-active', 'true')
    expect(screen.getByTestId('product-grid-missing-fields-banner-surface')).toHaveAttribute('data-missing-tone', 'warning')

    const banner = screen.getByTestId('product-grid-missing-fields-banner-surface')
    expect(banner).not.toHaveStyle({ backgroundColor: 'rgba(237, 108, 2, 0.9)' })
    expect(banner).not.toHaveStyle({ color: 'rgb(255, 255, 255)' })

    for (const legacy of ['rgba(25,118,210,0.3)', 'rgba(237,108,2,0.9)', '#fff']) {
      expect(document.body.textContent).not.toContain(legacy)
    }
  })

  it('uses theme-aware batch dialog image placeholders in dark mode', async () => {
    renderDark(
      <BatchProductDialog
        open
        onClose={vi.fn()}
        onConfirm={vi.fn()}
        existingProductIds={new Set()}
      />,
    )

    expect(await screen.findByTestId('batch-product-image-placeholder-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })

    fireEvent.click((await screen.findAllByText('无图口红'))[0])

    await waitFor(() => {
      expect(screen.getByTestId('batch-product-selected-placeholder-surface')).not.toHaveStyle({
        backgroundColor: 'rgb(245, 245, 245)',
      })
    })
  })

  it('keeps product type color configuration semantic instead of fixed legacy hex tokens', () => {
    expect(PRODUCT_TYPE_OPTIONS.map(option => [option.value, option.color])).toEqual([
      ['hot', 'error'],
      ['control', 'warning'],
      ['profit', 'success'],
      ['loss', 'info'],
      ['flat', 'default'],
    ])
    expect(PRODUCT_TYPE_COLOR_TONES).toEqual({
      hot: 'error',
      control: 'warning',
      profit: 'success',
      loss: 'info',
      flat: 'default',
    })

    const serialized = JSON.stringify(PRODUCT_TYPE_COLOR_MAP)
    for (const legacy of ['#ef5350', '#ff9800', '#66bb6a', '#42a5f5', '#9e9e9e']) {
      expect(serialized).not.toContain(legacy)
    }

    const darkTheme = createTheme({ palette: { mode: 'dark' } })
    expect(getProductTypeThemeColor(darkTheme, 'hot')).toBe(darkTheme.palette.error.light)
    expect(getProductTypeThemeColor(darkTheme, 'flat')).toBe(darkTheme.palette.text.secondary)
    expect(getProductTypeThemeColor(darkTheme, 'unknown')).toBe(darkTheme.palette.text.secondary)
    expect(getProductTypeThemeColor(darkTheme, 'control', 'soft')).toContain('rgba')
  })
})
