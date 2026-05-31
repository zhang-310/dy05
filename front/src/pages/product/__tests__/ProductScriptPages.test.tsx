import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter, Route, Routes } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import ProductScriptManagePage from '../ProductScriptManagePage'
import ProductScriptVersionPage from '../ProductScriptVersionPage'
import { exportProductToShortVideo, productApi } from '@/api/product'
import {
  ensureProductScriptOptimizationVersion,
  getProductScriptVersionHistory,
  recommendProductScriptVersions,
  updateProductScriptVersionStatus,
} from '@/api/product-script-version'
import {
  analyzeProductScriptVersion,
  getProductScriptOptimizationSuggestions,
  regenerateProductScriptBySuggestion,
} from '@/api/optimization'
import { scriptApi } from '@/api/script'

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/api/product', () => ({
  productApi: {
    get: vi.fn(),
    scriptList: vi.fn(),
    scriptActivate: vi.fn(),
    scriptDelete: vi.fn(),
    stylePresetRecommend: vi.fn(),
  },
  exportProductToShortVideo: vi.fn(),
  generateMultiStyleScriptsSse: vi.fn(() => ({ abort: vi.fn() })),
  previewStyles: vi.fn(),
}))

vi.mock('@/api/script', () => ({
  scriptApi: {
    categories: vi.fn().mockResolvedValue(['护肤', '成交转化']),
  },
}))

vi.mock('@/api/product-script-version', () => ({
  ensureProductScriptOptimizationVersion: vi.fn(),
  getProductScriptVersionHistory: vi.fn(),
  recommendProductScriptVersions: vi.fn(),
  updateProductScriptVersionStatus: vi.fn(),
}))

vi.mock('@/api/optimization', () => ({
  analyzeProductScriptVersion: vi.fn(),
  getProductScriptOptimizationSuggestions: vi.fn(),
  regenerateProductScriptBySuggestion: vi.fn(),
}))

vi.mock('@/components/product/script-manage', () => ({
  ScriptStyleList: ({ byStyle, onActivate, onDelete, onHistory, onExportShortVideo, onRefine, onCopy }: any) => (
    <div>
      {Object.values(byStyle).flat().map((script: any) => (
        <div key={script.id}>
          <span>{script.scriptContent}</span>
          <button aria-label={`激活-${script.id}`} onClick={() => onActivate(script)}>激活</button>
          <button onClick={() => onDelete(script)}>删除</button>
          <button onClick={() => onHistory(script)}>版本历史</button>
          <button onClick={() => onExportShortVideo(script)}>导出为短视频项目</button>
          <button onClick={() => onRefine(script)}>AI 精修</button>
          <button onClick={() => onCopy(script)}>复制</button>
        </div>
      ))}
    </div>
  ),
  ScriptGeneratePanel: () => <div>AI 生成配置</div>,
}))

