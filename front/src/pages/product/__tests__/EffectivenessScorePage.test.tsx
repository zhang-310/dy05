import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import EffectivenessScorePage from '../EffectivenessScorePage'
import { productApi } from '@/api/product'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => <div data-testid="echarts-mock">{JSON.stringify(option)}</div>,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({
      rows,
      columns,
      searchSlot,
      actionSlot,
      showExport,
    }: any) => (
      <div data-testid="standard-data-grid" data-show-export={String(showExport)}>
        <div>{searchSlot}</div>
        <div>{actionSlot}</div>
        <table>
          <tbody>
            {rows.map((row: any) => (
              <tr key={row.versionId ?? row.id}>
                {columns.map((col: any) => (
                  <td key={col.field}>
                    {col.renderCell
                      ? col.renderCell({ row, value: row[col.field], field: col.field })
                      : col.valueFormatter
                        ? col.valueFormatter(row[col.field])
                        : String(row[col.field] ?? '')}
                  </td>
                ))}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    ),
  }
})

vi.mock('@/api/product', () => ({
  productApi: {
    list: vi.fn(),
    effectivenessRanking: vi.fn(),
    effectivenessRecalculate: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

const products = {
  total: 2,
  list: [
    { id: 1, productName: '修护精华', price: 199, status: 1 },
    { id: 2, productName: '低库存面膜', price: 79, status: 1 },
  ],
  pageNum: 0,
  pageSize: 200,
}

const ranking = {
  list: [
    {
      productId: 1,
      productName: 'V2 专业',
      versionId: 22,
      versionNumber: 2,
      style: '专业',
      avgScore: 8.6,
      score: 86,
      scoreLevel: 'A',
      useCount: 18,
      conversionRate: 12.5,
      likesCount: 320,
      commentsCount: 18,
      isRecommended: true,
      tag: '专业',
      trend: 'flat' as const,
    },
    {
      productId: 1,
      productName: 'V1 种草',
      versionId: 21,
      versionNumber: 1,
      style: '种草',
      avgScore: 4.2,
      score: 42,
      scoreLevel: 'D',
      useCount: 0,
      conversionRate: 0,
      likesCount: 4,
      commentsCount: 1,
      isRecommended: false,
      tag: '种草',
      trend: 'down' as const,
    },
  ],
  total: 2,
  summary: {
    dates: [],
    avgScores: [],
    avgScore: 6.4,
    maxScore: 8.6,
    scoredCount: 2,
    avgConversionRate: 6.25,
  },
}

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <EffectivenessScorePage />
    </MemoryRouter>,
  )
}

describe('EffectivenessScorePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(productApi.list).mockResolvedValue(products as never)
    vi.mocked(productApi.effectivenessRanking).mockResolvedValue(ranking)
    vi.mocked(productApi.effectivenessRecalculate).mockResolvedValue(2)
  })

  it('loads products and queries script-version effectiveness after product selection', async () => {
    const user = userEvent.setup()
    renderPage()

    expect(screen.getByRole('heading', { name: '商品效果评分' })).toBeInTheDocument()
    expect(screen.getByTestId('product-effectiveness-workbench')).toHaveAttribute('data-contract-scope', 'product-effectiveness-ranking')
    expect(screen.getByTestId('product-effectiveness-workbench')).toHaveAttribute('data-ready-endpoints', '/product/search|/product/script-effectiveness/ranking|/product/script-effectiveness/recalculate')
    expect(screen.getByTestId('product-effectiveness-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('all-product-ranking'))
    expect(screen.getByTestId('product-effectiveness-contract-alert')).toHaveAttribute('data-no-all-product-ranking', 'true')
    expect(screen.getByTestId('product-effectiveness-contract-alert')).toHaveAttribute('data-no-auto-script-generation', 'true')
    expect(screen.getByTestId('standard-data-grid')).toHaveAttribute('data-show-export', 'false')
    expect(screen.getByText(/必须先选择商品/)).toBeInTheDocument()
    expect(screen.getByText('请选择商品后查看该商品的话术版本评分。')).toBeInTheDocument()

    await waitFor(() => {
      expect(productApi.list).toHaveBeenCalledWith({ page: 0, rows: 200 })
    })
    expect(productApi.effectivenessRanking).not.toHaveBeenCalled()

    await screen.findByRole('option', { name: '修护精华' })
    fireEvent.change(screen.getByLabelText('商品'), { target: { value: '1' } })

    await waitFor(() => {
      expect(productApi.effectivenessRanking).toHaveBeenCalledWith({
        productId: 1,
        sortBy: 'score',
        page: 0,
        rows: 20,
        topN: 0,
      })
    })

    expect(await screen.findByText('V2 专业')).toBeInTheDocument()
    expect(screen.getByTestId('product-effectiveness-workbench')).toHaveAttribute('data-selected-product-id', '1')
    expect(screen.getByTestId('effectiveness-score-chart-surface')).toHaveAttribute('data-contract-source', '/product/script-effectiveness/ranking')
    expect(screen.getAllByTestId('product-effectiveness-kpi-card')[1]).toHaveAttribute('data-contract-source', 'local-derived-from-ranking')
    expect(screen.getAllByTestId('product-effectiveness-diagnostic-card')[0]).toHaveAttribute('data-contract-source', 'local-derived-from-ranking')
    expect(screen.getAllByText('专业').length).toBeGreaterThan(0)
    expect(screen.getByText('低分版本')).toBeInTheDocument()
    expect(screen.getByText('无使用记录')).toBeInTheDocument()
    expect(screen.getByText('推荐版本')).toBeInTheDocument()
    expect(screen.getByText('评分样本可用，继续关注低分版本和未使用版本。')).toBeInTheDocument()

    await user.click(screen.getAllByRole('button', { name: /重新计算评分/ })[0])
    await waitFor(() => {
      expect(productApi.effectivenessRecalculate).toHaveBeenCalledWith(1)
    })
    expect(toast).toHaveBeenCalledWith('已重新计算 2 个话术版本', 'success')
  })

  it('shows retryable product-list and ranking errors', async () => {
    vi.mocked(productApi.list).mockResolvedValue(products as never)
    vi.mocked(productApi.effectivenessRanking).mockRejectedValueOnce(new Error('ranking down') as never)

    renderPage()

    await screen.findByRole('option', { name: '修护精华' })
    fireEvent.change(screen.getByLabelText('商品'), { target: { value: '1' } })

    expect(await screen.findByTestId('product-effectiveness-ranking-error')).toHaveAttribute('data-no-static-ranking-fallback', 'true')
    expect(screen.getByText(/商品效果评分加载失败/)).toBeInTheDocument()
    expect(screen.getByText(/ranking down/)).toBeInTheDocument()
  })

  it('shows product list load failure before selection', async () => {
    vi.mocked(productApi.list).mockRejectedValueOnce(new Error('product search down') as never)

    renderPage()

    expect(await screen.findByTestId('product-effectiveness-products-error')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.getByText('商品列表加载失败')).toBeInTheDocument()
    expect(screen.getByText(/product search down/)).toBeInTheDocument()
  })

  it('shows empty ranking as backend empty state without static fallback', async () => {
    vi.mocked(productApi.effectivenessRanking).mockResolvedValueOnce({
      list: [],
      total: 0,
      summary: {
        dates: [],
        avgScores: [],
        avgScore: 0,
        maxScore: 0,
        scoredCount: 0,
        avgConversionRate: 0,
      },
    } as never)

    renderPage()

    await screen.findByRole('option', { name: '修护精华' })
    fireEvent.change(screen.getByLabelText('商品'), { target: { value: '1' } })

    expect(await screen.findByTestId('product-effectiveness-empty-ranking')).toHaveAttribute('data-no-static-ranking-fallback', 'true')
    expect(screen.getByText(/暂无评分分布/)).toBeInTheDocument()
  })

  it('uses a theme-aware score distribution chart color in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <EffectivenessScorePage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByRole('option', { name: '修护精华' })
    fireEvent.change(screen.getByLabelText('商品'), { target: { value: '1' } })

    const chart = await screen.findByTestId('effectiveness-score-chart-surface')
    expect(chart).toHaveAttribute('data-chart-color', '#e3f2fd')
    expect(chart.textContent).not.toContain('#1976d2')
    expect(chart.textContent).toContain('#e3f2fd')
  })
})
