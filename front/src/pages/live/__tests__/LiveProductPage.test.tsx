import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import LiveProductPage from '../LiveProductPage'
import { liveApi } from '@/api/live'
import { AppThemeProvider } from '@/theme/AppThemeProvider'

vi.mock('@/api/live', () => ({
  liveApi: {
    sessionSearch: vi.fn(),
    productSearch: vi.fn(),
    productSave: vi.fn(),
    productDelete: vi.fn(),
    productBatchAdd: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    TableSkeleton: () => <div data-testid="live-product-table-skeleton" />,
  }
})

const sessions = [
  { id: 18, liveTitle: '晚场修护直播', userId: 1, accountId: 1, personaId: 1, sessionCover: '', scriptStyle: '', liveDescription: '', scheduledTime: '', scheduledEndTime: '', startTime: '', endTime: '', liveUrl: '', viewers: 0, likes: 0, status: 1, sessionType: '', liveFormat: '', createTime: '', updateTime: '' },
  { id: 19, liveTitle: '新品预热直播', userId: 1, accountId: 1, personaId: 1, sessionCover: '', scriptStyle: '', liveDescription: '', scheduledTime: '', scheduledEndTime: '', startTime: '', endTime: '', liveUrl: '', viewers: 0, likes: 0, status: 0, sessionType: '', liveFormat: '', createTime: '', updateTime: '' },
]

const products = [
  { id: 31, sessionId: 18, productId: 9001, productName: '修护精华', saleQuantity: 12, revenue: 4999.5, position: 1, productType: 'hot', productScriptId: 77, createTime: '2026-05-22T20:00:00' },
  { id: 32, sessionId: 18, productId: 9002, productName: '舒缓面霜', saleQuantity: 8, revenue: 2399, position: 2, productType: 'profit', createTime: '2026-05-22T20:05:00' },
]

function renderPage() {
  return renderWithProviders(
    <MemoryRouter initialEntries={['/admin/live/products']}>
      <LiveProductPage />
    </MemoryRouter>
  )
}

function renderPageWithTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter initialEntries={['/admin/live/products']}>
        <LiveProductPage />
      </MemoryRouter>
    </AppThemeProvider>
  )
}

