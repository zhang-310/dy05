import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import StylePresetPage from '../StylePresetPage'
import SalesHistoryPage from '../SalesHistoryPage'
import { productApi } from '@/api/product'

vi.mock('@/api/product', () => ({
  productApi: {
    stylePresetList: vi.fn(),
    stylePresetSave: vi.fn(),
    stylePresetDelete: vi.fn(),
    stylePresetRecommend: vi.fn(),
    salesHistorySearch: vi.fn(),
    salesHistorySave: vi.fn(),
    salesHistoryTotalSalesAmount: vi.fn(),
    salesHistoryTotalSalesQuantity: vi.fn(),
    list: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, searchSlot, actionSlot, showExport }: any) => (
      <div data-testid="standard-data-grid" data-show-export={String(showExport)}>
        <div>{searchSlot}</div>
        <div>{actionSlot}</div>
        {columns.map((col: any) => (
          <span key={col.field}>{col.headerName}</span>
        ))}
        {rows.length === 0 ? <div>暂无数据</div> : null}
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(col.valueGetter ? col.valueGetter(row[col.field], row) : row[col.field] ?? '')}
              </span>
            ))}
          </div>
        ))}
      </div>
    ),
  }
})

function renderPage(ui: React.ReactElement) {
  return renderWithProviders(<MemoryRouter>{ui}</MemoryRouter>)
}

