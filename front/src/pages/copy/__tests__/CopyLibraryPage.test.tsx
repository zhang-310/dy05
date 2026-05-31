import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import CopyLibraryPage from '../CopyLibraryPage'
import { copyApi } from '@/api/copy'

vi.mock('@/api/copy', () => ({
  copyApi: {
    list: vi.fn(),
    semanticSearch: vi.fn(),
    usageList: vi.fn(),
    delete: vi.fn(),
    batchDelete: vi.fn(),
    batchSubmitApproval: vi.fn(),
    batchTag: vi.fn(),
    aiGenerate: vi.fn(),
    save: vi.fn(),
    exportCsv: vi.fn(),
    approvalSearch: vi.fn(),
    approvalStats: vi.fn(),
    approvalGet: vi.fn(),
    approvalSave: vi.fn(),
    approvalApprove: vi.fn(),
    approvalReject: vi.fn(),
    templateList: vi.fn(),
    templateSave: vi.fn(),
    templateDelete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('CopyLibraryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(copyApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          title: '护肤直播开场',
          content: '大家好，欢迎来到直播间，今天给大家带来修护精华',
          category: '护肤',
          tags: '开场话术,商品介绍',
          useCount: 12,
          rating: 8.8,
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(copyApi.usageList).mockResolvedValue([] as never)
    vi.mocked(copyApi.approvalSearch).mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 200 } as never)
    vi.mocked(copyApi.approvalStats).mockRejectedValue(new Error('后端未提供文案审批统计接口'))
    vi.mocked(copyApi.templateList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 11,
          templateName: '护肤模板',
          content: '今天主推 {productName}，适合 {skinType}',
          category: '护肤',
          variables: 'productName,skinType',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(copyApi.batchTag).mockRejectedValue(new Error('后端未提供文案批量打标签接口'))
    vi.mocked(copyApi.exportCsv).mockRejectedValue(new Error('后端未提供文案 CSV 导出接口'))
    vi.mocked(copyApi.aiGenerate).mockResolvedValue([
      {
        title: '补水精华直播文案',
        content: '姐妹们，换季干燥先看这瓶补水精华。',
        category: '护肤',
        tags: '商品介绍,补水',
        status: 0,
        source: 'copy_ai',
      },
    ] as never)
    vi.mocked(copyApi.save).mockResolvedValue(9 as never)
  })

  it('loads copy library list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(copyApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText(/大家好，欢迎来到直播间/)).toBeInTheDocument()
      expect(screen.getAllByText('开场话术').length).toBeGreaterThan(0)
      expect(screen.getByText('已审核')).toBeInTheDocument()
    })

    const root = screen.getByTestId('copy-library-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'copy-library-composite')
    expect(root).toHaveAttribute(
      'data-ready-endpoints',
      '/copy/library/search,/copy/library/save,/copy/library/delete,/copy/approval/search,/copy/approval/get,/copy/approval/save,/copy/template/search,/copy/template/save,/copy/template/delete,/copy/ai/generate',
    )
    expect(root).toHaveAttribute(
      'data-unsupported-actions',
      'semantic-vector-search,csv-export,batch-tag,usage-detail,approval-stats,approval-revise,server-side-batch-delete,batch-ai-save',
    )
    expect(root).toHaveAttribute('data-active-tab', 'library')

    const listWorkbench = screen.getByTestId('copy-library-list-workbench')
    expect(listWorkbench).toHaveAttribute('data-contract-scope', 'copy-library-list')
    expect(listWorkbench).toHaveAttribute('data-row-count', '1')
    expect(listWorkbench).toHaveAttribute('data-query-mode', 'keyword')
    expect(listWorkbench).toHaveAttribute('data-no-local-copy-fallback', 'true')
    expect(listWorkbench).toHaveAttribute('data-row-retained-on-action-error', 'true')
    expect(listWorkbench).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getByTestId('copy-library-list-contract-alert')).toHaveAttribute('data-contract-status', 'ready-with-explicit-degradation')
    expect(screen.queryByRole('button', { name: /导出/ })).not.toBeInTheDocument()
  })

  it('shows degradation message when semantic search is enabled', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(copyApi.list).toHaveBeenCalled())

    fireEvent.click(screen.getByText('语义'))

    expect(await screen.findByText(/后端未提供文案向量检索接口/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-library-semantic-degradation')).toHaveAttribute('data-no-vector-request', 'true')
    expect(screen.getByTestId('copy-library-list-workbench')).toHaveAttribute('data-query-mode', 'keyword')
    expect(copyApi.semanticSearch).not.toHaveBeenCalled()
  })

  it('shows retryable error when library list fails', async () => {
    vi.mocked(copyApi.list).mockRejectedValueOnce(new Error('copy backend down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/\/copy\/library\/search 文案库加载失败：copy backend down/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-library-list-error')).toHaveAttribute('data-no-local-copy-fallback', 'true')
    expect(screen.getByTestId('copy-library-list-error')).toHaveAttribute('data-source-endpoint', '/copy/library/search')
  })

  it('shows source endpoint when save fails and keeps edit dialog open', async () => {
    vi.mocked(copyApi.save).mockRejectedValue(new Error('save down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(copyApi.list).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: '新建' }))
    const dialog = screen.getByRole('dialog', { name: '新建文案' })
    fireEvent.change(within(dialog).getByRole('textbox', { name: /标题/ }), { target: { value: '失败文案' } })
    fireEvent.change(within(dialog).getByRole('textbox', { name: /文案内容/ }), { target: { value: '保存失败时保留输入' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect(await within(dialog).findByText(/\/copy\/library\/save 保存失败：save down/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-library-edit-dialog')).toHaveAttribute('data-ready-endpoint', '/copy/library/save')
    expect(screen.getByTestId('copy-library-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByRole('dialog', { name: '新建文案' })).toBeInTheDocument()
  })

  it('shows source endpoint when delete or submit approval fails without removing row', async () => {
    vi.mocked(copyApi.delete).mockRejectedValue(new Error('delete down'))
    vi.mocked(copyApi.batchSubmitApproval).mockRejectedValue(new Error('approval down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await screen.findByText(/大家好，欢迎来到直播间/)
    fireEvent.click(screen.getByRole('button', { name: '发审批' }))
    expect(await screen.findByText(/\/copy\/approval\/save 提交审批失败：approval down/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-library-action-error')).toHaveAttribute('data-row-retained-on-action-error', 'true')

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(await screen.findByText(/\/copy\/library\/delete 删除失败：delete down/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-library-action-error')).toHaveAttribute('data-no-local-copy-mutation', 'true')
    expect(screen.getByText(/大家好，欢迎来到直播间/)).toBeInTheDocument()
  })

  it('shows source endpoint when AI generation fails', async () => {
    vi.mocked(copyApi.aiGenerate).mockRejectedValue(new Error('ai down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(copyApi.list).toHaveBeenCalled())
    fireEvent.click(screen.getByRole('button', { name: /AI生成/ }))
    const dialog = screen.getByRole('dialog', { name: 'AI 生成文案' })
    fireEvent.change(within(dialog).getByLabelText('生成提示词'), { target: { value: '补水精华' } })
    fireEvent.click(within(dialog).getByRole('button', { name: 'AI 生成 ✨' }))

    expect(await within(dialog).findByText(/\/copy\/ai\/generate AI 生成失败：ai down/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-library-ai-error')).toHaveAttribute('data-no-candidate-clear', 'true')
  })

  it('generates copy through real AI endpoint and saves a candidate to library', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await waitFor(() => expect(copyApi.list).toHaveBeenCalled())

    fireEvent.click(screen.getByRole('button', { name: /AI生成/ }))
    const dialog = screen.getByRole('dialog', { name: 'AI 生成文案' })
    expect(dialog).toBeInTheDocument()
    expect(screen.getByText(/真实调用 `\/copy\/ai\/generate`/)).toBeInTheDocument()
    expect(screen.getByTestId('copy-library-ai-dialog-workbench')).toHaveAttribute('data-unsupported-actions', 'batch-ai-save')
    expect(screen.getByTestId('copy-library-ai-contract-alert')).toHaveAttribute('data-contract-status', 'generate-then-explicit-save')

    fireEvent.change(within(dialog).getByLabelText('生成提示词'), { target: { value: '补水精华' } })
    fireEvent.change(within(dialog).getByLabelText(/核心关键词/), { target: { value: '补水,精华' } })
    fireEvent.mouseDown(within(dialog).getByRole('combobox', { name: '文案类型' }))
    fireEvent.click(screen.getByRole('option', { name: '商品介绍' }))
    fireEvent.mouseDown(within(dialog).getByRole('combobox', { name: '分类' }))
    fireEvent.click(screen.getByRole('option', { name: '护肤' }))

    fireEvent.click(within(dialog).getByRole('button', { name: 'AI 生成 ✨' }))

    await waitFor(() => {
      expect(copyApi.aiGenerate).toHaveBeenCalledWith(expect.objectContaining({
        prompt: '补水精华',
        category: '护肤',
        copyType: '商品介绍',
        keywords: '补水,精华',
      }))
      expect(screen.getByText('补水精华直播文案')).toBeInTheDocument()
      expect(screen.getByText(/换季干燥先看这瓶补水精华/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('copy-library-ai-candidate-card')).toHaveAttribute('data-contract-status', 'generated-not-saved')

    fireEvent.click(screen.getByLabelText('保存到文案库'))

    await waitFor(() => {
      expect(copyApi.save).toHaveBeenCalledWith(expect.objectContaining({
        title: '补水精华直播文案',
        content: '姐妹们，换季干燥先看这瓶补水精华。',
        category: '护肤',
        tags: '商品介绍,补水',
        status: 0,
      }))
    })
  })

  it('does not call unsupported usage or approval stats endpoints and derives stats locally', async () => {
    vi.mocked(copyApi.approvalSearch).mockResolvedValue({
      total: 3,
      list: [
        {
          id: 21,
          copyId: 1,
          approvalStatus: 2,
          comments: '待审核',
          copyTitle: '待审文案',
          copyContent: '这条文案需要审核',
          approvalTime: '',
          createTime: '2020-01-01 10:00:00',
        },
        {
          id: 22,
          copyId: 2,
          approvalStatus: 1,
          comments: '通过',
          copyTitle: '已通过文案',
          copyContent: '这条文案已经通过',
          approvalTime: new Date().toISOString(),
          createTime: '2026-04-10 10:00:00',
        },
        {
          id: 23,
          copyId: 3,
          approvalStatus: 0,
          comments: '拒绝',
          copyTitle: '已拒绝文案',
          copyContent: '这条文案已经拒绝',
          approvalTime: '2026-04-10 12:00:00',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 200,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await screen.findByText(/大家好，欢迎来到直播间/)
    fireEvent.click(screen.getByRole('button', { name: /大家好，欢迎来到直播间/ }))
    expect(screen.getByTestId('copy-library-preview-drawer')).toHaveAttribute('data-contract-status', 'server-source-preview')
    fireEvent.click(screen.getByRole('tab', { name: '使用记录' }))

    expect(await screen.findByTestId('copy-library-usage-degradation')).toHaveAttribute('data-no-usage-request', 'true')
    expect(copyApi.usageList).not.toHaveBeenCalled()

    fireEvent.click(within(screen.getByTestId('copy-library-preview-drawer')).getAllByRole('button')[0])
    await waitFor(() => {
      expect(screen.queryByTestId('copy-library-preview-drawer')).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('tab', { name: '审批看板' }))
    expect(await screen.findByTestId('copy-library-approval-workbench')).toHaveAttribute('data-stat-source', 'local-derived')
    expect(screen.getByTestId('copy-library-approval-workbench')).toHaveAttribute('data-no-local-approval-fallback', 'true')
    await waitFor(() => {
      expect(screen.getByTestId('copy-library-approval-workbench')).toHaveAttribute('data-pending-count', '1')
    })
    expect(screen.getByTestId('copy-library-approval-stats-degradation')).toHaveAttribute('data-no-stats-request', 'true')
    expect(screen.getAllByTestId('copy-library-sla-stat-surface')[0]).toHaveAttribute('data-summary-metric', 'pending')
    expect(screen.getAllByTestId('copy-library-sla-stat-surface')[0]).toHaveAttribute('data-contract-status', 'local-derived')
    expect(screen.getAllByTestId('copy-library-kanban-header-surface')[0]).toHaveAttribute('data-source-endpoint', '/copy/approval/search')
    expect(copyApi.approvalStats).not.toHaveBeenCalled()
  })

  it('marks template tab contract and hides unsupported batch/export actions', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await screen.findByText(/大家好，欢迎来到直播间/)
    fireEvent.click(screen.getByRole('tab', { name: '文案模板' }))

    const templateWorkbench = await screen.findByTestId('copy-library-template-workbench')
    expect(templateWorkbench).toHaveAttribute('data-contract-scope', 'copy-template-inline')
    expect(templateWorkbench).toHaveAttribute('data-ready-endpoints', '/copy/template/search,/copy/template/save,/copy/template/delete')
    expect(templateWorkbench).toHaveAttribute('data-unsupported-endpoint', '/copy/template/batch-delete')
    expect(templateWorkbench).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(screen.getByTestId('copy-library-template-contract-alert')).toHaveAttribute('data-contract-status', 'ready-with-explicit-degradation')
    expect(screen.queryByRole('button', { name: /导出|批量/ })).not.toBeInTheDocument()
  })

  it('marks inline approval and template source errors as no-local fallbacks', async () => {
    vi.mocked(copyApi.approvalSearch).mockRejectedValueOnce(new Error('approval list down'))
    vi.mocked(copyApi.templateList).mockRejectedValueOnce(new Error('template list down'))

    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await screen.findByText(/大家好，欢迎来到直播间/)
    fireEvent.click(screen.getByRole('tab', { name: '审批看板' }))

    expect(await screen.findByTestId('copy-library-approval-list-error')).toHaveAttribute('data-no-local-approval-fallback', 'true')
    expect(screen.getByTestId('copy-library-approval-list-error')).toHaveAttribute('data-source-endpoint', '/copy/approval/search')

    fireEvent.click(screen.getByRole('tab', { name: '文案模板' }))
    expect(await screen.findByTestId('copy-library-template-list-error')).toHaveAttribute('data-no-local-template-fallback', 'true')
    expect(screen.getByTestId('copy-library-template-list-error')).toHaveAttribute('data-source-endpoint', '/copy/template/search')
  })

  it('uses theme-aware approval and template surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(copyApi.approvalSearch).mockResolvedValue({
      total: 2,
      list: [
        {
          id: 21,
          copyId: 1,
          approvalStatus: 2,
          comments: '待审核',
          copyTitle: '逾期文案',
          copyContent: '这条文案需要尽快审核',
          approvalTime: '',
          createTime: '2020-01-01 10:00:00',
        },
        {
          id: 22,
          copyId: 2,
          approvalStatus: 1,
          comments: '通过',
          copyTitle: '已通过文案',
          copyContent: '这条文案已经通过',
          approvalTime: '2026-04-10 12:00:00',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 200,
    } as never)

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <CopyLibraryPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    await screen.findByText(/大家好，欢迎来到直播间/)
    fireEvent.click(screen.getByRole('tab', { name: '审批看板' }))

    expect(await screen.findByTestId('copy-library-overdue-approval-card')).toBeInTheDocument()
    expect(screen.getAllByTestId('copy-library-kanban-header-surface')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })
    expect(screen.getAllByTestId('copy-library-sla-stat-surface')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(255, 255, 255)',
    })

    fireEvent.click(screen.getByRole('tab', { name: '文案模板' }))
    await screen.findByText('护肤模板')
    fireEvent.click(screen.getByRole('button', { name: '预览' }))

    expect(await screen.findByTestId('copy-template-preview-content-surface')).not.toHaveStyle({
      backgroundColor: 'rgb(250, 250, 250)',
    })
    expect(screen.getAllByTestId('copy-template-variable-chip-surface')[0]).not.toHaveStyle({
      backgroundColor: 'rgb(255, 243, 224)',
    })
  })
})