vi.mock('@/components/product/script-manage/StylePreviewDialog', () => ({
  StylePreviewDialog: () => null,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, showExport }: any) => (
      <div data-testid="standard-data-grid" data-show-export={String(showExport)}>
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

function renderWithRoute(path: string, element: React.ReactElement) {
  return renderWithProviders(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path="/admin/product/:productId/scripts" element={element} />
        <Route path="/admin/product/:productId/script-versions" element={element} />
      </Routes>
    </MemoryRouter>,
  )
}

describe('Product script pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    Object.assign(navigator, {
      clipboard: {
        writeText: vi.fn().mockResolvedValue(undefined),
      },
    })
    vi.mocked(productApi.get).mockResolvedValue({
      id: 9,
      userId: 1,
      productName: '修护精华',
      productCode: 'P9',
      category: '护肤',
      brand: '示例',
      price: 199,
      costPrice: 80,
      profitMarginPct: 0.6,
      inventory: 10,
      unit: '瓶',
      weight: 1,
      mainImage: '',
      description: '',
      highlights: '',
      sellingPoints: '',
      status: 1,
      createTime: '2026-05-21 10:00:00',
      updateTime: '2026-05-21 10:00:00',
    } as never)
    vi.mocked(productApi.stylePresetRecommend).mockResolvedValue(['professional'] as never)
    vi.mocked(productApi.scriptList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 8,
          productId: 9,
          scriptTitle: 'V1',
          scriptContent: '修护精华话术',
          scriptType: 'formal',
          style: 'professional',
          duration: 60,
          useCount: 0,
          rating: 0,
          status: 0,
          isActive: false,
          createTime: '2026-05-21 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 100,
    } as never)
    vi.mocked(productApi.scriptActivate).mockResolvedValue(undefined as never)
    vi.mocked(productApi.scriptDelete).mockResolvedValue(undefined as never)
    vi.mocked(exportProductToShortVideo).mockResolvedValue({
      scriptId: 7,
      projectId: 99,
      projectName: '商品转短视频',
    } as never)
    vi.mocked(ensureProductScriptOptimizationVersion).mockResolvedValue({
      scriptId: 8,
      scriptVersionId: 21,
      created: true,
    } as never)
    vi.mocked(analyzeProductScriptVersion).mockResolvedValue({
      id: 31,
      scriptVersionId: 21,
      overallScore: 72.5,
      dataSource: 'PRODUCT_SCRIPT',
      analysisType: 'COMPREHENSIVE',
      weakPoints: [{ type: 'LOW_INTERACTION', severity: 'HIGH', description: '互动环节偏弱' }],
      styleProfile: { dominantStyle: 'FRIENDLY', styleScores: {} },
    } as never)
    vi.mocked(getProductScriptOptimizationSuggestions).mockResolvedValue([
      {
        id: 41,
        scriptVersionId: 21,
        analysisResultId: 31,
        category: 'CONTENT',
        priority: 'HIGH',
        suggestionContent: '加入问题式互动开场',
        adoptionStatus: 'PENDING',
      },
    ] as never)
    vi.mocked(regenerateProductScriptBySuggestion).mockResolvedValue({
      regenerationTaskId: 'regen_1',
      generatedAt: '2026-05-22T10:00:00',
      variants: [
        {
          id: 51,
          scriptVersionId: 21,
          suggestionId: 41,
          generationStyle: 'FRIENDLY',
          regeneratedContent: '优化后的亲和话术',
          aiQualityScore: 8.2,
          approvalStatus: 'PENDING',
        },
      ],
    } as never)
    vi.mocked(getProductScriptVersionHistory).mockResolvedValue([
      {
        id: 21,
        productId: 9,
        versionNumber: 3,
        content: '版本三话术',
        style: 'warm',
        effectivenessScore: 88,
        conversionRate: 12.5,
        usageCount: 4,
        isActive: false,
        isRecommended: true,
        createdAt: '2026-05-21 11:00:00',
      },
    ] as never)
    vi.mocked(recommendProductScriptVersions).mockResolvedValue({
      versions: [
        {
          id: 21,
          versionNumber: 3,
          style: 'warm',
          effectivenessScore: 88,
          usageCount: 4,
          conversionRate: 12.5,
          recommendScore: 91.5,
        },
      ],
      scores: [91.5],
    } as never)
    vi.mocked(updateProductScriptVersionStatus).mockResolvedValue({
      id: 21,
      productId: 9,
      versionNumber: 3,
      content: '版本三话术',
      isActive: true,
    } as never)
  })

  it('ProductScriptManagePage renders diagnostics and exports current script to short video', async () => {
    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    expect(await screen.findByText('修护精华')).toBeInTheDocument()
    expect(screen.getByTestId('product-script-workbench')).toHaveAttribute('data-contract-scope', 'product-script-management')
    expect(screen.getByTestId('product-script-workbench')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/product/script/search'))
    expect(screen.getByTestId('product-script-workbench')).toHaveAttribute('data-context-endpoints', expect.stringContaining('/product/script/analyze'))
    expect(screen.getByTestId('product-script-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-shortvideo-project'))
    expect(screen.getByTestId('product-script-workbench')).toHaveAttribute('data-no-static-script-fallback', 'true')
    expect(screen.getByTestId('product-script-contract-alert')).toHaveAttribute('data-generation-source', '/product/script/generate-multi-sse')
    expect(screen.getByTestId('product-script-contract-alert')).toHaveAttribute('data-no-local-shortvideo-project', 'true')
    expect(screen.getByTestId('product-script-contract-alert')).toHaveAttribute('data-no-version-rollback-from-main-page', 'true')
    expect(screen.getAllByText(/\/product\/script\/search/).length).toBeGreaterThan(0)
    expect(screen.getByText('话术总数')).toBeInTheDocument()
    expect(screen.getByText('修护精华话术')).toBeInTheDocument()
    expect(screen.getByTestId('product-script-workbench')).toHaveAttribute('data-total-scripts', '1')
    expect(screen.getByTestId('product-script-workbench')).toHaveAttribute('data-current-type-scripts', '1')
    expect(screen.getByTestId('product-script-list-panel')).toHaveAttribute('data-contract-source', '/product/script/search')
    expect(screen.getByTestId('product-script-generate-panel')).toHaveAttribute('data-contract-source', '/product/script/generate-multi-sse')
    expect(screen.getAllByTestId('product-script-kpi-card')[0]).toHaveAttribute('data-contract-source', '/product/script/search')
    expect(screen.getAllByTestId('product-script-kpi-card')[3]).toHaveAttribute('data-contract-source', '/script/categories')

    fireEvent.click(screen.getAllByRole('button', { name: '导出为短视频项目' })[0])
    expect(await screen.findByTestId('product-script-export-dialog')).toHaveAttribute('data-contract-source', '/product/script/export-to-shortvideo')
    expect(screen.getByTestId('product-script-export-dialog')).toHaveAttribute('data-no-local-shortvideo-project', 'true')
    fireEvent.click(await screen.findByRole('button', { name: '确认导出' }))

    await waitFor(() => {
      expect(exportProductToShortVideo).toHaveBeenCalledWith({
        productId: 9,
        style: 'professional',
        duration: 60,
      })
    })
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/workbench?projectId=99'))
  })

  it('ProductScriptManagePage confirms delete before calling real delete API', async () => {
    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    await screen.findByText('修护精华话术')
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(productApi.scriptDelete).toHaveBeenCalledWith(8)
    })
  })

  it('ProductScriptManagePage keeps activation failure visible without changing local state', async () => {
    vi.mocked(productApi.scriptActivate).mockRejectedValueOnce(new Error('activate down'))

    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    await screen.findByText('修护精华话术')
    fireEvent.click(screen.getByRole('button', { name: '激活-8' }))

    expect(await screen.findByText(/\/product\/script\/activate\/8：activate down/)).toBeInTheDocument()
    expect(screen.getByTestId('product-script-action-error')).toHaveAttribute('data-no-local-state-change', 'true')
    expect(screen.getByText(/失败时不会本地切换激活状态或移除话术/)).toBeInTheDocument()
    expect(screen.getByText('修护精华话术')).toBeInTheDocument()
  })

  it('ProductScriptManagePage keeps delete failure visible and preserves row', async () => {
    vi.mocked(productApi.scriptDelete).mockRejectedValueOnce(new Error('delete down'))

    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    await screen.findByText('修护精华话术')
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    expect(await screen.findByText(/\/product\/script\/8：delete down/)).toBeInTheDocument()
    expect(screen.getByTestId('product-script-action-error')).toHaveAttribute('data-contract-source', '/product/script/activate/{scriptId}|/product/script/{scriptId}')
    expect(screen.getByText(/失败时不会本地切换激活状态或移除话术/)).toBeInTheDocument()
    expect(screen.getByText('修护精华话术')).toBeInTheDocument()
  })

  it('ProductScriptManagePage shows export failure in dialog and page diagnostics', async () => {
    vi.mocked(exportProductToShortVideo).mockRejectedValueOnce(new Error('export down'))

    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    await screen.findByText('修护精华话术')
    fireEvent.click(screen.getAllByRole('button', { name: '导出为短视频项目' })[0])
    fireEvent.click(await screen.findByRole('button', { name: '确认导出' }))

    expect(await screen.findAllByText(/\/product\/script\/export-to-shortvideo：export down/)).toHaveLength(2)
    expect(screen.getByTestId('product-script-export-error-page')).toHaveAttribute('data-no-local-shortvideo-project', 'true')
    expect(screen.getByTestId('product-script-export-error-dialog')).toHaveAttribute('data-dialog-input-preserved', 'true')
    expect(screen.getByText(/导出失败不会创建本地假项目/)).toBeInTheDocument()
    expect(screen.getByRole('dialog')).toBeInTheDocument()
  })

  it('ProductScriptManagePage runs real optimization version and regeneration chain for AI refine', async () => {
    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    await screen.findByText('修护精华话术')
    fireEvent.click(screen.getByRole('button', { name: 'AI 精修' }))

    expect(await screen.findByText('AI 精修结果')).toBeInTheDocument()
    expect(screen.getByTestId('product-script-refine-dialog')).toHaveAttribute('data-contract-source', expect.stringContaining('/product/script/analyze'))

    await waitFor(() => {
      expect(ensureProductScriptOptimizationVersion).toHaveBeenCalledWith(8)
      expect(analyzeProductScriptVersion).toHaveBeenCalledWith(21)
      expect(getProductScriptOptimizationSuggestions).toHaveBeenCalledWith(21, 31, 5)
      expect(regenerateProductScriptBySuggestion).toHaveBeenCalledWith(21, 41, ['FRIENDLY', 'HUMOROUS', 'PREMIUM'])
    })

    expect(screen.getByText('优化版本')).toBeInTheDocument()
    await waitFor(() => {
      expect(screen.getByTestId('product-script-refine-dialog')).toHaveAttribute('data-version-id', '21')
      expect(screen.getByTestId('product-script-refine-dialog')).toHaveAttribute('data-suggestion-count', '1')
      expect(screen.getByTestId('product-script-refine-dialog')).toHaveAttribute('data-variant-count', '1')
    })
    expect(screen.getByText('互动环节偏弱')).toBeInTheDocument()
    expect(screen.getByText('加入问题式互动开场')).toBeInTheDocument()
    expect(screen.getByText('优化后的亲和话术')).toBeInTheDocument()
  })

  it('ProductScriptManagePage names product, script, and category fallback boundaries', async () => {
    vi.mocked(productApi.stylePresetRecommend).mockRejectedValueOnce(new Error('recommend style down') as never)
    vi.mocked(productApi.scriptList).mockRejectedValueOnce(new Error('script list down') as never)

    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    expect(await screen.findByTestId('product-script-list-error')).toHaveAttribute('data-contract-source', '/product/script/search')
    expect(screen.getByTestId('product-script-list-error')).toHaveAttribute('data-no-static-script-fallback', 'true')
    expect(screen.getByText(/script list down/)).toBeInTheDocument()
    expect(await screen.findByTestId('product-script-category-error')).toHaveAttribute('data-fallback-scope', 'manual-style-and-built-in-categories')
    expect(screen.getByText(/recommend style down/)).toBeInTheDocument()

    vi.clearAllMocks()
    vi.mocked(productApi.get).mockRejectedValueOnce(new Error('product down') as never)
    vi.mocked(productApi.scriptList).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 100 } as never)
    vi.mocked(productApi.stylePresetRecommend).mockResolvedValue([] as never)
    vi.mocked(scriptApi.categories).mockResolvedValue(['护肤'] as never)

    renderWithRoute('/admin/product/9/scripts', <ProductScriptManagePage />)

    expect(await screen.findByTestId('product-script-product-error')).toHaveAttribute('data-contract-source', '/product/get')
    expect(screen.getByTestId('product-script-product-error')).toHaveAttribute('data-no-local-product-fallback', 'true')
    expect(screen.getByText(/product down/)).toBeInTheDocument()
  })

  it('ProductScriptVersionPage uses list-by-product and update-status instead of rollback', async () => {
    renderWithRoute('/admin/product/9/script-versions', <ProductScriptVersionPage />)

    expect(await screen.findByText('商品话术版本')).toBeInTheDocument()
    expect(screen.getByTestId('product-script-version-workbench')).toHaveAttribute('data-contract-scope', 'product-script-version-management')
    expect(screen.getByTestId('product-script-version-workbench')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/product/script-version/list-by-product'))
    expect(screen.getByTestId('product-script-version-workbench')).toHaveAttribute('data-context-endpoints', expect.stringContaining('/product/script-version/best'))
    expect(screen.getByTestId('product-script-version-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('product-version-rollback'))
    expect(screen.getByTestId('product-script-version-contract-alert')).toHaveAttribute('data-no-product-version-rollback', 'true')
    expect(screen.getByTestId('product-script-version-contract-alert')).toHaveAttribute('data-no-product-version-diff', 'true')
    expect(screen.getByTestId('product-script-version-contract-alert')).toHaveAttribute('data-no-local-shortvideo-project', 'true')
    expect(screen.getByTestId('standard-data-grid')).toHaveAttribute('data-show-export', 'false')
    expect(getProductScriptVersionHistory).toHaveBeenCalledWith(9)
    expect(screen.getByText(/暂无按 `productId \+ versionNumber` 回滚/)).toBeInTheDocument()
    expect(screen.getByText('版本三话术')).toBeInTheDocument()
    expect(screen.getByText('推荐版本诊断')).toBeInTheDocument()
    expect(screen.getByText(/\/product\/script-version\/recommend/)).toBeInTheDocument()
    expect(screen.getAllByTestId('product-script-version-kpi-card')[0]).toHaveAttribute('data-contract-source', '/product/script-version/list-by-product')
    expect(screen.getByTestId('product-script-version-recommend-card')).toHaveAttribute('data-contract-source', '/product/script-version/recommend')

    await waitFor(() => {
      expect(recommendProductScriptVersions).toHaveBeenCalledWith(9, undefined, 3)
    })
    expect(screen.getByText('V3 warm · 推荐分 91.5')).toBeInTheDocument()
    expect(screen.getByTestId('product-script-version-workbench')).toHaveAttribute('data-row-count', '1')
    expect(screen.getByTestId('product-script-version-workbench')).toHaveAttribute('data-recommend-result-count', '1')
    expect(screen.getByTestId('product-script-version-recommend-card')).toHaveAttribute('data-result-count', '1')

    fireEvent.click(screen.getByRole('button', { name: '启用' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(updateProductScriptVersionStatus).toHaveBeenCalledWith(21, true)
    })
  })

  it('ProductScriptVersionPage exports selected version to short video', async () => {
    renderWithRoute('/admin/product/9/script-versions', <ProductScriptVersionPage />)

    await screen.findByText('版本三话术')
    fireEvent.click(screen.getByRole('button', { name: '导出' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(exportProductToShortVideo).toHaveBeenCalledWith({
        productId: 9,
        versionId: 21,
        style: 'warm',
        duration: 60,
      })
    })
  })

  it('ProductScriptVersionPage keeps status failure visible without switching local state', async () => {
    vi.mocked(updateProductScriptVersionStatus).mockRejectedValueOnce(new Error('status down'))

    renderWithRoute('/admin/product/9/script-versions', <ProductScriptVersionPage />)

    await screen.findByText('版本三话术')
    fireEvent.click(screen.getByRole('button', { name: '启用' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    expect(await screen.findByText(/\/product\/script-version\/update-status：status down/)).toBeInTheDocument()
    expect(screen.getByTestId('product-script-version-status-error')).toHaveAttribute('data-no-local-status-toggle', 'true')
    expect(screen.getByText(/失败时不会本地切换启用状态/)).toBeInTheDocument()
    expect(screen.getByText('版本三话术')).toBeInTheDocument()
    expect(updateProductScriptVersionStatus).toHaveBeenCalledWith(21, true)
  })

  it('ProductScriptVersionPage keeps export failure visible without navigating', async () => {
    vi.mocked(exportProductToShortVideo).mockRejectedValueOnce(new Error('export version down'))

    renderWithRoute('/admin/product/9/script-versions', <ProductScriptVersionPage />)

    await screen.findByText('版本三话术')
    fireEvent.click(screen.getByRole('button', { name: '导出' }))
    fireEvent.click(await screen.findByRole('button', { name: '确认' }))

    expect(await screen.findByText(/\/product\/script\/export-to-shortvideo：export version down/)).toBeInTheDocument()
    expect(screen.getByTestId('product-script-version-export-error')).toHaveAttribute('data-no-local-shortvideo-project', 'true')
    expect(screen.getByText(/导出失败不会创建本地假项目或跳转工作台/)).toBeInTheDocument()
    expect(navigate).not.toHaveBeenCalledWith(expect.stringContaining('/shortvideo/workbench?projectId=99'))
  })

  it('ProductScriptVersionPage shows recommendation API failure separately from list loading', async () => {
    vi.mocked(recommendProductScriptVersions).mockRejectedValueOnce(new Error('recommend down'))

    renderWithRoute('/admin/product/9/script-versions', <ProductScriptVersionPage />)

    expect(await screen.findByText('版本三话术')).toBeInTheDocument()
    expect(await screen.findByText('recommend down')).toBeInTheDocument()
    expect(screen.getByTestId('product-script-version-recommend-error')).toHaveAttribute('data-list-preserved', 'true')
    expect(screen.getByText('推荐版本诊断')).toBeInTheDocument()
  })

  it('ProductScriptVersionPage names list and recommendation empty fallback boundaries', async () => {
    vi.mocked(getProductScriptVersionHistory).mockRejectedValueOnce(new Error('version list down'))

    renderWithRoute('/admin/product/9/script-versions', <ProductScriptVersionPage />)

    expect(await screen.findByTestId('product-script-version-list-error')).toHaveAttribute('data-contract-source', '/product/script-version/list-by-product')
    expect(screen.getByTestId('product-script-version-list-error')).toHaveAttribute('data-no-static-version-fallback', 'true')
    expect(screen.getByText(/version list down/)).toBeInTheDocument()

    vi.clearAllMocks()
    vi.mocked(getProductScriptVersionHistory).mockResolvedValue([] as never)
    vi.mocked(recommendProductScriptVersions).mockResolvedValue({ versions: [], scores: [] } as never)

    renderWithRoute('/admin/product/9/script-versions', <ProductScriptVersionPage />)

    expect(await screen.findByTestId('product-script-version-empty')).toHaveAttribute('data-no-static-version-fallback', 'true')
    expect(await screen.findByTestId('product-script-version-recommend-empty')).toHaveAttribute('data-no-static-recommend-fallback', 'true')
  })
})
