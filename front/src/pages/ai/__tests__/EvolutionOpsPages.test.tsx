import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import EvolutionReviewPage from '../EvolutionReviewPage'
import EvolutionTasksPage from '../EvolutionTasksPage'
import EvolutionTopicPage from '../EvolutionTopicPage'
import IndustryBrainDiagnosisPage from '../IndustryBrainDiagnosisPage'
import { aiApi } from '@/api/ai'
import { brainApi } from '@/api/brain'

vi.mock('@/api/ai', () => ({
  aiApi: {
    evolutionReviewList: vi.fn(),
    evolutionReviewStats: vi.fn(),
    evolutionReviewApprove: vi.fn(),
    evolutionReviewReject: vi.fn(),
    evolveTaskList: vi.fn(),
    evolveTaskTrigger: vi.fn(),
    evolveTaskReport: vi.fn(),
    evolveTaskCancel: vi.fn(),
    topicList: vi.fn(),
    topicSave: vi.fn(),
    topicDelete: vi.fn(),
    topicImport: vi.fn(),
  },
}))

vi.mock('@/api/brain', () => ({
  brainApi: {
    contentDiagnosis: vi.fn(),
    productDiagnosis: vi.fn(),
    rhythmDiagnosis: vi.fn(),
    accountDiagnose: vi.fn(),
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
    StandardDataGrid: ({ rows, columns }: any) => (
      <div>
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

function renderPageWithTheme(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('Evolution ops pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')

    vi.mocked(aiApi.evolutionReviewList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 21,
          evolveTaskId: 7,
          contentPreview: '建议补充竞品对比证据链',
          qualityScore: 7,
          reviewStatus: 'PENDING',
          createTime: '2026-05-21 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.evolutionReviewStats).mockResolvedValue({
      pending: 1,
      approvedCount: 2,
      rejected: 1,
      revisedCount: 1,
      approvalRate7d: 66.7,
    } as never)
    vi.mocked(aiApi.evolutionReviewApprove).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.evolutionReviewReject).mockResolvedValue(undefined as never)

    vi.mocked(aiApi.evolveTaskList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 31,
          taskType: 'gap',
          status: 1,
          progress: 50,
          kbId: 7,
          createTime: '2026-05-21 09:00:00',
          scoreTotal: 8,
        },
      ],
      pageNum: 0,
      pageSize: 50,
    } as never)
    vi.mocked(aiApi.evolveTaskTrigger).mockResolvedValue({
      taskId: 'job-1',
      message: '进化任务已触发',
      taskType: 'gap',
    } as never)
    vi.mocked(aiApi.evolveTaskReport).mockResolvedValue({
      methodologySection: '方法论报告',
      fullContent: '完整报告',
    } as never)
    vi.mocked(aiApi.evolveTaskCancel).mockResolvedValue(undefined as never)

    vi.mocked(aiApi.topicList).mockResolvedValue([
      {
        id: 41,
        topicName: '屏障修护脚本证据链',
        category: 'script',
        priority: 1,
        usedCount: 3,
      },
    ] as never)
    vi.mocked(aiApi.topicSave).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.topicDelete).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.topicImport).mockResolvedValue({ imported: 1 } as never)

    vi.mocked(brainApi.contentDiagnosis).mockResolvedValue({
      diagnosisType: 'content',
      status: 'success',
      analysis: '内容诊断结果',
      strengths: ['转化证据清晰'],
      actions: ['补充竞品对比'],
      tokensUsed: 100,
    } as never)
    vi.mocked(brainApi.productDiagnosis).mockResolvedValue({
      diagnosisType: 'product',
      status: 'success',
      analysis: '选品诊断结果',
    } as never)
    vi.mocked(brainApi.rhythmDiagnosis).mockResolvedValue({
      diagnosisType: 'rhythm',
      status: 'success',
      analysis: '节奏诊断结果',
    } as never)
    vi.mocked(brainApi.accountDiagnose).mockResolvedValue({
      accountId: 5,
      positioningClarity: 0.8,
      contentCompetitiveness: 0.7,
      growthHealth: 0.6,
      riskLevel: 'LOW',
      suggestedPriorities: ['强化成分证据'],
      summary: '账号诊断摘要',
    } as never)
  })

  it('EvolutionReviewPage passes approve comment and documents review contract', async () => {
    renderPage(<EvolutionReviewPage />)

    expect(screen.getByRole('heading', { name: '进化内容审核' })).toBeInTheDocument()
    expect(screen.getByTestId('evolution-review-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/evolution-review/list,/ai/evolution-review/stats,/ai/evolution-review/approve,/ai/evolution-review/reject',
    )
    expect(screen.getByTestId('evolution-review-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/evolution-review/mock,/ai/evolution-review/local-list,/ai/evolution-review/static-stats,/ai/evolution-review/local-approve,/ai/evolution-review/local-reject',
    )
    expect(screen.getByTestId('evolution-review-page')).toHaveAttribute('data-no-local-review-fallback', 'true')
    expect(screen.getByTestId('evolution-review-page')).toHaveAttribute('data-no-static-stats-fallback', 'true')
    expect(screen.getByText(/\/ai\/evolution-review\/list/)).toBeInTheDocument()

    await waitFor(() => {
      expect(aiApi.evolutionReviewList).toHaveBeenCalledWith({ page: 0, rows: 20, status: 'PENDING' })
      expect(aiApi.evolutionReviewStats).toHaveBeenCalled()
    })
    expect(await screen.findByText('建议补充竞品对比证据链')).toBeInTheDocument()
    expect(screen.getByText('已修订')).toBeInTheDocument()
    expect(screen.getByText('66.7%')).toBeInTheDocument()
    expect(screen.getByTestId('evolution-review-boundary-contract')).toHaveAttribute('data-no-local-review-fallback', 'true')
    expect(screen.getByTestId('evolution-review-summary-cards')).toHaveAttribute('data-no-static-stats-fallback', 'true')
    expect(screen.getByTestId('evolution-review-grid')).toHaveAttribute('data-pagination-mode', 'server')

    fireEvent.click(screen.getByRole('button', { name: '通过' }))
    fireEvent.change(screen.getByLabelText('审核备注（可选）'), {
      target: { value: '证据链合格' },
    })
    fireEvent.click(screen.getByRole('button', { name: '通过并入库' }))

    await waitFor(() => {
      expect(aiApi.evolutionReviewApprove).toHaveBeenCalledWith(21, '证据链合格')
    })
  })

  it('EvolutionReviewPage renders wrapped records from SDK-compatible payloads', async () => {
    vi.mocked(aiApi.evolutionReviewList).mockResolvedValue({
      payload: {
        reviewTasks: [
          {
            id: 22,
            evolveTaskId: 8,
            contentSummary: 'reviewTasks 包装审核任务',
            qualityScore: 9,
            status: 'PENDING',
            createTime: '2026-05-21 10:00:00',
          },
        ],
        totalElements: 1,
        page: 0,
        size: 20,
      },
    } as never)

    renderPage(<EvolutionReviewPage />)

    expect(await screen.findByText('reviewTasks 包装审核任务')).toBeInTheDocument()
  })

  it('EvolutionTasksPage triggers typed task and shows report failure inline', async () => {
    vi.mocked(aiApi.evolveTaskReport).mockRejectedValueOnce(new Error('report not found'))
    renderPage(<EvolutionTasksPage />)

    expect(screen.getByRole('heading', { name: '进化任务' })).toBeInTheDocument()
    expect(screen.getByTestId('evolution-tasks-page')).toHaveAttribute('data-no-local-task-insertion', 'true')
    expect(screen.getByTestId('evolution-tasks-boundary-contract')).toHaveAttribute('data-no-local-report-fallback', 'true')
    expect(screen.getByText(/\/ai\/evolution\/task\/list/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-tasks-refresh-list')).toHaveAttribute('data-refresh-scope', 'task-list-only')
    expect(screen.getByTestId('evolution-tasks-trigger-button')).toHaveAttribute('data-source-endpoint', '/ai/evolution/task/trigger')

    await waitFor(() => {
      expect(aiApi.evolveTaskList).toHaveBeenCalledWith({ rows: 50, page: 0, status: undefined })
    })
    expect(await screen.findByText(/gap/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-tasks-summary-cards')).toHaveAttribute('data-cancelable-count', '1')
    expect(screen.getByTestId('evolution-tasks-card')).toHaveAttribute('data-task-id', '31')
    expect(screen.getByTestId('evolution-tasks-card')).toHaveAttribute('data-task-status', '1')
    expect(screen.getByTestId('evolution-tasks-card')).toHaveAttribute('data-task-kb-id', '7')
    expect(screen.getByTestId('evolution-tasks-view-report')).toHaveAttribute('data-no-local-report-fallback', 'true')
    expect(screen.getByTestId('evolution-tasks-cancel-open')).toHaveAttribute('data-no-local-cancel-mutation', 'true')

    fireEvent.change(screen.getByLabelText('targetKbId（可选）'), { target: { value: '7' } })
    fireEvent.mouseDown(screen.getByRole('combobox', { name: '触发类型' }))
    fireEvent.click(screen.getByRole('option', { name: '深度进化' }))
    fireEvent.click(screen.getByRole('button', { name: '立即触发' }))

    await waitFor(() => {
      expect(aiApi.evolveTaskTrigger).toHaveBeenCalledWith({ taskType: 'deepen', targetKbId: 7 })
    })

    fireEvent.click(screen.getByRole('button', { name: '查看报告' }))
    expect(await screen.findByText(/加载报告失败（POST \/ai\/admin\/evolve\/report\/by-task）：report not found/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-tasks-operation-error')).toHaveAttribute('data-no-local-report-fallback', 'true')
    expect(screen.getByText(/任务卡片和筛选条件会保留/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    expect(await screen.findByRole('dialog', { name: '取消进化任务' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(aiApi.evolveTaskCancel).toHaveBeenCalledWith(31)
    })
  })

  it('EvolutionTasksPage accepts wrapped task list payloads', async () => {
    vi.mocked(aiApi.evolveTaskList).mockResolvedValue({
      data: {
        result: {
          tasks: [
            {
              id: 32,
              taskType: 'deepen',
              status: 2,
              progress: 100,
              kbId: 8,
            },
          ],
          total: 1,
        },
      },
      total: 1,
      pageNum: 0,
      pageSize: 50,
    } as never)

    renderPage(<EvolutionTasksPage />)

    expect(await screen.findByText(/KB 8/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-tasks-filter-contract')).toHaveAttribute('data-client-time-filter-only', 'true')
    expect(screen.getByTestId('evolution-tasks-summary-cards')).toHaveAttribute('data-filtered-count', '1')
    expect(screen.getAllByText((_, element) => element?.textContent?.includes('当前筛选 1 / 拉取 1') ?? false).length).toBeGreaterThan(0)
  })

  it('EvolutionTopicPage documents server-path import and saves topic with normalized priority', async () => {
    renderPage(<EvolutionTopicPage />)

    expect(screen.getByRole('heading', { name: '进化主题池' })).toBeInTheDocument()
    expect(screen.getByTestId('evolution-topic-page')).toHaveAttribute('data-no-browser-file-read', 'true')
    expect(screen.getByTestId('evolution-topic-boundary-contract')).toHaveAttribute('data-server-path-import', 'true')
    expect(screen.getByText(/服务端路径/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-topic-refresh-list')).toHaveAttribute('data-refresh-scope', 'topic-list-only')
    expect(screen.getByTestId('evolution-topic-import-open')).toHaveAttribute('data-no-browser-file-read', 'true')
    expect(screen.getByTestId('evolution-topic-create-open')).toHaveAttribute('data-no-local-topic-mutation', 'true')

    await waitFor(() => {
      expect(aiApi.topicList).toHaveBeenCalledWith({ scopeGlobal: true })
    })
    expect(await screen.findByText('屏障修护脚本证据链')).toBeInTheDocument()
    expect(screen.getByTestId('evolution-topic-summary-cards')).toHaveAttribute('data-filtered-count', '1')
    expect(screen.getByTestId('evolution-topic-summary-cards')).toHaveAttribute('data-category-count', '1')
    expect(screen.getByTestId('evolution-topic-card')).toHaveAttribute('data-topic-id', '41')
    expect(screen.getByTestId('evolution-topic-card')).toHaveAttribute('data-topic-category', 'script')
    expect(screen.getByTestId('evolution-topic-card')).toHaveAttribute('data-topic-priority', '1')
    expect(screen.getByTestId('evolution-topic-edit-open')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('evolution-topic-delete-open')).toHaveAttribute('data-no-local-topic-mutation', 'true')

    fireEvent.click(screen.getByRole('button', { name: '新增主题' }))
    expect(screen.getByTestId('evolution-topic-edit-dialog')).toHaveAttribute('data-no-local-topic-mutation', 'true')
    fireEvent.change(screen.getByLabelText('主题内容'), { target: { value: '直播转化证据链' } })
    fireEvent.change(screen.getByLabelText('分类标签'), { target: { value: 'live' } })
    fireEvent.click(screen.getByTestId('evolution-topic-save-submit'))

    await waitFor(() => {
      expect(aiApi.topicSave).toHaveBeenCalledWith(expect.objectContaining({
        topicName: '直播转化证据链',
        category: 'live',
        priority: 2,
      }))
    })
    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '新增主题' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '批量导入' }))
    expect(screen.getByTestId('evolution-topic-import-dialog')).toHaveAttribute('data-no-browser-file-read', 'true')
    expect(screen.getByTestId('evolution-topic-import-dialog')).toHaveAttribute('data-input-retained', 'true')
    fireEvent.change(screen.getByLabelText('服务端路径'), { target: { value: '/data/topics.json' } })
    fireEvent.click(screen.getByTestId('evolution-topic-import-submit'))
    await waitFor(() => {
      expect(aiApi.topicImport).toHaveBeenCalledWith('/data/topics.json')
    })
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '批量导入主题' })).not.toBeInTheDocument()
    })

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(await screen.findByRole('dialog', { name: '删除进化主题' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    await waitFor(() => {
      expect(aiApi.topicDelete).toHaveBeenCalledWith(41)
    })
  })

  it('EvolutionTopicPage accepts wrapped topic list and shows import failures inline', async () => {
    vi.mocked(aiApi.topicList).mockResolvedValue({
      data: {
        result: {
          topics: [
            {
              id: 42,
              topicName: '包装主题池主题',
              category: 'viral',
              priority: 3,
            },
          ],
        },
      },
    } as never)
    vi.mocked(aiApi.topicImport).mockRejectedValueOnce(new Error('file not found'))

    renderPage(<EvolutionTopicPage />)

    expect(await screen.findByText('包装主题池主题')).toBeInTheDocument()
    expect(screen.getByTestId('evolution-topic-card')).toHaveAttribute('data-topic-priority', '3')

    fireEvent.click(screen.getByRole('button', { name: '批量导入' }))
    fireEvent.change(screen.getByLabelText('服务端路径'), { target: { value: '/data/missing.json' } })
    fireEvent.click(screen.getByTestId('evolution-topic-import-submit'))

    expect(await screen.findByText(/导入失败（POST \/ai\/evolution\/topic\/import）：file not found/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-topic-operation-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/导入路径会保留/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('/data/missing.json')).toBeInTheDocument()
  })

  it('EvolutionReviewPage keeps approve dialog and comment when approve endpoint fails', async () => {
    vi.mocked(aiApi.evolutionReviewApprove).mockRejectedValueOnce(new Error('approve denied'))
    renderPage(<EvolutionReviewPage />)

    expect(await screen.findByText('建议补充竞品对比证据链')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '通过' }))
    fireEvent.change(screen.getByLabelText('审核备注（可选）'), {
      target: { value: '证据链合格' },
    })
    fireEvent.click(screen.getByRole('button', { name: '通过并入库' }))

    expect(await screen.findByText(/通过失败（POST \/ai\/evolution-review\/approve）：approve denied/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-review-approve-error')).toHaveAttribute('data-no-local-review-mutation', 'true')
    expect(screen.getByTestId('evolution-review-approve-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('evolution-review-action-dialog')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByRole('dialog', { name: '确认通过并入库' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('证据链合格')).toBeInTheDocument()
    expect(screen.getAllByText('建议补充竞品对比证据链').length).toBeGreaterThan(0)
  })

  it('EvolutionReviewPage keeps reject dialog and reason when reject endpoint fails', async () => {
    vi.mocked(aiApi.evolutionReviewReject).mockRejectedValueOnce(new Error('reject denied'))
    renderPage(<EvolutionReviewPage />)

    expect(await screen.findByText('建议补充竞品对比证据链')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '拒绝' }))
    fireEvent.change(screen.getByRole('textbox', { name: '拒绝原因' }), {
      target: { value: '缺少引用证据' },
    })
    fireEvent.click(screen.getByRole('button', { name: '确认拒绝' }))

    expect(await screen.findByText(/拒绝失败（POST \/ai\/evolution-review\/reject）：reject denied/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-review-reject-error')).toHaveAttribute('data-no-local-review-mutation', 'true')
    expect(screen.getByTestId('evolution-review-reject-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByRole('dialog', { name: '拒绝原因' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('缺少引用证据')).toBeInTheDocument()
    await waitFor(() => {
      expect(aiApi.evolutionReviewReject).toHaveBeenCalledWith(21, '缺少引用证据')
    })
  })

  it('EvolutionReviewPage uses a theme-aware review summary surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme(<EvolutionReviewPage />)

    expect(await screen.findByText('建议补充竞品对比证据链')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '通过' }))

    expect(await screen.findByRole('dialog', { name: '确认通过并入库' })).toBeInTheDocument()
    expect(screen.getByTestId('evolution-review-summary-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('EvolutionTopicPage keeps edit dialog and values when save endpoint fails', async () => {
    vi.mocked(aiApi.topicSave).mockRejectedValueOnce(new Error('save denied'))
    renderPage(<EvolutionTopicPage />)

    await screen.findByText('屏障修护脚本证据链')
    fireEvent.click(screen.getByRole('button', { name: '新增主题' }))
    fireEvent.change(screen.getByLabelText('主题内容'), { target: { value: '直播转化证据链' } })
    fireEvent.change(screen.getByLabelText('分类标签'), { target: { value: 'live' } })
    fireEvent.click(screen.getByTestId('evolution-topic-save-submit'))

    expect(await screen.findByText(/保存失败（POST \/ai\/evolution\/topic\/save）：save denied/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-topic-operation-error')).toHaveAttribute('data-no-local-topic-mutation', 'true')
    expect(screen.getByRole('dialog', { name: '新增主题' })).toBeInTheDocument()
    expect(screen.getByDisplayValue('直播转化证据链')).toBeInTheDocument()
    expect(screen.getByDisplayValue('live')).toBeInTheDocument()
  })

  it('EvolutionTopicPage keeps topic card when delete endpoint fails', async () => {
    vi.mocked(aiApi.topicDelete).mockRejectedValueOnce(new Error('delete denied'))
    renderPage(<EvolutionTopicPage />)

    expect(await screen.findByText('屏障修护脚本证据链')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    expect(await screen.findByRole('dialog', { name: '删除进化主题' })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findByText(/删除失败（POST \/ai\/evolution\/topic\/delete）：delete denied/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-topic-operation-error')).toHaveAttribute('data-no-local-topic-mutation', 'true')
    expect(screen.getByText('屏障修护脚本证据链')).toBeInTheDocument()
  })

  it('IndustryBrainDiagnosisPage displays disabled backend error and validates account id', async () => {
    vi.mocked(brainApi.contentDiagnosis).mockRejectedValueOnce(new Error('行业大脑未启用'))
    renderPage(<IndustryBrainDiagnosisPage />)

    expect(screen.getByRole('heading', { name: '行业大脑 · 诊断中心' })).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-diagnosis-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/ai/brain/content-diagnosis,/ai/brain/product-diagnosis,/ai/brain/rhythm-diagnosis,/ai/brain/account/diagnose',
    )
    expect(screen.getByTestId('industry-brain-diagnosis-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      '/ai/brain/mock-diagnosis,/ai/brain/local-history,/ai/brain/static-diagnosis,/ai/brain/account/local-score',
    )
    expect(screen.getByTestId('industry-brain-diagnosis-boundary-contract')).toHaveAttribute('data-session-history-only', 'true')
    expect(screen.getAllByText(/\/ai\/brain\/content-diagnosis/).length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '开始内容诊断' }))
    expect(await screen.findByText(/诊断失败（POST \/ai\/brain\/content-diagnosis）：行业大脑未启用。当前诊断类型和参数会保留。/)).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-diagnosis-page-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('industry-brain-diagnosis-page-error')).toHaveAttribute('data-no-static-diagnosis-fallback', 'true')

    fireEvent.click(screen.getByRole('tab', { name: /账号诊断/ }))
    expect(screen.getByRole('button', { name: '开始账号诊断' })).toBeDisabled()
    expect(screen.getByTestId('industry-brain-account-validation')).toHaveAttribute('data-no-local-account-score-fallback', 'true')
    expect(screen.getAllByText(/douyin_account.id/).length).toBeGreaterThan(0)

    fireEvent.change(screen.getByLabelText('抖音账号 ID（douyin_account.id）'), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: '开始账号诊断' }))
    await waitFor(() => {
      expect(brainApi.accountDiagnose).toHaveBeenCalledWith(5)
    })
    expect((await screen.findAllByText('账号诊断摘要')).length).toBeGreaterThan(0)
    expect(screen.getAllByTestId('industry-brain-account-diagnosis-surface')[0]).toHaveAttribute('data-no-local-account-score-fallback', 'true')
    expect(screen.getByTestId('industry-brain-session-history')).toHaveAttribute('data-session-history-only', 'true')
  })

  it('IndustryBrainDiagnosisPage shows endpoint and structured LLM fields', async () => {
    vi.mocked(brainApi.contentDiagnosis).mockResolvedValue({
      diagnosisType: 'content',
      status: 'success',
      analysis: '内容诊断结果',
      strengths: ['转化证据清晰'],
      actions: ['补充竞品对比'],
      tokensUsed: 100,
    } as never)
    renderPage(<IndustryBrainDiagnosisPage />)

    expect(screen.getByText(/当前接口：/)).toBeInTheDocument()
    expect(screen.getAllByText('/ai/brain/content-diagnosis').length).toBeGreaterThan(0)

    fireEvent.click(screen.getByRole('button', { name: '开始内容诊断' }))

    expect(await screen.findByText('内容诊断结果')).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-diagnosis-params-panel')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByTestId('industry-brain-diagnosis-tabs')).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-diagnosis-result-panel')).toHaveAttribute('data-no-static-diagnosis-fallback', 'true')
    expect(screen.getAllByTestId('industry-brain-llm-result-surface')[0]).toHaveAttribute('data-no-static-diagnosis-fallback', 'true')
    expect(screen.getAllByText('结构化诊断字段').length).toBeGreaterThan(0)
    expect(screen.getAllByText('strengths').length).toBeGreaterThan(0)
    expect(screen.getAllByText(/转化证据清晰/).length).toBeGreaterThan(0)
    expect(screen.getAllByText('/ai/brain/content-diagnosis').length).toBeGreaterThan(1)
  })

  it('IndustryBrainDiagnosisPage uses theme-aware diagnosis surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme(<IndustryBrainDiagnosisPage />)

    expect(screen.getByTestId('industry-brain-diagnosis-empty-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })

    fireEvent.click(screen.getByRole('button', { name: '开始内容诊断' }))

    expect(await screen.findByText('内容诊断结果')).toBeInTheDocument()
    screen.getAllByTestId('industry-brain-llm-diagnosis-surface').forEach((surface) => {
      expect(surface).toHaveStyle({
        backgroundColor: 'rgb(18, 18, 18)',
      })
    })
  })

  it('IndustryBrainDiagnosisPage uses theme tokens for diagnosis tabs and account score in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme(<IndustryBrainDiagnosisPage />)

    const legacyColors = new Set([
      'rgb(25, 118, 210)',
      'rgb(56, 142, 60)',
      'rgb(245, 124, 0)',
      'rgb(123, 31, 162)',
      'rgb(76, 175, 80)',
      'rgb(255, 152, 0)',
      'rgb(244, 67, 54)',
    ])

    const activeTab = screen.getAllByTestId('industry-brain-diagnosis-tab-surface')[0]
    expect(legacyColors.has(window.getComputedStyle(activeTab).borderLeftColor)).toBe(false)

    fireEvent.click(screen.getByRole('tab', { name: /账号诊断/ }))
    fireEvent.change(screen.getByLabelText('抖音账号 ID（douyin_account.id）'), { target: { value: '5' } })
    fireEvent.click(screen.getByRole('button', { name: '开始账号诊断' }))

    expect((await screen.findAllByText('账号诊断摘要')).length).toBeGreaterThan(0)
    const accountTab = screen.getAllByTestId('industry-brain-diagnosis-tab-surface')[3]
    const scoreValue = screen.getAllByTestId('industry-brain-score-value-surface')[0]
    const scoreGauge = screen.getAllByTestId('industry-brain-score-gauge-surface')[0]

    expect(legacyColors.has(window.getComputedStyle(accountTab).borderLeftColor)).toBe(false)
    expect(legacyColors.has(window.getComputedStyle(scoreValue).color)).toBe(false)
    expect(window.getComputedStyle(scoreGauge).backgroundColor).not.toBe('rgba(255, 152, 0, 0.03)')
  })

  it('EvolutionReviewPage exposes retryable list and stats degradation without local rows', async () => {
    vi.mocked(aiApi.evolutionReviewList).mockRejectedValueOnce(new Error('review list offline'))
    vi.mocked(aiApi.evolutionReviewStats).mockRejectedValueOnce(new Error('stats offline'))
    renderPage(<EvolutionReviewPage />)

    expect(await screen.findByText(/审核任务加载失败：review list offline/)).toBeInTheDocument()
    expect(screen.getByTestId('evolution-review-list-error')).toHaveAttribute('data-no-local-review-fallback', 'true')
    expect(screen.getByTestId('evolution-review-stats-warning')).toHaveAttribute('data-no-static-stats-fallback', 'true')
    expect(screen.queryByText('建议补充竞品对比证据链')).not.toBeInTheDocument()
  })
})
