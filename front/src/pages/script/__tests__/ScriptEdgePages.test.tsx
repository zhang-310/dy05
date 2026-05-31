import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ViolationWordPage from '../ViolationWordPage'
import ViolationCheckPage from '../ViolationCheckPage'
import HybridSearchPage from '../HybridSearchPage'
import { scriptApi } from '@/api/script'

vi.mock('@/api/script', () => ({
  scriptApi: {
    violationList: vi.fn(),
    violationSave: vi.fn(),
    violationDelete: vi.fn(),
    violationCheck: vi.fn(),
    violationCheckBatch: vi.fn(),
    violationPublicList: vi.fn(),
    violationSuggestReplacement: vi.fn(),
    searchHybrid: vi.fn(),
    searchSemantic: vi.fn(),
    searchSuggest: vi.fn(),
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
    StandardDataGrid: ({ rows, columns, searchSlot, showExport }: any) => (
      <div data-testid="script-edge-grid" data-show-export={String(showExport)}>
        <div>{searchSlot}</div>
        {rows.length === 0 ? <div>暂无数据</div> : null}
        {rows.map((row: any) => (
          <div key={row.id}>
            {columns.map((col: any) => (
              <span key={col.field}>
                {col.renderCell
                  ? col.renderCell({ row, value: row[col.field] })
                  : String(col.valueGetter ? col.valueGetter(row[col.field], row) : col.valueFormatter ? col.valueFormatter(row[col.field]) : row[col.field] ?? '')}
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

describe('Script edge pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptApi.violationList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          word: '根治',
          level: 3,
          reason: '医疗功效宣称',
          scope: 'live_only',
          replacement: '改善',
          status: 1,
          createTime: '2026-05-21 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(scriptApi.violationSave).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.violationDelete).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.violationCheck).mockResolvedValue({
      hasViolation: true,
      totalCount: 1,
      violations: [
        { word: '根治', level: 3, reason: '医疗功效宣称', replacement: '改善', source: 'public', position: 2 },
      ],
    } as never)
    vi.mocked(scriptApi.violationCheckBatch).mockResolvedValue([] as never)
    vi.mocked(scriptApi.violationPublicList).mockResolvedValue({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(scriptApi.violationSuggestReplacement).mockResolvedValue({ replacement: '改善' } as never)
    vi.mocked(scriptApi.searchHybrid).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 9,
          scriptId: 9,
          title: '屏障修护话术',
          content: '修护屏障，温和讲解。',
          category: '护肤',
          style: 'professional',
          hybridScore: 0.86,
          vectorScore: 0.92,
          lexicalScore: 0.45,
          score: 0.86,
          source: 'script',
        },
      ],
      pageNum: 0,
      pageSize: 20,
      searchTime: 42,
    } as never)
    vi.mocked(scriptApi.searchSemantic).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 0 } as never)
    vi.mocked(scriptApi.searchSuggest).mockResolvedValue([] as never)
  })

  it('ViolationWordPage uses level reason scope contract', async () => {
    renderPage(<ViolationWordPage />)

    expect(screen.getByRole('heading', { name: '违规词库' })).toBeInTheDocument()
    expect(screen.getByText(/level、reason、scope/)).toBeInTheDocument()
    const workbench = screen.getByTestId('violation-word-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'script-violation-word-admin')
    expect(workbench).toHaveAttribute(
      'data-ready-endpoints',
      '/script/admin/violation/list|/script/admin/violation/save|/script/admin/violation/delete',
    )
    expect(workbench).toHaveAttribute(
      'data-file-endpoints',
      '/script/admin/violation/import|/script/admin/violation/export',
    )
    expect(workbench).toHaveAttribute(
      'data-context-endpoints',
      '/script/admin/violation/active|/script/violation/check|/script/violation/check-batch|/script/violation/suggest-replacement',
    )
    expect(workbench).toHaveAttribute(
      'data-unsupported-actions',
      'server-file-import|server-file-export|inline-active-list|inline-suggest-replacement',
    )
    expect(screen.getByTestId('violation-word-contract-alert')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.getByTestId('violation-word-contract-alert')).toHaveAttribute('data-no-server-import-request', 'true')
    expect(screen.getByTestId('violation-word-local-export-button')).toHaveAttribute('data-contract-action', 'local-current-page-csv')
    expect(screen.getByTestId('script-edge-grid')).toHaveAttribute('data-show-export', 'false')

    await waitFor(() => {
      expect(scriptApi.violationList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        keyword: undefined,
        reason: undefined,
        level: undefined,
      })
    })
    expect(await screen.findByText('根治')).toBeInTheDocument()
    expect(screen.getByText('医疗功效宣称')).toBeInTheDocument()
    expect(screen.getByText('仅直播')).toBeInTheDocument()
    expect(screen.getByTestId('violation-word-kpi-high-risk')).toHaveAttribute('data-contract-source', 'local-derived level>=3')
    expect(screen.getByTestId('violation-word-kpi-current-page')).toHaveAttribute('data-contract-source', '/script/admin/violation/list list.length')

    fireEvent.click(screen.getByRole('button', { name: '新增违规词' }))
    const dialog = screen.getByRole('dialog', { name: '新增违规词' })
    fireEvent.change(within(dialog).getByLabelText(/违规词/), { target: { value: '最有效' } })
    fireEvent.change(within(dialog).getByLabelText(/原因/), { target: { value: '绝对化用语' } })
    fireEvent.change(within(dialog).getByLabelText(/替换词/), { target: { value: '更适合' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(vi.mocked(scriptApi.violationSave).mock.calls[0][0]).toEqual(expect.objectContaining({
        word: '最有效',
        level: 2,
        reason: '绝对化用语',
        scope: 'all',
        replacement: '更适合',
        status: 1,
      }))
    })
  })

  it('ViolationWordPage keeps operation failures visible with endpoint source', async () => {
    vi.mocked(scriptApi.violationList).mockRejectedValueOnce(new Error('list down') as never)
    vi.mocked(scriptApi.violationSave).mockRejectedValueOnce(new Error('save down') as never)
    vi.mocked(scriptApi.violationDelete).mockRejectedValueOnce(new Error('delete down') as never)

    renderPage(<ViolationWordPage />)

    expect(await screen.findByText(/接口来源：\/script\/admin\/violation\/list/)).toBeInTheDocument()

    vi.mocked(scriptApi.violationList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          word: '根治',
          level: 3,
          reason: '医疗功效宣称',
          scope: 'live_only',
          replacement: '改善',
          status: 1,
          createTime: '2026-05-21 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    expect(await screen.findByText('根治')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    fireEvent.click(within(screen.getByRole('dialog', { name: '编辑违规词' })).getByRole('button', { name: '保存' }))
    expect(await screen.findByText(/接口来源：\/script\/admin\/violation\/save/)).toBeInTheDocument()
    expect(screen.getAllByText(/route=\/admin\/script\/violation-words/).length).toBeGreaterThan(0)
    expect(screen.getAllByText(/word=根治/).length).toBeGreaterThan(0)
    expect(within(screen.getByRole('dialog', { name: '编辑违规词' })).getByLabelText(/违规词/)).toHaveValue('根治')
    fireEvent.click(within(screen.getByRole('dialog', { name: '编辑违规词' })).getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '编辑违规词' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/接口来源：\/script\/admin\/violation\/delete/)).toBeInTheDocument()
    expect(screen.getAllByText(/wordId=1; word=根治/).length).toBeGreaterThan(0)
    expect(screen.getByText('根治')).toBeInTheDocument()
  })

  it('ViolationCheckPage sends selected scope and renders hit details', async () => {
    renderPage(<ViolationCheckPage />)

    const workbench = screen.getByTestId('violation-check-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'script-violation-check')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/script/violation/check')
    expect(workbench).toHaveAttribute(
      'data-related-endpoints',
      '/script/violation/check-batch|/script/violation/public/list|/script/violation/suggest-replacement',
    )
    expect(workbench).toHaveAttribute(
      'data-unsupported-actions',
      'local-rule-fallback|inline-public-list|inline-batch-check|inline-suggest-replacement',
    )
    const alert = screen.getByTestId('violation-check-contract-alert')
    expect(alert).toHaveAttribute('data-no-local-rule-fallback', 'true')
    expect(alert).toHaveAttribute('data-no-public-list-request', 'true')
    expect(alert).toHaveAttribute('data-no-batch-check-request', 'true')
    expect(alert).toHaveAttribute('data-no-suggest-replacement-request', 'true')
    expect(screen.getByTestId('violation-check-input-card')).toHaveAttribute('data-ready-endpoint', '/script/violation/check')

    fireEvent.mouseDown(screen.getByLabelText('检测范围'))
    fireEvent.click(await screen.findByRole('option', { name: '直播' }))
    fireEvent.change(screen.getByLabelText('待检测文本'), { target: { value: '可以根治敏感肌' } })
    fireEvent.click(screen.getByRole('button', { name: '开始检测' }))

    await waitFor(() => {
      expect(scriptApi.violationCheck).toHaveBeenCalledWith('可以根治敏感肌', 'live')
    })
    expect(await screen.findByText('根治')).toBeInTheDocument()
    expect(screen.getByText('建议替换：改善')).toBeInTheDocument()
    expect(screen.getByText('原因：医疗功效宣称')).toBeInTheDocument()
    expect(screen.getByTestId('violation-check-kpi-total')).toHaveAttribute('data-contract-source', '/script/violation/check totalCount')
    expect(screen.getByTestId('violation-check-kpi-public')).toHaveAttribute('data-contract-source', 'local-derived source=public')
    expect(screen.getByTestId('violation-check-result-card')).toHaveAttribute('data-contract-source', '/script/violation/check')
    expect(screen.getByTestId('violation-check-hit-0')).toHaveAttribute('data-hit-source', 'public')
    expect(screen.getByTestId('violation-check-hit-0')).toHaveAttribute('data-hit-level', '3')
    expect(scriptApi.violationCheckBatch).not.toHaveBeenCalled()
    expect(scriptApi.violationPublicList).not.toHaveBeenCalled()
    expect(scriptApi.violationSuggestReplacement).not.toHaveBeenCalled()
  })

  it('ViolationCheckPage shows endpoint source on detection failure', async () => {
    vi.mocked(scriptApi.violationCheck).mockRejectedValueOnce(new Error('check down') as never)

    renderPage(<ViolationCheckPage />)

    fireEvent.change(screen.getByLabelText('待检测文本'), { target: { value: '可以根治敏感肌' } })
    fireEvent.click(screen.getByRole('button', { name: '开始检测' }))

    expect(await screen.findByText(/接口来源：\/script\/violation\/check/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/script\/violation-check; scope=all/)).toBeInTheDocument()
    expect(screen.getByText(/textLength=7/)).toBeInTheDocument()
    expect(screen.getByText(/不会用本地规则伪造检测结果/)).toBeInTheDocument()
    expect(screen.getByTestId('violation-check-error')).toHaveAttribute('data-no-local-rule-fallback', 'true')
    expect(screen.getByLabelText('待检测文本')).toHaveValue('可以根治敏感肌')
  })

  it('HybridSearchPage queries script hybrid endpoint and renders scores', async () => {
    renderPage(<HybridSearchPage />)

    expect(screen.getByRole('heading', { name: '话术混合搜索' })).toBeInTheDocument()
    expect(screen.getByText(/\/script\/search\/hybrid/)).toBeInTheDocument()
    const workbench = screen.getByTestId('hybrid-search-workbench')
    expect(workbench).toHaveAttribute('data-contract-scope', 'script-hybrid-search')
    expect(workbench).toHaveAttribute('data-ready-endpoints', '/script/search/hybrid')
    expect(workbench).toHaveAttribute('data-related-endpoints', '/script/search/semantic|/script/search/suggest')
    expect(workbench).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/search/hybrid|/script/search/analytics|/script/search/click-feedback|/ai/knowledge/search|/product/search|/live/search|/douyin/video/search',
    )
    expect(workbench).toHaveAttribute('data-no-static-fallback', 'true')
    expect(screen.getByTestId('hybrid-search-contract-alert')).toHaveAttribute('data-no-ai-global-search-request', 'true')
    expect(screen.getByTestId('hybrid-search-contract-alert')).toHaveAttribute('data-no-semantic-request', 'true')
    expect(screen.getByTestId('hybrid-search-contract-alert')).toHaveAttribute('data-no-suggest-request', 'true')
    expect(screen.getByTestId('hybrid-search-query-card')).toHaveAttribute('data-ready-endpoint', '/script/search/hybrid')
    expect(screen.getByTestId('hybrid-search-mode-semantic')).toHaveAttribute('data-vector-weight', '1')
    expect(screen.getByTestId('hybrid-search-mode-keyword')).toHaveAttribute('data-lexical-weight', '1')

    fireEvent.click(screen.getByRole('tab', { name: '语义搜索' }))
    fireEvent.change(screen.getByPlaceholderText('输入搜索内容...'), { target: { value: '屏障' } })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    await waitFor(() => {
      expect(scriptApi.searchHybrid).toHaveBeenCalledWith({
        query: '屏障',
        mode: 'semantic',
        category: undefined,
        style: undefined,
        rows: 20,
        topK: 20,
      })
    })
    expect(await screen.findByText('屏障修护话术')).toBeInTheDocument()
    expect(screen.getByText('融合 86%')).toBeInTheDocument()
    expect(screen.getByText('向量 92%')).toBeInTheDocument()
    expect(screen.getByText('42')).toBeInTheDocument()
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-mode', 'semantic')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-vector-weight', '1')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-lexical-weight', '0')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-result-count', '1')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-vector-hit-count', '1')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-bm25-hit-count', '1')
    expect(screen.getByTestId('hybrid-search-kpi-total')).toHaveAttribute('data-contract-source', '/script/search/hybrid total')
    expect(screen.getByTestId('hybrid-search-kpi-current-page')).toHaveAttribute('data-contract-source', 'scriptApi-normalized list.length')
    expect(screen.getByTestId('hybrid-search-kpi-vector-hit')).toHaveAttribute('data-contract-source', '/script/search/hybrid vectorScore')
    expect(screen.getByTestId('hybrid-search-result-0')).toHaveAttribute('data-contract-source', '/script/search/hybrid')
    expect(screen.getByTestId('hybrid-search-result-0')).toHaveAttribute('data-result-source', 'script')
    expect(screen.getByTestId('hybrid-search-result-0')).toHaveAttribute('data-hybrid-score', '0.86')
    expect(scriptApi.searchSemantic).not.toHaveBeenCalled()
    expect(scriptApi.searchSuggest).not.toHaveBeenCalled()
  })

  it('HybridSearchPage relies on scriptApi-normalized search results', async () => {
    vi.mocked(scriptApi.searchHybrid).mockResolvedValue({
      total: 4,
      pageNum: 0,
      pageSize: 20,
      searchTime: 88,
      list: [
        {
          id: 18,
          scriptId: 18,
          title: '归一混合搜索话术',
          content: '页面层只消费 scriptApi 已归一后的 list。',
          category: '彩妆',
          hybridScore: 0.77,
          vectorScore: 0.81,
          lexicalScore: 0.35,
          score: 0.77,
          source: 'script',
        },
      ],
    } as never)

    renderPage(<HybridSearchPage />)

    fireEvent.change(screen.getByPlaceholderText('输入搜索内容...'), { target: { value: '包装' } })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    expect(await screen.findByText('归一混合搜索话术')).toBeInTheDocument()
    expect(screen.getByText('融合 77%')).toBeInTheDocument()
    expect(screen.getByText('4')).toBeInTheDocument()
    expect(screen.getByText('88')).toBeInTheDocument()
  })

  it('HybridSearchPage keeps query context visible on search failure', async () => {
    vi.mocked(scriptApi.searchHybrid).mockRejectedValueOnce(new Error('vector down') as never)

    renderPage(<HybridSearchPage />)

    fireEvent.change(screen.getByPlaceholderText('输入搜索内容...'), { target: { value: '屏障' } })
    fireEvent.change(screen.getByLabelText('分类'), { target: { value: '护肤' } })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    expect(await screen.findByText(/搜索失败：vector down/)).toBeInTheDocument()
    expect(screen.getByText(/route=\/admin\/script\/hybrid-search; query=屏障; mode=hybrid; category=护肤; topK=20/)).toBeInTheDocument()
    expect(screen.getByText(/失败时不会展示静态搜索结果/)).toBeInTheDocument()
    expect(screen.getByTestId('hybrid-search-error')).toHaveAttribute('data-contract-source', '/script/search/hybrid')
    expect(screen.getByTestId('hybrid-search-error')).toHaveAttribute('data-no-static-fallback', 'true')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-result-count', '0')
    expect(screen.getByPlaceholderText('输入搜索内容...')).toHaveValue('屏障')
    expect(scriptApi.searchSemantic).not.toHaveBeenCalled()
    expect(scriptApi.searchSuggest).not.toHaveBeenCalled()
  })

  it('HybridSearchPage marks empty results as backend empty state without static fallback', async () => {
    vi.mocked(scriptApi.searchHybrid).mockResolvedValueOnce({
      total: 0,
      list: [],
      pageNum: 0,
      pageSize: 20,
      searchTime: 12,
    } as never)

    renderPage(<HybridSearchPage />)

    fireEvent.click(screen.getByRole('tab', { name: '关键词搜索' }))
    fireEvent.change(screen.getByPlaceholderText('输入搜索内容...'), { target: { value: '不存在的话术' } })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))

    expect(await screen.findByTestId('hybrid-search-empty')).toHaveAttribute('data-contract-source', '/script/search/hybrid')
    expect(screen.getByTestId('hybrid-search-empty')).toHaveAttribute('data-no-static-fallback', 'true')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-mode', 'keyword')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-vector-weight', '0')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-lexical-weight', '1')
    expect(screen.getByTestId('hybrid-search-workbench')).toHaveAttribute('data-result-count', '0')
    expect(scriptApi.searchSemantic).not.toHaveBeenCalled()
    expect(scriptApi.searchSuggest).not.toHaveBeenCalled()
  })
})