describe('LiveProductPage', () => {
  beforeEach(() => {
    window.localStorage.clear()
    vi.clearAllMocks()
    vi.mocked(liveApi.sessionSearch).mockResolvedValue({ total: 2, list: sessions, pageNum: 0, pageSize: 100 })
    vi.mocked(liveApi.productSearch).mockResolvedValue({ total: 2, list: products, pageNum: 0, pageSize: 20 })
    vi.mocked(liveApi.productSave).mockResolvedValue(31)
    vi.mocked(liveApi.productDelete).mockResolvedValue(undefined)
    vi.mocked(liveApi.productBatchAdd).mockResolvedValue(2 as never)
  })

  it('renders product rows and real contract diagnostics', async () => {
    renderPage()

    expect(await screen.findByText('修护精华')).toBeInTheDocument()
    expect(screen.getByText('舒缓面霜')).toBeInTheDocument()
    expect(screen.getByTestId('live-product-workbench')).toHaveAttribute('data-contract-scope', 'live-product-session-relations')
    expect(screen.getByTestId('live-product-workbench')).toHaveAttribute('data-ready-endpoints', '/live/session/search|/live/product/search|/live/product/save|/live/product/delete|/live/product/batch-add')
    expect(screen.getByTestId('live-product-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('shortvideo-project-create'))
    expect(screen.getByTestId('live-product-contract-alert')).toHaveAttribute('data-no-price-status-save', 'true')
    expect(screen.getByTestId('live-product-contract-alert')).toHaveAttribute('data-no-shortvideo-project-create', 'true')
    expect(screen.getByTestId('live-product-table-surface')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.getByText(/保存接口真实接收 sessionId、productId、productName、saleQuantity、position、productType/)).toBeInTheDocument()
    expect(screen.getByText('当前页销量')).toBeInTheDocument()
    expect(screen.getByText('当前页收益')).toBeInTheDocument()
  })

  it('uses theme-aware summary KPI colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderPageWithTheme()

    expect(await screen.findByText('修护精华')).toBeInTheDocument()
    const values = await screen.findAllByTestId('live-product-kpi-value-surface')
    expect(values.map((node) => node.getAttribute('data-kpi-tone'))).toEqual([
      'primary',
      'success',
      'warning',
      'secondary',
    ])
    const colors = values.map((node) => window.getComputedStyle(node).color)
    for (const legacyRgb of [
      'rgb(25, 118, 210)',
      'rgb(76, 175, 80)',
      'rgb(255, 152, 0)',
      'rgb(123, 31, 162)',
    ]) {
      expect(colors).not.toContain(legacyRgb)
    }
  })

  it('filters by session with backend payload', async () => {
    renderPage()

    await screen.findByText('修护精华')
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '场次筛选' }))
    fireEvent.click(screen.getByRole('option', { name: '晚场修护直播' }))

    await waitFor(() => {
      expect(liveApi.productSearch).toHaveBeenLastCalledWith({ page: 0, rows: 20, sessionId: 18 })
    })
  })

  it('saves only fields accepted by /live/product/save', async () => {
    const user = userEvent.setup()
    renderPage()

    await screen.findByText('修护精华')
    await user.click(screen.getByRole('button', { name: '添加商品' }))
    const dialog = screen.getByRole('dialog')

    await user.type(within(dialog).getByRole('textbox', { name: '商品名称' }), '新品精华')
    await user.type(within(dialog).getByRole('spinbutton', { name: '商品ID' }), '9003')
    await user.type(within(dialog).getByRole('spinbutton', { name: '销量' }), '3')
    await user.type(within(dialog).getByRole('spinbutton', { name: '讲解位次' }), '5')
    fireEvent.mouseDown(within(dialog).getByRole('combobox', { name: '关联场次' }))
    fireEvent.click(screen.getByRole('option', { name: '晚场修护直播' }))
    fireEvent.mouseDown(within(dialog).getByRole('combobox', { name: '商品类型' }))
    fireEvent.click(screen.getByRole('option', { name: '爆品' }))
    await user.type(within(dialog).getByRole('spinbutton', { name: '商品话术ID' }), '88')
    await user.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(liveApi.productSave).toHaveBeenCalledWith({
        id: undefined,
        sessionId: 18,
        productId: 9003,
        productName: '新品精华',
        saleQuantity: 3,
        position: 5,
        productType: 'hot',
        scriptSource: 'session',
        productScriptId: 88,
      })
    })
  })

  it('shows save error inside dialog without closing it', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.productSave).mockRejectedValue(new Error('save down'))
    renderPage()

    await screen.findByText('修护精华')
    await user.click(screen.getAllByLabelText('编辑')[0])
    const dialog = screen.getByRole('dialog')
    await user.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByText(/\/live\/product\/save：save down/)).toBeInTheDocument()
    expect(within(dialog).getByTestId('live-product-save-error')).toHaveAttribute('data-input-preserved', 'true')
    expect(screen.getByTestId('live-product-save-dialog')).toHaveAttribute('data-no-price-status-save', 'true')
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('shows delete failure and keeps the row visible', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.productDelete).mockRejectedValue(new Error('delete down'))
    renderPage()

    await screen.findByText('修护精华')
    await user.click(screen.getAllByLabelText('删除')[0])
    await user.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByText(/\/live\/product\/delete：delete down/)).toBeInTheDocument()
    expect(screen.getByTestId('live-product-delete-error')).toHaveAttribute('data-no-local-delete-on-error', 'true')
    expect(screen.getByText('修护精华')).toBeInTheDocument()
  })

  it('shows batch add failure with source endpoint', async () => {
    const user = userEvent.setup()
    vi.mocked(liveApi.productBatchAdd).mockRejectedValue(new Error('batch down'))
    renderPage()

    await screen.findByText('修护精华')
    const checkboxes = screen.getAllByRole('checkbox')
    await user.click(checkboxes[1])
    await user.click(screen.getByRole('button', { name: '批量添加到场次' }))
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '选择目标场次' }))
    fireEvent.click(screen.getByRole('option', { name: '新品预热直播' }))
    await user.click(screen.getByRole('button', { name: '确认添加' }))

    await waitFor(() => {
      expect(liveApi.productBatchAdd).toHaveBeenCalledWith(19, [
        expect.objectContaining({ productId: 9001, productName: '修护精华', productType: 'hot', productScriptId: 77 }),
      ])
    })
    expect(await screen.findByText(/\/live\/product\/batch-add：batch down/)).toBeInTheDocument()
    expect(screen.getByTestId('live-product-batch-dialog-error')).toHaveAttribute('data-no-local-batch-result', 'true')
  }, 20000)

  it('shows empty state without mock product rows', async () => {
    vi.mocked(liveApi.productSearch).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 })

    renderPage()

    expect(await screen.findByText('还没有商品')).toBeInTheDocument()
    expect(screen.getByTestId('live-product-empty')).toHaveAttribute('data-no-static-live-product-fallback', 'true')
    expect(screen.getByText('添加第一个商品到直播场次，开始商品讲解')).toBeInTheDocument()
  })

  it('shows load failure sources without static live product fallback', async () => {
    vi.mocked(liveApi.productSearch).mockRejectedValue(new Error('product search down'))
    vi.mocked(liveApi.sessionSearch).mockRejectedValue(new Error('session search down'))

    renderPage()

    expect(await screen.findByTestId('live-product-load-error')).toHaveTextContent('/live/product/search 商品列表')
    expect(screen.getByTestId('live-product-load-error')).toHaveTextContent('/live/session/search 场次下拉')
    expect(screen.getByTestId('live-product-load-error')).toHaveAttribute('data-no-static-live-product-fallback', 'true')
    expect(screen.queryByText('修护精华')).not.toBeInTheDocument()
  })

  it('shows selected session empty state without injecting mock products', async () => {
    vi.mocked(liveApi.productSearch).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 })

    renderWithProviders(
      <MemoryRouter initialEntries={['/admin/live/products?sessionId=18']}>
        <LiveProductPage />
      </MemoryRouter>
    )

    expect(await screen.findByTestId('live-product-session-empty')).toHaveTextContent('当前场次暂无商品')
    expect(screen.getByTestId('live-product-session-empty')).toHaveAttribute('data-no-static-live-product-fallback', 'true')
    expect(screen.queryByText('修护精华')).not.toBeInTheDocument()
  })
})
