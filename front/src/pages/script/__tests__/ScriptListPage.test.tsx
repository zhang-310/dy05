import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import ScriptListPage from '../ScriptListPage'
import {
  scriptApi,
  type BatchCheckRow,
  type ScriptItem,
  type ScriptTemplate,
  type ViolationWord,
} from '@/api/script'

vi.mock('@/api/script', () => ({
  scriptApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    searchSemantic: vi.fn(),
    violationList: vi.fn(),
    violationSave: vi.fn(),
    violationDelete: vi.fn(),
    violationToggle: vi.fn(),
    violationCheck: vi.fn(),
    violationCheckBatch: vi.fn(),
    templateSearch: vi.fn(),
    templateSave: vi.fn(),
    templateDelete: vi.fn(),
    searchHybrid: vi.fn(),
    searchSuggest: vi.fn(),
  },
}))

const toast = vi.fn()
const writeText = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({
      rows,
      columns,
      searchSlot,
      slots,
      showExport,
      onPaginationModelChange,
      onRowSelectionModelChange,
    }: any) => {
      const toolbar = searchSlot ?? slots?.toolbar?.()
      return (
        <div data-testid="standard-data-grid" data-show-export={String(showExport)}>
          <div>{toolbar}</div>
          <button type="button" onClick={() => onPaginationModelChange?.({ page: 1, pageSize: 30 })}>
            mock-next-page
          </button>
          <button type="button" onClick={() => onRowSelectionModelChange?.(rows.map((r: any) => r.id))}>
            mock-select-all
          </button>
          <table>
            <tbody>
              {rows.map((row: any) => (
                <tr key={row.id}>
                  {columns.map((col: any) => (
                    <td key={col.field}>
                      {col.renderCell
                        ? col.renderCell({
                            row,
                            value: row[col.field],
                            field: col.field,
                          })
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
      )
    },
  }
})

const scriptRow: ScriptItem = {
  id: 1,
  userId: 1,
  title: '敏感肌直播开场',
  content: '欢迎来到直播间，今天讲{产品名}修护屏障。',
  category: '直播',
  tags: '敏感肌,修护',
  scriptType: 'manual',
  source: 'manual',
  industry: '成分党',
  duration: 60,
  useCount: 8,
  rating: 4.8,
  status: 1,
  createTime: '2026-05-01 10:00:00',
  updateTime: '2026-05-12 10:00:00',
}

const violationWord: ViolationWord = {
  id: 10,
  word: '最有效',
  category: '功效宣称',
  severity: 1,
  replacement: '帮助改善',
  status: 1,
  createTime: '2026-05-01 10:00:00',
}

const template: ScriptTemplate = {
  id: 20,
  templateName: '种草变量模板',
  templateContent: '今天推荐{产品名}，核心卖点是{卖点}',
  scene: '新品',
  industry: '护肤',
  tags: '{产品名},{卖点}',
  useCount: 3,
  status: 1,
  createTime: '2026-05-01 10:00:00',
}

const batchRows: BatchCheckRow[] = [
  { id: 1, title: '敏感肌直播开场', count: 1, maxSeverity: 1, status: '需修改' },
]

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <ScriptListPage />
    </MemoryRouter>,
  )
}

function expectPrimaryAction(testId: string) {
  const action = screen.getByTestId(testId)
  expect(action).toHaveAttribute('data-surface-tone', 'primary')
  expect(action.outerHTML).not.toContain('#000')
  expect(action.outerHTML).not.toContain('var(--color-primary)')
}

describe('ScriptListPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(scriptApi.list).mockResolvedValue({
      total: 1,
      list: [scriptRow],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(scriptApi.save).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.delete).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.searchSemantic).mockResolvedValue({
      total: 1,
      list: [{ ...scriptRow, title: '语义命中话术' }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(scriptApi.violationList).mockResolvedValue({
      total: 1,
      list: [violationWord],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(scriptApi.violationSave).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.violationDelete).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.violationToggle).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.violationCheck).mockResolvedValue({
      score: 68,
      violations: [violationWord],
    } as never)
    vi.mocked(scriptApi.violationCheckBatch).mockResolvedValue(batchRows as never)
    vi.mocked(scriptApi.templateSearch).mockResolvedValue({
      total: 1,
      list: [template],
      pageNum: 0,
      pageSize: 24,
    } as never)
    vi.mocked(scriptApi.templateSave).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.templateDelete).mockResolvedValue(undefined as never)
    vi.mocked(scriptApi.searchHybrid).mockResolvedValue({
      total: 1,
      list: [
        {
          ...scriptRow,
          id: 30,
          title: '屏障修护混合结果',
          content: '来自话术库和知识库的融合结果',
          score: 0.91,
          source: 'script',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(scriptApi.searchSuggest).mockResolvedValue(['屏障修护', '屏障水乳'] as never)

    Object.defineProperty(navigator, 'clipboard', {
      value: { writeText },
      configurable: true,
    })
  })

  it('manages script library search, preview, compliance, save, delete, and batch check', async () => {
    renderPage()

    expect(screen.getByTestId('script-list-workbench')).toHaveAttribute('data-contract-scope', 'script-workbench-composite')
    expect(screen.getByTestId('script-list-workbench')).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/script/export'))
    expect(screen.getByTestId('script-library-tab')).toHaveAttribute('data-ready-endpoints', '/script/list|/script/save|/script/delete')
    expect(screen.getByTestId('script-library-tab')).toHaveAttribute('data-no-static-list-fallback', 'true')
    expect(screen.getByTestId('script-library-contract-alert')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(screen.getByTestId('standard-data-grid')).toHaveAttribute('data-show-export', 'false')
    expect(await screen.findByText('敏感肌直播开场')).toBeInTheDocument()
    expect(screen.getByText(/真实筛选字段/)).toBeInTheDocument()
    expect(scriptApi.list).toHaveBeenCalledWith({ page: 0, rows: 20, keyword: '', source: '', category: '' })
    expectPrimaryAction('script-library-search-action')
    expectPrimaryAction('script-library-create-action')

    fireEvent.change(screen.getByPlaceholderText('搜索话术标题/内容'), {
      target: { value: '屏障' },
    })
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))
    await waitFor(() => {
      expect(scriptApi.list).toHaveBeenLastCalledWith({ page: 0, rows: 20, keyword: '屏障', source: '', category: '' })
    })

    fireEvent.click(screen.getByRole('checkbox', { name: '语义搜索' }))
    await waitFor(() => {
      expect(scriptApi.searchSemantic).toHaveBeenCalledWith({ query: '屏障', page: 0, rows: 20 })
    })

    fireEvent.click(screen.getByRole('button', { name: 'mock-next-page' }))
    await waitFor(() => {
      expect(scriptApi.searchSemantic).toHaveBeenLastCalledWith({ query: '屏障', page: 1, rows: 30 })
    })

    fireEvent.click(screen.getByText('语义命中话术'))
    expect(screen.getByRole('tab', { name: '话术内容' })).toBeInTheDocument()
    expect(screen.getByText(/欢迎来到直播间/)).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('复制内容'))
    expect(writeText).toHaveBeenCalledWith(scriptRow.content)
    expect(toast).toHaveBeenCalledWith('已复制', 'success')

    fireEvent.click(screen.getByRole('tab', { name: '合规检测' }))
    fireEvent.click(screen.getByRole('button', { name: '开始检测' }))
    await waitFor(() => {
      expect(scriptApi.violationCheck).toHaveBeenCalledWith(scriptRow.content)
      expect(screen.getByText('68分')).toBeInTheDocument()
      expect(screen.getByText('最有效')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByLabelText('编辑'))
    expect(screen.getByRole('dialog', { name: '编辑话术' })).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: /话术标题/ }), {
      target: { value: '敏感肌直播开场 Pro' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    await waitFor(() => {
      expect(scriptApi.save).toHaveBeenCalled()
      expect(vi.mocked(scriptApi.save).mock.calls[0][0]).toEqual(
        expect.objectContaining({ id: 1, title: '敏感肌直播开场 Pro' }),
      )
      expect(toast).toHaveBeenCalledWith('保存成功', 'success')
    })
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '编辑话术' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '关闭预览' }))
    await waitFor(() => {
      expect(screen.queryByRole('heading', { name: '语义命中话术' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: 'mock-select-all' }))
    fireEvent.click(await screen.findByRole('button', { name: '批量检测 (1)' }))
    await waitFor(() => {
      expect(scriptApi.violationCheckBatch).toHaveBeenCalledWith([
        { key: '1', text: '欢迎来到直播间，今天讲{产品名}修护屏障。' },
      ])
      expect(screen.getByRole('dialog', { name: '批量合规检测结果' })).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '关闭' }))
    fireEvent.click(screen.getAllByLabelText('删除')[0])
    expect(screen.getByRole('dialog', { name: '确认删除' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(vi.mocked(scriptApi.delete).mock.calls[0][0]).toBe(1)
      expect(toast).toHaveBeenCalledWith('删除成功', 'success')
    })
  })

  it('keeps script library operation errors visible without fabricating state', async () => {
    vi.mocked(scriptApi.list).mockRejectedValueOnce(new Error('script list down') as never)
    vi.mocked(scriptApi.save).mockRejectedValueOnce(new Error('save denied') as never)
    vi.mocked(scriptApi.delete).mockRejectedValueOnce(new Error('delete denied') as never)
    vi.mocked(scriptApi.violationCheck).mockRejectedValueOnce(new Error('check down') as never)
    vi.mocked(scriptApi.violationCheckBatch).mockRejectedValueOnce(new Error('batch down') as never)

    renderPage()

    expect(await screen.findByTestId('script-library-list-error')).toHaveAttribute('data-no-static-list-fallback', 'true')
    expect(screen.getByText(/话术列表加载失败：script list down/)).toBeInTheDocument()
    expect(screen.getByText(/接口来源：\/script\/list/)).toBeInTheDocument()

    vi.mocked(scriptApi.list).mockResolvedValue({
      total: 1,
      list: [scriptRow],
      pageNum: 0,
      pageSize: 20,
    } as never)
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    expect(await screen.findByText('敏感肌直播开场')).toBeInTheDocument()

    fireEvent.click(screen.getByText('敏感肌直播开场'))
    fireEvent.click(screen.getByRole('tab', { name: '合规检测' }))
    fireEvent.click(screen.getByRole('button', { name: '开始检测' }))
    expect(await screen.findByTestId('script-library-compliance-error')).toHaveAttribute('data-no-local-compliance-fallback', 'true')
    expect(screen.getByText(/话术合规检测失败：check down/)).toBeInTheDocument()
    expect(screen.getByText(/接口来源：\/script\/violation\/check/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '话术内容' }))
    fireEvent.click(screen.getAllByLabelText('编辑')[1])
    fireEvent.change(screen.getByRole('textbox', { name: /话术标题/ }), {
      target: { value: '失败保存标题' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    expect(await screen.findByTestId('script-library-save-error')).toHaveAttribute('data-no-local-save-fallback', 'true')
    expect(screen.getByText(/话术保存失败：save denied/)).toBeInTheDocument()
    expect(screen.getByText(/接口来源：\/script\/save/)).toBeInTheDocument()
    expect(screen.getByRole('dialog', { name: '编辑话术' })).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '编辑话术' })).not.toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: 'mock-select-all' }))
    fireEvent.click(await screen.findByRole('button', { name: '批量检测 (1)' }))
    expect(await screen.findByTestId('script-library-batch-check-error')).toHaveAttribute('data-no-local-compliance-fallback', 'true')
    expect(screen.getByText(/批量合规检测失败：batch down/)).toBeInTheDocument()
    expect(screen.getByText(/接口来源：\/script\/violation\/check-batch/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '关闭' }))
    fireEvent.click(screen.getAllByLabelText('删除')[0])
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/删除失败：delete denied/)).toBeInTheDocument()
    expect(screen.getByText(/接口来源：\/script\/delete/)).toBeInTheDocument()
    expect(screen.getByText('敏感肌直播开场')).toBeInTheDocument()
  })

  it('manages violation words and runs single text compliance detection', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '违规词管理' }))
    expect(await screen.findByTestId('script-violation-tab')).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/script/admin/violation/list'))
    expect(screen.getByTestId('script-violation-tab')).toHaveAttribute('data-no-static-violation-fallback', 'true')
    expect(screen.getByTestId('script-violation-tab')).toHaveAttribute('data-no-server-export-request', 'true')
    expect(await screen.findByText('最有效')).toBeInTheDocument()
    expect(screen.getByTestId('standard-data-grid')).toHaveAttribute('data-show-export', 'false')
    expect(screen.getByTestId('script-violation-check-panel')).toHaveAttribute('data-contract-source', '/script/violation/check')
    expectPrimaryAction('script-violation-create-action')
    expectPrimaryAction('script-violation-check-action')
    expect(scriptApi.violationList).toHaveBeenCalledWith({
      page: 0,
      rows: 20,
      keyword: '',
      severity: undefined,
    })

    fireEvent.change(screen.getByPlaceholderText('搜索违规词'), {
      target: { value: '有效' },
    })
    await waitFor(() => {
      expect(scriptApi.violationList).toHaveBeenLastCalledWith({
        page: 0,
        rows: 20,
        keyword: '有效',
        severity: undefined,
      })
    })
    expect(await screen.findByText('最有效')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('checkbox'))
    await waitFor(() => {
      expect(scriptApi.violationSave).toHaveBeenCalledWith(expect.objectContaining({
        id: 10,
        word: '最有效',
        level: 1,
        status: 0,
      }))
    })

    fireEvent.change(screen.getByPlaceholderText('输入文本进行检测...'), {
      target: { value: '这是最有效的护肤品' },
    })
    fireEvent.click(screen.getByRole('button', { name: '开始检测' }))
    await waitFor(() => {
      expect(scriptApi.violationCheck).toHaveBeenCalledWith('这是最有效的护肤品')
      expect(screen.getByTestId('script-violation-check-result')).toHaveAttribute('data-contract-source', '/script/violation/check')
      expect(screen.getByText('→ 帮助改善')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '新增违规词' }))
    expect(screen.getByRole('dialog', { name: '新增违规词' })).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: /违规词/ }), {
      target: { value: '根治' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: /建议替换词/ }), {
      target: { value: '改善' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    await waitFor(() => {
      expect(scriptApi.violationSave).toHaveBeenCalled()
      expect(vi.mocked(scriptApi.violationSave).mock.calls.some(([payload]) => (
        payload.word === '根治' && payload.replacement === '改善'
      ))).toBe(true)
      expect(toast).toHaveBeenCalledWith('保存成功', 'success')
    })

    fireEvent.click(screen.getAllByLabelText('删除')[0])
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(scriptApi.violationDelete).toHaveBeenCalled()
      expect(vi.mocked(scriptApi.violationDelete).mock.calls[0][0]).toBe(10)
      expect(toast).toHaveBeenCalledWith('删除成功', 'success')
    })
  })

  it('uses templates, replaces variables, saves edits, and deletes templates', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '模板库' }))
    expect(await screen.findByTestId('script-template-tab')).toHaveAttribute('data-ready-endpoints', '/script/template/search|/script/template/save|/script/template/delete')
    expect(screen.getByTestId('script-template-tab')).toHaveAttribute('data-no-admin-template-endpoint', 'true')
    expect(screen.getByTestId('script-template-tab')).toHaveAttribute('data-no-use-count-request', 'true')
    expect(await screen.findByText('种草变量模板')).toBeInTheDocument()
    expect(screen.getByTestId('script-template-card')).toHaveAttribute('data-contract-source', '/script/template/search')
    expectPrimaryAction('script-template-create-action')
    expect(scriptApi.templateSearch).toHaveBeenCalledWith({ page: 0, rows: 24, keyword: '', scene: '' })

    fireEvent.change(screen.getByPlaceholderText('搜索模板名称'), {
      target: { value: '种草' },
    })
    await waitFor(() => {
      expect(scriptApi.templateSearch).toHaveBeenLastCalledWith({ page: 0, rows: 24, keyword: '种草', scene: '' })
    })
    expect(await screen.findByText('种草变量模板')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '使用模板' }))
    expect(screen.getByRole('dialog', { name: '使用模板：种草变量模板' })).toBeInTheDocument()
    expect(screen.getByTestId('script-template-use-dialog')).toHaveAttribute('data-contract-source', 'local-template-variable-fill')
    expect(screen.getByTestId('script-template-use-dialog')).toHaveAttribute('data-no-use-count-request', 'true')
    expectPrimaryAction('script-template-apply-action')
    fireEvent.change(screen.getByRole('textbox', { name: /产品名/ }), {
      target: { value: '屏障霜' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: /卖点/ }), {
      target: { value: '温和修护' },
    })
    fireEvent.click(screen.getByRole('button', { name: '填充并复制' }))
    expect(writeText).toHaveBeenCalledWith('今天推荐屏障霜，核心卖点是温和修护')
    expect(toast).toHaveBeenCalledWith('已填充并复制到剪贴板', 'success')
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: /使用模板/ })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    expect(screen.getByRole('dialog', { name: '编辑模板' })).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: /模板名称/ }), {
      target: { value: '种草变量模板 Pro' },
    })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))
    await waitFor(() => {
      expect(scriptApi.templateSave).toHaveBeenCalled()
      expect(vi.mocked(scriptApi.templateSave).mock.calls[0][0]).toEqual(
        expect.objectContaining({ id: 20, templateName: '种草变量模板 Pro' }),
      )
      expect(toast).toHaveBeenCalledWith('保存成功', 'success')
    })
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '编辑模板' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(scriptApi.templateDelete).toHaveBeenCalled()
      expect(vi.mocked(scriptApi.templateDelete).mock.calls[0][0]).toBe(20)
      expect(toast).toHaveBeenCalledWith('删除成功', 'success')
    })
  })

  it('runs hybrid search with suggestions, source toggles, and mode changes', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '混合搜索' }))
    expect(screen.getByTestId('script-list-hybrid-tab')).toHaveAttribute('data-ready-endpoints', '/script/search/suggest|/script/search/hybrid')
    expect(screen.getByTestId('script-list-hybrid-tab')).toHaveAttribute('data-no-static-search-results', 'true')
    expect(screen.getByTestId('script-list-hybrid-tab')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('search-analytics'))
    expectPrimaryAction('script-hybrid-search-action')
    const searchBox = screen.getByPlaceholderText('输入关键词搜索...')
    fireEvent.change(searchBox, { target: { value: '屏障' } })
    await waitFor(() => {
      expect(scriptApi.searchSuggest).toHaveBeenCalledWith('屏障')
      expect(screen.getByText('修护')).toBeInTheDocument()
    })

    fireEvent.mouseDown(screen.getByText('修护'))
    await waitFor(() => {
      expect(scriptApi.searchHybrid).toHaveBeenCalledWith({
        query: '屏障修护',
        mode: 'hybrid',
        sources: ['script', 'knowledge', 'product'],
        rows: 20,
      })
      expect(screen.getByTestId('script-hybrid-result-card')).toHaveAttribute('data-contract-source', '/script/search/hybrid')
      expect(screen.getByTestId('script-hybrid-result-card')).toHaveAttribute('data-result-source', 'script')
      expect(screen.getByText('混合结果')).toBeInTheDocument()
    })

    fireEvent.click(screen.getByText('语义搜索'))
    fireEvent.click(screen.getByLabelText('知识库'))
    fireEvent.click(screen.getByRole('button', { name: '搜索' }))
    await waitFor(() => {
      expect(scriptApi.searchHybrid).toHaveBeenLastCalledWith({
        query: '屏障修护',
        mode: 'semantic',
        sources: ['script', 'product'],
        rows: 20,
      })
    })
  })
})
