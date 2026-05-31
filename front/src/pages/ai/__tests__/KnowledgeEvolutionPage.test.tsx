import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import userEvent from '@testing-library/user-event'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import KnowledgeEvolutionPage from '../KnowledgeEvolutionPage'
import { aiApi } from '@/api/ai'

vi.mock('@/api/ai', () => ({
  aiApi: {
    kbList: vi.fn(),
    evolveTaskList: vi.fn(),
    evolveRoi: vi.fn(),
    scoreTrend: vi.fn(),
    evolveTaskTrigger: vi.fn(),
    knowledgeEvolutionAnalyze: vi.fn(),
    knowledgeEvolutionAutoOptimize: vi.fn(),
    knowledgeEvolutionReport: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('KnowledgeEvolutionPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(aiApi.kbList).mockResolvedValue([
      { id: 7, kbName: '范围测试库', description: '', totalDocuments: 0, status: 1, createTime: '' },
    ] as never)
    vi.mocked(aiApi.evolveTaskList).mockResolvedValue({ list: [], total: 0 } as never)
    vi.mocked(aiApi.evolveRoi).mockResolvedValue({
      newKnowledge: 1,
      avgScore: 80,
      totalRuns: 2,
      coveredDocs: 1,
    } as never)
    vi.mocked(aiApi.scoreTrend).mockResolvedValue([] as never)
    vi.mocked(aiApi.knowledgeEvolutionAnalyze).mockResolvedValue({
      analysisId: 'evol_test',
      degraded: true,
      readyForInclusion: [],
      needsOptimization: [],
      duplicatesDetected: [],
      readyForArchival: [],
    } as never)
    vi.mocked(aiApi.knowledgeEvolutionAutoOptimize).mockResolvedValue({ status: 'COMPLETED' } as never)
    vi.mocked(aiApi.knowledgeEvolutionReport).mockResolvedValue({
      reportId: 'report_test',
      period: '2026-05-15 ~ 2026-05-22',
      generatedAt: '2026-05-22',
      overview: {
        totalScriptsInLibrary: 4,
        newAddedCount: 1,
        archivedCount: 0,
        deduplicatedCount: 1,
        averageScore: 82,
      },
      recommendations: [
        { type: 'DEDUP_OPPORTUNITY', priority: 'MEDIUM', description: '1 组重复候选待处理' },
      ],
    } as never)
  })

  it('renders dashboard title and loads data for all KBs by default', async () => {
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('进化监控看板')).toBeInTheDocument()
    expect(screen.getByTestId('knowledge-evolution-workbench')).toHaveAttribute('data-no-local-roi-fallback', 'true')
    expect(screen.getByTestId('knowledge-evolution-workbench')).toHaveAttribute('data-no-client-score-synthesis', 'true')
    expect(screen.getByTestId('knowledge-evolution-analysis-section')).toHaveAttribute('data-no-template-writeback', 'true')

    await waitFor(() => {
      expect(aiApi.kbList).toHaveBeenCalledWith({ page: 0, rows: 200 })
      expect(aiApi.evolveTaskList).toHaveBeenCalledWith({
        page: 0,
        rows: 20,
        kbId: undefined,
        taskType: undefined,
        status: undefined,
      })
      expect(aiApi.evolveRoi).toHaveBeenCalledWith({})
      expect(aiApi.scoreTrend).toHaveBeenCalledWith({ days: 7, kbId: undefined })
    })
  })

  it('refetches with kbId when knowledge base scope changes', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByLabelText('知识库范围')).toBeInTheDocument()
    })

    await user.click(screen.getByLabelText('知识库范围'))
    await user.click(await screen.findByRole('option', { name: '范围测试库' }))

    await waitFor(() => {
      expect(aiApi.evolveTaskList).toHaveBeenCalledWith(expect.objectContaining({ page: 0, rows: 20, kbId: 7 }))
      expect(aiApi.evolveRoi).toHaveBeenCalledWith({ kbId: 7 })
      expect(aiApi.scoreTrend).toHaveBeenCalledWith({ days: 7, kbId: 7 })
    })
  })

  it('requests score trend with selected day window', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getAllByLabelText('趋势天数').length).toBeGreaterThan(0)
    })

    await user.click(screen.getAllByLabelText('趋势天数')[0])
    await user.click(await screen.findByRole('option', { name: '14 天' }))

    await waitFor(() => {
      expect(aiApi.scoreTrend).toHaveBeenCalledWith(expect.objectContaining({ days: 14, kbId: undefined }))
    })
  })

  it('filters task list by agent type', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByLabelText('Agent 类型')).toBeInTheDocument()
    })

    vi.mocked(aiApi.evolveTaskList).mockClear()

    await user.click(screen.getByLabelText('Agent 类型'))
    await user.click(await screen.findByRole('option', { name: '质量评分Agent' }))

    await waitFor(() => {
      expect(aiApi.evolveTaskList).toHaveBeenCalledWith(expect.objectContaining({
        page: 0,
        taskType: 'quality',
      }))
    })
  })

  it('shows auto optimization execution result after analysis', async () => {
    const user = userEvent.setup()
    vi.mocked(aiApi.knowledgeEvolutionAnalyze).mockResolvedValue({
      analysisId: 'evol_ready',
      degraded: false,
      periodStart: '2026-05-15',
      periodEnd: '2026-05-22',
      readyForInclusion: [{ scriptVersionId: 101, title: '高转化开场', score: 91, reason: '高分' }],
      needsOptimization: [],
      duplicatesDetected: [],
      readyForArchival: [],
      expectedImpact: { newInclusionsCount: 1, deduplicationCount: 0, improvementRate: 25 },
    } as never)
    vi.mocked(aiApi.knowledgeEvolutionAutoOptimize).mockResolvedValue({
      executionId: 'exec_ready',
      status: 'COMPLETED',
      results: {
        included: { count: 1, scriptIds: [101] },
        merged: { count: 0 },
        archived: { count: 0, scriptIds: [] },
      },
      summary: {
        totalProcessed: 1,
        estimatedUserBenefit: '纳入 1 个脚本',
      },
      executedAt: '2026-05-22',
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await user.click(await screen.findByRole('button', { name: '运行分析' }))
    await waitFor(() => {
      expect(screen.getByText('高转化开场')).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '应用优化' }))

    await waitFor(() => {
      expect(aiApi.knowledgeEvolutionAutoOptimize).toHaveBeenCalledWith({
        analysisId: 'evol_ready',
        actions: { autoInclude: true, autoMerge: true, autoArchive: false },
        approvalRequired: false,
      })
      expect(screen.getByText(/优化执行结果：COMPLETED/)).toBeInTheDocument()
      expect(screen.getByText(/处理 1 项：入库 1 · 合并 0 · 归档 0/)).toBeInTheDocument()
    })
  })

  it('keeps the page in explicit degraded mode without local opportunities', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await user.click(await screen.findByRole('button', { name: '运行分析' }))

    await waitFor(() => {
      expect(aiApi.knowledgeEvolutionAnalyze).toHaveBeenCalledWith({ analysisScope: 'LAST_7_DAYS' })
      expect(screen.getByTestId('knowledge-evolution-degraded-analysis')).toHaveAttribute('data-no-local-analysis-fallback', 'true')
      expect(screen.getByTestId('knowledge-evolution-degraded-analysis')).toHaveAttribute('data-optimize-disabled', 'true')
      expect(screen.getByRole('button', { name: '应用优化' })).toBeDisabled()
    })
  })

  it('generates and displays knowledge evolution report', async () => {
    const user = userEvent.setup()
    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await user.click(await screen.findByRole('button', { name: '运行分析' }))
    await user.click(await screen.findByRole('button', { name: '生成进化报告' }))

    await waitFor(() => {
      expect(aiApi.knowledgeEvolutionReport).toHaveBeenCalledWith({
        reportType: 'WEEKLY',
        includeTopScripts: true,
        includeStyleAnalysis: true,
      })
      expect(screen.getByText('最新报告：report_test')).toBeInTheDocument()
      expect(screen.getByText(/脚本 4/)).toBeInTheDocument()
      expect(screen.getByText(/MEDIUM · 1 组重复候选待处理/)).toBeInTheDocument()
    })
  })

  it('shows endpoint-specific load errors and inline mutation failures', async () => {
    const user = userEvent.setup()
    vi.mocked(aiApi.kbList).mockRejectedValueOnce(new Error('kb list timeout') as never)
    vi.mocked(aiApi.evolveTaskList).mockRejectedValueOnce(new Error('task list timeout') as never)
    vi.mocked(aiApi.evolveRoi).mockRejectedValueOnce(new Error('roi timeout') as never)
    vi.mocked(aiApi.scoreTrend).mockRejectedValueOnce(new Error('trend timeout') as never)
    vi.mocked(aiApi.knowledgeEvolutionAnalyze).mockRejectedValueOnce(new Error('analyze timeout') as never)
    vi.mocked(aiApi.knowledgeEvolutionReport).mockRejectedValueOnce(new Error('report timeout') as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(screen.getByText(/知识库列表加载失败（\/ai\/knowledge-base\/list）：kb list timeout/)).toBeInTheDocument()
      expect(screen.getByText(/进化任务列表加载失败（\/ai\/evolution\/task\/list）：task list timeout/)).toBeInTheDocument()
      expect(screen.getByText(/ROI 指标加载失败（\/ai\/evolution\/roi）：roi timeout/)).toBeInTheDocument()
      expect(screen.getByText(/质量趋势加载失败（\/ai\/evolution\/score-trend）：trend timeout/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('knowledge-evolution-load-error')).toHaveAttribute('data-no-local-roi-fallback', 'true')
    expect(screen.getByTestId('knowledge-evolution-load-error')).toHaveAttribute('data-no-local-task-fallback', 'true')

    await user.click(screen.getByRole('button', { name: '运行分析' }))
    await waitFor(() => {
      expect(screen.getByText(/分析失败（\/ai\/knowledge-evolution\/analyze）：analyze timeout/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('knowledge-evolution-analysis-error')).toHaveAttribute('data-input-retained', 'true')

    vi.mocked(aiApi.knowledgeEvolutionAnalyze).mockResolvedValueOnce({
      analysisId: 'evol_after_error',
      degraded: false,
      readyForInclusion: [],
      needsOptimization: [],
      duplicatesDetected: [],
      readyForArchival: [],
    } as never)

    await user.click(screen.getByRole('button', { name: '运行分析' }))
    await waitFor(() => {
      expect(screen.getByRole('button', { name: '生成进化报告' })).toBeInTheDocument()
    })

    await user.click(screen.getByRole('button', { name: '生成进化报告' }))
    await waitFor(() => {
      expect(screen.getByText(/报告生成失败（\/ai\/knowledge-evolution\/report）：report timeout/)).toBeInTheDocument()
    })
    expect(screen.getByTestId('knowledge-evolution-report-error')).toHaveAttribute('data-no-local-report-fallback', 'true')
  })

  it('shows trigger failure without inserting a local task row', async () => {
    const user = userEvent.setup()
    vi.mocked(aiApi.evolveTaskTrigger).mockRejectedValue(new Error('trigger timeout') as never)

    renderWithProviders(
      <MemoryRouter>
        <KnowledgeEvolutionPage />
      </MemoryRouter>,
    )

    await user.click(await screen.findByRole('button', { name: '触发进化' }))

    await waitFor(() => {
      expect(aiApi.evolveTaskTrigger).toHaveBeenCalledWith({
        taskType: 'gap',
        targetKbId: undefined,
      })
      expect(screen.getByTestId('knowledge-evolution-trigger-error')).toHaveAttribute('data-no-local-task-insertion', 'true')
      expect(screen.getByText(/进化任务触发失败（\/ai\/evolution\/task\/trigger）：trigger timeout/)).toBeInTheDocument()
    })
  })
})
