import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ProductsPage from '../ProductsPage'
import { exportProductToShortVideo, productApi } from '@/api/product'

vi.mock('@/utils/echarts-registry', () => ({
  LazyECharts: ({ option }: { option: unknown }) => (
    <div data-testid="mock-echarts">{JSON.stringify(option)}</div>
  ),
}))

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    batchDelete: vi.fn(),
    scriptGenerate: vi.fn(),
    scriptList: vi.fn(),
    effectivenessRanking: vi.fn(),
    scriptUsageList: vi.fn(),
    salesHistorySearch: vi.fn(),
    extractFromLink: vi.fn(),
  },
  exportProductToShortVideo: vi.fn(),
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('ProductsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
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
          mainImage: '',
          imageUrl: '',
          productCode: 'SKU-001',
          unit: '件',
          description: '屏障修护精华',
          sellingPoints: '修护屏障\n敏感肌可用',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(productApi.scriptList).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 50 } as never)
    vi.mocked(productApi.effectivenessRanking).mockResolvedValue({
      total: 1,
      list: [
        {
          productId: 1,
          productName: '春季直播',
          avgScore: 8.6,
          useCount: 12,
          conversionRate: 0.18,
          tag: '高转化',
          trend: 'up',
        },
      ],
      summary: { dates: [], avgScores: [] },
    } as never)
    vi.mocked(productApi.scriptUsageList).mockResolvedValue([
      { sessionTitle: '直播场次 A', totalGmv: 68000, totalOrders: 42, sessionId: 8, versionLabel: 'v1 -> v2', gmvLift: 18 },
    ] as never)
    vi.mocked(productApi.salesHistorySearch).mockResolvedValue({
      total: 2,
      list: [
        { id: 1, productId: 1, saleAmount: 42000, channelSource: '直播间', createTime: '2026-04-11 10:00:00' },
        { id: 2, productId: 1, saleAmount: 26000, channelSource: '短视频', createTime: '2026-04-12 10:00:00' },
      ],
      pageNum: 0,
      pageSize: 1000,
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

    expect(screen.getByTestId('products-workbench')).toHaveAttribute('data-contract-scope', 'product-assets')
    expect(screen.getByTestId('products-workbench')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/product/search'))
    expect(screen.getByTestId('products-workbench')).toHaveAttribute('data-context-endpoints', expect.stringContaining('/product/script-effectiveness/ranking'))
    expect(screen.getByTestId('products-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('store-batch-sync'))
    expect(screen.getByTestId('products-workbench')).toHaveAttribute('data-no-static-product-fallback', 'true')
    expect(screen.getByTestId('products-contract-alert')).toHaveAttribute('data-no-store-sync-endpoint', 'true')
    expect(screen.getByTestId('products-contract-alert')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.getAllByTestId('product-kpi-card')[0]).toHaveAttribute('data-contract-source', '/product/search')
    expect(screen.getAllByTestId('product-capability-card')).toHaveLength(4)
    expect(screen.getAllByTestId('product-capability-card')[2]).toHaveAttribute('data-no-store-sync-endpoint', 'true')
    expect(screen.getByText('商品资产')).toBeInTheDocument()
    expect(screen.getByText('店铺同步')).toBeInTheDocument()
    expect(screen.getByText(/未发现真实同步端点/)).toBeInTheDocument()
    expect(screen.getAllByRole('button', { name: /链接提取/ }).length).toBeGreaterThan(0)
    expect(screen.getByRole('button', { name: '立即提取' })).toBeInTheDocument()
    expect(screen.getByText('商品总数')).toBeInTheDocument()
  })

  it('opens link extraction from capability card action', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(productApi.list).toHaveBeenCalled()
    })

    await user.click(screen.getByRole('button', { name: '立即提取' }))

    expect(await screen.findByRole('dialog', { name: '商品链接提取' })).toBeInTheDocument()
  })

  it('extracts product link and pre-fills add product form', async () => {
    const user = userEvent.setup()
    vi.mocked(productApi.extractFromLink).mockResolvedValue({
      productName: '链接提取精华',
      imageUrl: 'https://img.example/extract.png',
      description: '从商品详情页抓取的商品描述',
      aiSellingPoints: '修护屏障\n敏感肌可用',
    })

    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(productApi.list).toHaveBeenCalled()
    })

    await user.click(screen.getAllByRole('button', { name: /链接提取/ })[0])
    const extractDialog = await screen.findByRole('dialog', { name: '商品链接提取' })
    expect(within(extractDialog).getByTestId('product-link-extract-dialog')).toHaveAttribute('data-contract-source', '/product/extract-from-link')
    expect(within(extractDialog).getByTestId('product-link-extract-contract-alert')).toHaveAttribute('data-no-store-sync-endpoint', 'true')

    await user.type(within(extractDialog).getByLabelText('商品链接'), 'https://shop.example/item/9')
    await user.click(within(extractDialog).getByRole('button', { name: /开始提取/ }))

    await waitFor(() => {
      expect(productApi.extractFromLink).toHaveBeenCalledWith('https://shop.example/item/9')
    })
    expect(await within(extractDialog).findByText('链接提取精华')).toBeInTheDocument()
    expect(within(extractDialog).getByTestId('product-link-extract-result')).toHaveAttribute('data-contract-source', '/product/extract-from-link')
    expect(within(extractDialog).getByText(/修护屏障/)).toBeInTheDocument()

    await user.click(within(extractDialog).getByRole('button', { name: '填入新增商品' }))

    const formDialog = await screen.findByRole('dialog', { name: '新增商品' })
    expect(within(formDialog).getByLabelText(/商品名称/)).toHaveValue('链接提取精华')
    expect(within(formDialog).getByLabelText('商品链接')).toHaveValue('https://shop.example/item/9')
    expect(within(formDialog).getByLabelText('主图链接')).toHaveValue('https://img.example/extract.png')
    expect(within(formDialog).getByLabelText('商品描述')).toHaveValue('从商品详情页抓取的商品描述')
    expect(within(formDialog).getByLabelText('卖点')).toHaveValue('修护屏障\n敏感肌可用')
  })

  it('shows retryable product list error', async () => {
    vi.mocked(productApi.list).mockRejectedValueOnce(new Error('product search down'))

    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/商品列表加载失败/)).toBeInTheDocument()
    expect(screen.getByTestId('products-list-error')).toHaveAttribute('data-contract-source', '/product/search')
    expect(screen.getByTestId('products-list-error')).toHaveAttribute('data-no-static-product-fallback', 'true')
    expect(screen.getByText(/product search down/)).toBeInTheDocument()
    expect(screen.getByText(/POST \/product\/search/)).toBeInTheDocument()
    expect(screen.getByText(/route=.*\/product\/list; keyword=空; category=全部; status=全部; page=0; rows=20/)).toBeInTheDocument()
  })

  it('shows save endpoint context and keeps add form input on failure', async () => {
    const user = userEvent.setup()
    vi.mocked(productApi.save).mockRejectedValue(new Error('save down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await screen.findByText('修护精华')
    await user.click(screen.getAllByRole('button', { name: '新增商品' })[0])

    const dialog = await screen.findByRole('dialog', { name: '新增商品' })
    expect(within(dialog).getByTestId('product-save-dialog')).toHaveAttribute('data-contract-source', '/product/save')
    expect(within(dialog).getByTestId('product-save-dialog')).toHaveAttribute('data-input-preserved', 'true')
    await user.clear(within(dialog).getByLabelText(/商品名称/))
    await user.type(within(dialog).getByLabelText(/商品名称/), '失败商品')
    await user.type(within(dialog).getByLabelText('商品编码'), 'SKU-FAIL')
    await user.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByText(/商品保存失败（POST \/product\/save）：save down/)).toBeInTheDocument()
    expect(within(dialog).getByTestId('product-save-error')).toHaveAttribute('data-input-preserved', 'true')
    expect(within(dialog).getByText(/productName=失败商品/)).toBeInTheDocument()
    expect(within(dialog).getByText(/sku=SKU-FAIL/)).toBeInTheDocument()
    expect(within(dialog).getByLabelText(/商品名称/)).toHaveValue('失败商品')
  })

  it('shows delete endpoint context and keeps row on failure', async () => {
    const user = userEvent.setup()
    vi.mocked(productApi.delete).mockRejectedValue(new Error('delete down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await screen.findByText('修护精华')
    await user.click(screen.getByRole('button', { name: '删除' }))
    await user.click(await screen.findByRole('button', { name: '确认' }))

    expect(await screen.findByText(/商品删除失败（POST \/product\/delete）：delete down/)).toBeInTheDocument()
    expect(screen.getByTestId('products-action-error')).toHaveAttribute('data-no-local-delete-on-error', 'true')
    expect(screen.getAllByText(/productId=1; productName=修护精华/).length).toBeGreaterThan(0)
    expect(screen.getByText(/失败不会本地移除商品行/)).toBeInTheDocument()
    expect(screen.getByText('修护精华')).toBeInTheDocument()
  })

  it('shows link extraction endpoint context and keeps URL input on failure', async () => {
    const user = userEvent.setup()
    vi.mocked(productApi.extractFromLink).mockRejectedValue(new Error('extract down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await screen.findByText('修护精华')
    await user.click(screen.getAllByRole('button', { name: /链接提取/ })[0])
    const dialog = await screen.findByRole('dialog', { name: '商品链接提取' })
    await user.type(within(dialog).getByLabelText('商品链接'), 'https://shop.example/error')
    await user.click(within(dialog).getByRole('button', { name: /开始提取/ }))

    expect(await within(dialog).findByText(/链接提取失败（POST \/product\/extract-from-link）：extract down/)).toBeInTheDocument()
    expect(within(dialog).getByTestId('product-link-extract-error')).toHaveAttribute('data-input-preserved', 'true')
    expect(within(dialog).getByText(/productLink=https:\/\/shop.example\/error/)).toBeInTheDocument()
    expect(within(dialog).getByLabelText('商品链接')).toHaveValue('https://shop.example/error')
  })

  it('shows short-video export endpoint context and keeps product drawer on failure', async () => {
    const user = userEvent.setup()
    vi.mocked(exportProductToShortVideo).mockRejectedValue(new Error('export down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await screen.findByText('修护精华')
    await user.click(screen.getByRole('button', { name: '详情' }))
    expect(await screen.findByTestId('product-detail-drawer')).toHaveAttribute('data-contract-source', expect.stringContaining('/product/script/search'))
    await user.click(await screen.findByRole('button', { name: '导出为短视频脚本' }))
    const dialog = await screen.findByRole('dialog', { name: '导出为短视频脚本' })
    expect(within(dialog).getByTestId('product-export-sv-dialog')).toHaveAttribute('data-contract-source', '/product/script/export-to-shortvideo')
    expect(within(dialog).getByTestId('product-export-sv-dialog')).toHaveAttribute('data-no-local-shortvideo-project', 'true')
    await user.click(within(dialog).getByRole('button', { name: /确认导出/ }))

    expect(await within(dialog).findByText(/导出短视频失败（POST \/product\/script\/export-to-shortvideo）：export down/)).toBeInTheDocument()
    expect(within(dialog).getByTestId('product-export-sv-error')).toHaveAttribute('data-dialog-input-preserved', 'true')
    expect(within(dialog).getByText(/productId=1; productName=修护精华; style=种草; duration=60/)).toBeInTheDocument()
    expect(screen.getAllByText('修护精华').length).toBeGreaterThan(0)
  })

  it('uses theme-aware product drawer and list surfaces in dark mode', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <ProductsPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText('修护精华')
    expect(screen.getByTestId('product-image-placeholder-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(238, 238, 238)',
    })
    expect(screen.getByTestId('product-profit-chip-surface')).not.toHaveStyle({
      backgroundColor: 'rgba(76, 175, 80, 0.125)',
    })

    await user.click(screen.getByRole('button', { name: '详情' }))
    await user.click(await screen.findByRole('tab', { name: 'AI卖点' }))
    expect(await screen.findAllByTestId('product-selling-point-surface')).toHaveLength(2)
    expect(screen.getAllByTestId('product-selling-point-surface')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(227, 242, 253)',
    })

    await user.click(screen.getByRole('tab', { name: 'GMV贡献' }))
    expect(await screen.findByTestId('product-gmv-summary-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(232, 245, 233)',
    })
    expect((await screen.findAllByTestId('product-channel-track-surface'))[0]).not.toHaveStyle({
      backgroundColor: 'rgb(238, 238, 238)',
    })
    expect(await screen.findByTestId('product-version-compare-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(245, 245, 245)',
    })
  })

  it('uses a theme-aware product effectiveness chart in dark mode', async () => {
    const user = userEvent.setup()
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <ProductsPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText('修护精华')
    await user.click(screen.getByRole('button', { name: '详情' }))
    await user.click(await screen.findByRole('tab', { name: '效果历史' }))

    const chart = await screen.findByTestId('product-effectiveness-chart-surface')
    expect(chart).toHaveAttribute('data-chart-color', '#e3f2fd')
    expect(chart.textContent).not.toContain('#1976d2')
    expect(chart.textContent).not.toContain('rgba(25,118,210,0.08)')
    expect(chart.textContent).toContain('#e3f2fd')
    expect(chart.textContent).toContain('rgba(227, 242, 253, 0.18)')
  })

  it('names product detail dependency failures without static fallbacks', async () => {
    const user = userEvent.setup()
    vi.mocked(productApi.scriptList).mockRejectedValueOnce(new Error('script list down') as never)
    vi.mocked(productApi.effectivenessRanking).mockRejectedValueOnce(new Error('ranking down') as never)
    vi.mocked(productApi.salesHistorySearch).mockRejectedValueOnce(new Error('sales down') as never)
    vi.mocked(productApi.scriptUsageList).mockRejectedValueOnce(new Error('gmv down') as never)

    renderWithProviders(
      <MemoryRouter>
        <ProductsPage />
      </MemoryRouter>,
    )

    await screen.findByText('修护精华')
    await user.click(screen.getByRole('button', { name: '详情' }))

    await user.click(await screen.findByRole('tab', { name: '话术版本' }))
    expect(await screen.findByTestId('product-detail-script-list-error')).toHaveAttribute('data-contract-source', '/product/script/search')
    expect(screen.getByTestId('product-detail-script-list-error')).toHaveAttribute('data-no-static-script-fallback', 'true')

    await user.click(screen.getByRole('tab', { name: '效果历史' }))
    expect(await screen.findByTestId('product-detail-effectiveness-error')).toHaveAttribute('data-contract-source', '/product/script-effectiveness/ranking')
    expect(screen.getByTestId('product-detail-effectiveness-error')).toHaveAttribute('data-no-static-ranking-fallback', 'true')

    await user.click(screen.getByRole('tab', { name: 'GMV贡献' }))
    expect(await screen.findByTestId('product-detail-sales-history-error')).toHaveAttribute('data-no-dashboard-gmv-fallback', 'true')
    expect(await screen.findByTestId('product-detail-gmv-contrib-error')).toHaveAttribute('data-contract-source', '/product/script/usage-list')
  })
})