describe('Product auxiliary pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(productApi.stylePresetList).mockResolvedValue([
      {
        id: 1,
        presetName: '专业讲解',
        presetCode: 'professional',
        styleValue: '专业',
        category: '护肤',
        description: '成分和功效讲清楚',
        isEnabled: false,
        sortOrder: 10,
      },
      {
        id: 2,
        presetName: '温暖讲解',
        presetCode: 'warm',
        styleValue: '温暖',
        category: '护肤',
        description: '温和亲切',
        isEnabled: true,
        sortOrder: 20,
      },
      {
        id: 3,
        presetName: '热情促单',
        presetCode: 'enthusiastic',
        styleValue: '热情',
        category: '彩妆',
        description: '促单氛围强',
        isEnabled: true,
        sortOrder: 30,
      },
    ] as never)
    vi.mocked(productApi.stylePresetSave).mockResolvedValue({ id: 2 } as never)
    vi.mocked(productApi.stylePresetDelete).mockResolvedValue(undefined as never)
    vi.mocked(productApi.stylePresetRecommend).mockResolvedValue(['professional', 'warm', 'missing_code'] as never)

    vi.mocked(productApi.salesHistorySearch).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 9,
          productId: 3,
          saleAmount: 299,
          saleQuantity: 2,
          saleTime: '2026-05-21 10:00:00',
          channelSource: 'douyin_live',
          sessionId: 'live-1',
          createTime: '2026-05-21 10:01:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(productApi.salesHistorySave).mockResolvedValue(10 as never)
    vi.mocked(productApi.salesHistoryTotalSalesAmount).mockResolvedValue(1299 as never)
    vi.mocked(productApi.salesHistoryTotalSalesQuantity).mockResolvedValue(12 as never)
    vi.mocked(productApi.list).mockResolvedValue({
      total: 1,
      list: [{ id: 3, productName: '精华液', price: 199, status: 1 }],
      pageNum: 0,
      pageSize: 200,
    } as never)
  })

  it('StylePresetPage reads management list and saves validated payload', async () => {
    renderPage(<StylePresetPage />)

    expect(screen.getByRole('heading', { name: '话术风格预设' })).toBeInTheDocument()
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-contract-scope', 'product-style-preset-admin')
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/product/style-preset/list-all'))
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-context-endpoints', expect.stringContaining('/product/style-preset/recommend'))
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('management-list-from-enabled-list'))
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-no-static-style-fallback', 'true')
    expect(screen.getByTestId('style-preset-contract-alert')).toHaveAttribute('data-management-source', '/product/style-preset/list-all')
    expect(screen.getByTestId('style-preset-contract-alert')).toHaveAttribute('data-no-enabled-list-management-source', 'true')
    expect(screen.getByTestId('style-preset-contract-alert')).toHaveAttribute('data-no-product-script-generation', 'true')
    expect(screen.getByTestId('standard-data-grid')).toHaveAttribute('data-show-export', 'false')
    expect(screen.getByText(/\/product\/style-preset\/list-all/)).toBeInTheDocument()

    await waitFor(() => {
      expect(productApi.stylePresetList).toHaveBeenCalled()
    })
    expect(await screen.findByText('专业讲解')).toBeInTheDocument()
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-total-count', '3')
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-enabled-count', '2')
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-disabled-count', '1')
    expect(screen.getAllByTestId('style-preset-kpi-card')[0]).toHaveAttribute('data-contract-source', '/product/style-preset/list-all')
    expect(screen.getAllByText('停用').length).toBeGreaterThan(0)
    expect(screen.getByText('预设代码')).toBeInTheDocument()
    expect(screen.getByTestId('style-preset-recommend-card')).toHaveAttribute('data-contract-source', '/product/style-preset/recommend')
    expect(screen.getByTestId('style-preset-recommend-card')).toHaveAttribute('data-product-source', '/product/search')

    fireEvent.mouseDown(screen.getByLabelText('选择商品'))
    fireEvent.click(await screen.findByRole('option', { name: '精华液' }))

    await waitFor(() => {
      expect(productApi.stylePresetRecommend).toHaveBeenCalledWith(3)
    })
    expect(await screen.findByText(/专业讲解 \/ professional/)).toBeInTheDocument()
    expect(screen.getByText(/温暖讲解 \/ warm/)).toBeInTheDocument()
    expect(screen.getByText(/missing_code/)).toBeInTheDocument()
    expect(screen.getByText(/缺失 1 个/)).toBeInTheDocument()
    expect(screen.getByTestId('style-preset-recommend-summary')).toHaveAttribute('data-missing-recommended-count', '1')
    expect(screen.getByTestId('style-preset-recommend-summary')).toHaveAttribute('data-disabled-recommended-count', '1')
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-recommend-product-id', '3')
    expect(screen.getByTestId('style-preset-workbench')).toHaveAttribute('data-recommended-count', '3')

    fireEvent.click(screen.getByRole('button', { name: '新建预设' }))
    fireEvent.change(screen.getByLabelText('预设名称'), { target: { value: '温暖种草' } })
    fireEvent.change(screen.getByLabelText('预设代码'), { target: { value: 'warm_seed' } })
    fireEvent.mouseDown(screen.getByLabelText('语气风格'))
    fireEvent.click(await screen.findByRole('option', { name: '温暖' }))
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(productApi.stylePresetSave).toHaveBeenCalledWith(expect.objectContaining({
        presetName: '温暖种草',
        presetCode: 'warm_seed',
        styleValue: '温暖',
        isEnabled: true,
      }))
    })
  })

  it('StylePresetPage keeps dialog input and row when save, delete, or recommend fails', async () => {
    vi.mocked(productApi.stylePresetSave).mockRejectedValue(new Error('保存接口异常') as never)
    vi.mocked(productApi.stylePresetDelete).mockRejectedValue(new Error('删除接口异常') as never)
    vi.mocked(productApi.stylePresetRecommend).mockRejectedValue(new Error('推荐接口异常') as never)

    renderPage(<StylePresetPage />)

    expect(await screen.findByText('专业讲解')).toBeInTheDocument()

    fireEvent.mouseDown(screen.getByLabelText('选择商品'))
    fireEvent.click(await screen.findByRole('option', { name: '精华液' }))

    expect(await screen.findByText(/风格推荐加载失败.*推荐接口异常/)).toBeInTheDocument()
    expect(screen.getByTestId('style-preset-recommend-error')).toHaveAttribute('data-contract-source', '/product/style-preset/recommend')
    expect(screen.getByTestId('style-preset-recommend-error')).toHaveAttribute('data-input-preserved', 'true')
    expect(screen.getAllByText(/\/product\/style-preset\/recommend/).length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '新建预设' }))
    fireEvent.change(screen.getByLabelText('预设名称'), { target: { value: '失败保留' } })
    fireEvent.change(screen.getByLabelText('预设代码'), { target: { value: 'keep_on_error' } })
    fireEvent.mouseDown(screen.getByLabelText('语气风格'))
    fireEvent.click(await screen.findByRole('option', { name: '温暖' }))
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(screen.getAllByText(/保存失败.*保存接口异常/).length).toBeGreaterThan(0)
    })
    expect(screen.getByTestId('style-preset-save-error-page')).toHaveAttribute('data-dialog-input-preserved', 'true')
    expect(screen.getByTestId('style-preset-save-error-dialog')).toHaveAttribute('data-input-preserved', 'true')
    expect(screen.getByDisplayValue('失败保留')).toBeInTheDocument()
    expect(screen.getAllByText(/\/product\/style-preset\/save/).length).toBeGreaterThan(0)

    fireEvent.click(screen.getByLabelText('关闭'))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '新建风格预设' })).not.toBeInTheDocument()
    })
    fireEvent.click(screen.getAllByRole('button', { name: '删除' })[0])
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByText(/删除失败.*删除接口异常/)).toBeInTheDocument()
    expect(screen.getByTestId('style-preset-delete-error')).toHaveAttribute('data-contract-source', '/product/style-preset/delete')
    expect(screen.getByTestId('style-preset-delete-error')).toHaveAttribute('data-row-preserved', 'true')
    expect(screen.getByText('专业讲解')).toBeInTheDocument()
    expect(screen.getAllByText(/\/product\/style-preset\/delete/).length).toBeGreaterThan(0)
  })

  it('StylePresetPage names list and product lookup failures without local fallbacks', async () => {
    vi.mocked(productApi.stylePresetList).mockRejectedValue(new Error('列表接口异常') as never)
    vi.mocked(productApi.list).mockRejectedValue(new Error('商品接口异常') as never)

    renderPage(<StylePresetPage />)

    expect(await screen.findByTestId('style-preset-list-error')).toHaveAttribute('data-contract-source', '/product/style-preset/list-all')
    expect(screen.getByTestId('style-preset-list-error')).toHaveAttribute('data-no-static-style-fallback', 'true')
    expect(screen.getByText(/列表接口异常.*\/product\/style-preset\/list-all/)).toBeInTheDocument()

    expect(await screen.findByTestId('style-preset-products-error')).toHaveAttribute('data-contract-source', '/product/search')
    expect(screen.getByTestId('style-preset-products-error')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.getByText(/商品接口异常.*\/product\/search/)).toBeInTheDocument()
  })

  it('SalesHistoryPage posts SalesHistorySaveVO fields', async () => {
    renderPage(<SalesHistoryPage />)

    expect(screen.getByRole('heading', { name: '销售记录' })).toBeInTheDocument()
    expect(screen.getByTestId('sales-history-workbench')).toHaveAttribute('data-contract-scope', 'product-sales-history')
    expect(screen.getByTestId('sales-history-workbench')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/product/sales-history/search'))
    expect(screen.getByTestId('sales-history-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('auto-deduct-inventory-ui'))
    expect(screen.getByTestId('sales-history-contract-alert')).toHaveAttribute('data-no-auto-deduct-inventory-ui', 'true')
    expect(screen.getByTestId('sales-history-contract-alert')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.getByTestId('standard-data-grid')).toHaveAttribute('data-show-export', 'false')
    expect(screen.getByText(/saleAmount \/ saleQuantity \/ saleTime \/ channelSource/)).toBeInTheDocument()

    await waitFor(() => {
      expect(productApi.salesHistorySearch).toHaveBeenCalledWith({ page: 0, rows: 20, productId: undefined })
    })
    expect(await screen.findByText('精华液')).toBeInTheDocument()
    expect(screen.getAllByTestId('sales-history-kpi-card')[1]).toHaveAttribute('data-contract-source', 'local-derived-from-current-page')

    fireEvent.mouseDown(screen.getByLabelText('商品筛选'))
    fireEvent.click(await screen.findByRole('option', { name: '精华液' }))

    await waitFor(() => {
      expect(productApi.salesHistoryTotalSalesAmount).toHaveBeenCalledWith(3)
      expect(productApi.salesHistoryTotalSalesQuantity).toHaveBeenCalledWith(3)
    })
    await waitFor(() => {
      expect(productApi.salesHistorySearch).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        productId: 3,
        channelSource: undefined,
        sessionId: undefined,
        startTime: undefined,
        endTime: undefined,
      })
    })
    expect(screen.getByText(/当前累计指标来自选中商品：精华液/)).toBeInTheDocument()
    expect(screen.getByTestId('sales-history-filter-panel')).toHaveAttribute('data-product-id', '3')
    expect(screen.getAllByTestId('sales-history-kpi-card')[4]).toHaveAttribute('data-contract-source', 'sales-history-total-endpoint')
    expect(screen.getByText('¥1,299.00')).toBeInTheDocument()
    expect(screen.getByText('12')).toBeInTheDocument()

    fireEvent.mouseDown(screen.getByLabelText('渠道筛选'))
    fireEvent.click(await screen.findByRole('option', { name: '抖音直播' }))
    fireEvent.change(screen.getByLabelText('场次 ID'), { target: { value: 'live-1' } })
    fireEvent.change(screen.getByLabelText('开始时间'), { target: { value: '2026-05-21T00:00' } })
    fireEvent.change(screen.getByLabelText('结束时间'), { target: { value: '2026-05-21T23:59' } })

    await waitFor(() => {
      expect(productApi.salesHistorySearch).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        productId: 3,
        channelSource: 'douyin_live',
        sessionId: 'live-1',
        startTime: '2026-05-21T00:00',
        endTime: '2026-05-21T23:59',
      })
    })

    fireEvent.click(screen.getByRole('button', { name: '录入记录' }))
    expect(screen.getByTestId('sales-history-save-dialog')).toHaveAttribute('data-contract-source', '/product/sales-history/save')
    fireEvent.mouseDown(screen.getByLabelText('商品'))
    fireEvent.click(await screen.findByRole('option', { name: '精华液' }))
    fireEvent.change(screen.getByLabelText('销售金额'), { target: { value: '399' } })
    fireEvent.change(screen.getByLabelText('数量'), { target: { value: '4' } })
    fireEvent.change(screen.getByLabelText('销售时间'), { target: { value: '2026-05-21T10:00' } })
    fireEvent.change(screen.getByLabelText('场次 ID（可选）'), { target: { value: 'live-2' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(productApi.salesHistorySave).toHaveBeenCalledWith({
        productId: 3,
        saleAmount: 399,
        saleQuantity: 4,
        channelSource: 'manual',
        saleTime: '2026-05-21T10:00',
        sessionId: 'live-2',
      })
    })
  })

  it('SalesHistoryPage keeps input on save failure and names total endpoints', async () => {
    vi.mocked(productApi.salesHistorySave).mockRejectedValue(new Error('保存销售失败') as never)
    vi.mocked(productApi.salesHistoryTotalSalesAmount).mockRejectedValue(new Error('累计金额失败') as never)
    vi.mocked(productApi.salesHistoryTotalSalesQuantity).mockRejectedValue(new Error('累计销量失败') as never)

    renderPage(<SalesHistoryPage />)

    expect(await screen.findByText('精华液')).toBeInTheDocument()
    fireEvent.mouseDown(screen.getByLabelText('商品筛选'))
    fireEvent.click(await screen.findByRole('option', { name: '精华液' }))

    expect(await screen.findByTestId('sales-history-total-error')).toHaveAttribute('data-no-dashboard-fallback', 'true')
    expect(screen.getByText(/累计金额失败.*\/product\/sales-history\/total-sales-amount/)).toBeInTheDocument()
    expect(screen.getByText(/累计销量失败.*\/product\/sales-history\/total-sales-quantity/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '录入记录' }))
    fireEvent.mouseDown(screen.getByLabelText('商品'))
    fireEvent.click(await screen.findByRole('option', { name: '精华液' }))
    fireEvent.change(screen.getByLabelText('销售金额'), { target: { value: '499' } })
    fireEvent.change(screen.getByLabelText('数量'), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(screen.getAllByText(/保存失败.*保存销售失败/).length).toBeGreaterThan(0)
    })
    expect(screen.getByTestId('sales-history-save-error-page')).toHaveAttribute('data-dialog-input-preserved', 'true')
    expect(screen.getByTestId('sales-history-save-error-dialog')).toHaveAttribute('data-input-preserved', 'true')
    expect(screen.getByDisplayValue('499')).toBeInTheDocument()
    expect(screen.getByDisplayValue('5')).toBeInTheDocument()
    expect(screen.getAllByText(/\/product\/sales-history\/save/).length).toBeGreaterThan(0)
  })
})
