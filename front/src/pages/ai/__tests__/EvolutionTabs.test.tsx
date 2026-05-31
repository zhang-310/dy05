import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { cleanup, fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import { TaskQueueTab } from '../evolution/tabs/TaskQueueTab'
import { TopicsTab } from '../evolution/tabs/TopicsTab'
import { RoiTab } from '../evolution/tabs/RoiTab'
import { QualityHeatmapTab } from '../evolution/tabs/QualityHeatmapTab'
import { EvolutionMapTab } from '../evolution/tabs/EvolutionMapTab'
import { ReviewTab } from '../evolution/tabs/ReviewTab'
import { aiApi } from '@/api/ai'

vi.mock('echarts-for-react', () => ({
  default: ({ className, option }: { className?: string; option?: unknown }) => (
    <div className={className} data-testid="echarts">{JSON.stringify(option)}</div>
  ),
}))

vi.mock('@/api/ai', () => ({
  aiApi: {
    evolveTaskList: vi.fn(),
    evolveTaskTrigger: vi.fn(),
    evolveTaskCancel: vi.fn(),
    evolveTaskReport: vi.fn(),
    topicList: vi.fn(),
    topicSave: vi.fn(),
    topicDelete: vi.fn(),
    pendingDeepenTrigger: vi.fn(),
    evolveRoi: vi.fn(),
    scoreTrend: vi.fn(),
    qualityScoreHistory: vi.fn(),
    evolutionReviewList: vi.fn(),
    evolutionReviewStats: vi.fn(),
    evolutionReviewApprove: vi.fn(),
    evolutionReviewReject: vi.fn(),
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

function renderTab(ui: React.ReactElement) {
  return renderWithProviders(<MemoryRouter>{ui}</MemoryRouter>)
}

function renderTabWithTheme(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>{ui}</MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('Evolution governance tabs', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    vi.mocked(aiApi.evolveTaskList).mockResolvedValue({
      total: 1,
      list: [{
        id: 31,
        taskType: 'deepen',
        status: 1,
        progress: 35,
        kbId: 7,
        scoreTotal: 8,
        topicTexts: '主题摘要',
        scoreDetail: '{"quality":8,"evidence":"引用完整"}',
        createTime: '2026-05-22 10:00:00',
      }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.evolveTaskTrigger).mockResolvedValue({ taskId: 'job-1' } as never)
    vi.mocked(aiApi.evolveTaskCancel).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.evolveTaskReport).mockResolvedValue({ fullContent: '后台报告' } as never)
    vi.mocked(aiApi.topicList).mockResolvedValue([
      { id: 41, kbId: 7, topicName: '屏障修护脚本证据链', category: 'script', priority: 1, status: 1 },
    ] as never)
    vi.mocked(aiApi.topicSave).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.topicDelete).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.evolveRoi).mockResolvedValue({
      newKnowledge: 3,
      avgScore: 8.6,
      successRate: 80,
      coveredDocs: 2,
      totalTasks: 5,
      totalRuns: 4,
    } as never)
    vi.mocked(aiApi.scoreTrend).mockResolvedValue([{ date: '2026-05-22', score: 8, count: 2 }] as never)
    vi.mocked(aiApi.qualityScoreHistory).mockResolvedValue([{ date: '2026-05-22', score: 8, count: 2 }] as never)
    vi.mocked(aiApi.evolutionReviewList).mockResolvedValue({
      total: 1,
      list: [{
        id: 51,
        evolveTaskId: 31,
        contentPreview: '审核内容摘要',
        reviewStatus: 'PENDING',
        qualityScore: 7,
        createTime: '2026-05-22 10:30:00',
      }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(aiApi.evolutionReviewStats).mockResolvedValue({
      pendingCount: 1,
      approvedCount: 0,
      rejectedCount: 0,
    } as never)
    vi.mocked(aiApi.evolutionReviewApprove).mockResolvedValue(undefined as never)
    vi.mocked(aiApi.evolutionReviewReject).mockResolvedValue(undefined as never)
  })

  it('declares real endpoint contracts for each evolution tab without local fallback actions', async () => {
    renderTab(<TaskQueueTab scopeKbId="7" />)
    const taskQueue = screen.getByTestId('evolution-task-queue-tab-contract')
    expect(taskQueue).toHaveAttribute('data-contract-scope', 'ai-evolution-task-queue')
    expect(taskQueue).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/task/list'))
    expect(taskQueue).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/task/trigger'))
    expect(taskQueue).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/task/cancel'))
    expect(taskQueue).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/admin/evolve/report/by-task'))
    expect(taskQueue).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-task-insertion'))
    expect(taskQueue).toHaveAttribute('data-no-local-task-fallback', 'true')
    expect(taskQueue).toHaveAttribute('data-no-static-report-fallback', 'true')
    cleanup()

    renderTab(<TopicsTab scopeKbId="7" />)
    const topics = screen.getByTestId('evolution-topics-tab-contract')
    expect(topics).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/topic/list'))
    expect(topics).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/topic/save'))
    expect(topics).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/topic/delete'))
    expect(topics).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution/task/trigger'))
    expect(topics).toHaveAttribute('data-unsupported-actions', expect.stringContaining('/ai/evolution/pending-deepen/trigger'))
    expect(topics).toHaveAttribute('data-no-pending-deepen-trigger', 'true')
    expect(topics).toHaveAttribute('data-no-local-topic-fallback', 'true')
    cleanup()

    renderTab(<RoiTab scopeKbId="7" />)
    const roi = screen.getByTestId('evolution-roi-tab-contract')
    expect(roi).toHaveAttribute('data-ready-endpoints', '/ai/evolution/roi|/ai/evolution/score-trend')
    expect(roi).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-roi-aggregation'))
    expect(roi).toHaveAttribute('data-no-static-trend-fallback', 'true')
    cleanup()

    renderTab(<QualityHeatmapTab scopeKbId="7" />)
    const quality = screen.getByTestId('evolution-quality-heatmap-tab-contract')
    expect(quality).toHaveAttribute('data-ready-endpoints', '/ai/evolution/quality-score/history')
    expect(quality).toHaveAttribute('data-unsupported-actions', expect.stringContaining('static-heatmap'))
    expect(quality).toHaveAttribute('data-no-local-trend-fallback', 'true')
    cleanup()

    renderTab(<EvolutionMapTab scopeKbId="7" />)
    const map = screen.getByTestId('evolution-map-tab-contract')
    expect(map).toHaveAttribute('data-ready-endpoints', '/ai/evolution/topic/list')
    expect(map).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-dependency-graph'))
    expect(map).toHaveAttribute('data-no-static-topology-fallback', 'true')
    cleanup()

    renderTab(<ReviewTab />)
    const review = screen.getByTestId('evolution-review-tab-contract')
    expect(review).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution-review/list'))
    expect(review).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution-review/stats'))
    expect(review).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution-review/approve'))
    expect(review).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/evolution-review/reject'))
    expect(review).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-approval-mutation'))
    expect(review).toHaveAttribute('data-no-local-review-fallback', 'true')
    expect(review).toHaveAttribute('data-no-static-review-stats', 'true')
  })

  it('TopicsTab runs a topic through evolveTaskTrigger instead of pendingDeepenTrigger', async () => {
    renderTab(<TopicsTab scopeKbId="7" />)

    expect(await screen.findByText('屏障修护脚本证据链')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '运行' }))

    await waitFor(() => {
      expect(aiApi.evolveTaskTrigger).toHaveBeenCalledWith({
        taskType: 'deepen',
        targetKbId: 7,
        targetId: 41,
      })
    })
    expect(aiApi.pendingDeepenTrigger).not.toHaveBeenCalled()
  })

  it('TopicsTab keeps topic row when run endpoint fails', async () => {
    vi.mocked(aiApi.evolveTaskTrigger).mockRejectedValueOnce(new Error('queue denied'))
    renderTab(<TopicsTab scopeKbId="7" />)

    expect(await screen.findByText('屏障修护脚本证据链')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '运行' }))

    expect(await screen.findByText(/运行主题失败（POST \/ai\/evolution\/task\/trigger）：queue denied/)).toBeInTheDocument()
    expect(screen.getByText(/主题行会保留/)).toBeInTheDocument()
    expect(screen.getByText('屏障修护脚本证据链')).toBeInTheDocument()
  })

  it('TopicsTab uses theme-aware create and save action text in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderTabWithTheme(<TopicsTab scopeKbId="7" />)

    expect(await screen.findByText('屏障修护脚本证据链')).toBeInTheDocument()
    const createAction = screen.getByTestId('evolution-topic-create-action-surface')
    expect(createAction).not.toHaveStyle({ color: 'rgb(0, 0, 0)' })

    fireEvent.click(createAction)
    const saveAction = await screen.findByTestId('evolution-topic-save-action-surface')
    expect(saveAction).not.toHaveStyle({ color: 'rgb(0, 0, 0)' })
  })

  it('TaskQueueTab shows trigger, report and cancel endpoints without losing context', async () => {
    vi.mocked(aiApi.evolveTaskTrigger).mockRejectedValueOnce(new Error('trigger denied'))
    vi.mocked(aiApi.evolveTaskReport).mockRejectedValueOnce(new Error('report missing'))
    vi.mocked(aiApi.evolveTaskCancel).mockRejectedValueOnce(new Error('cancel denied'))
    renderTab(<TaskQueueTab scopeKbId="7" />)

    expect(await screen.findByText(/深度进化Agent/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '触发 深度进化Agent' }))
    expect(await screen.findByText(/触发失败（POST \/ai\/evolution\/task\/trigger）：trigger denied/)).toBeInTheDocument()
    expect(screen.getByText(/当前 Agent 类型和知识库范围会保留/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '详情' }))
    expect(await screen.findByRole('dialog', { name: /任务详情 #31/ })).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '拉取后台报告' }))
    expect(await screen.findByText(/报告加载失败（POST \/ai\/admin\/evolve\/report\/by-task）：report missing/)).toBeInTheDocument()
    expect(screen.getByText(/详情抽屉保留当前任务上下文/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '取消' }))
    expect(await screen.findByText(/取消失败（POST \/ai\/evolution\/task\/cancel）：cancel denied/)).toBeInTheDocument()
    expect(screen.getByText(/任务行会保留/)).toBeInTheDocument()
  })

  it('TaskQueueTab uses theme-aware detail and report preview surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderTabWithTheme(<TaskQueueTab scopeKbId="7" />)

    expect(await screen.findByText('35%')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '详情' }))

    const scoreSurface = await screen.findByTestId('evolution-task-score-detail-surface')
    expect(scoreSurface).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })

    fireEvent.click(screen.getByRole('button', { name: '拉取后台报告' }))
    expect(await screen.findByText(/后台报告/)).toBeInTheDocument()
    const reportSurface = await screen.findByTestId('evolution-task-report-surface')
    expect(reportSurface).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('TaskQueueTab uses theme-aware trigger hover surfaces in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderTabWithTheme(<TaskQueueTab scopeKbId="7" />)

    expect(await screen.findByText('35%')).toBeInTheDocument()
    const triggerActions = screen.getAllByTestId('evolution-task-trigger-action-surface')
    expect(triggerActions.length).toBeGreaterThan(0)
    triggerActions.forEach(action => {
      expect(action).not.toHaveAttribute('data-hover-bg', 'rgba(0, 208, 132, 0.04)')
    })
  })

  it('ReviewTab keeps reject reason and row when reject endpoint fails', async () => {
    vi.mocked(aiApi.evolutionReviewReject).mockRejectedValueOnce(new Error('reject denied'))
    renderTab(<ReviewTab />)

    expect(await screen.findByText('审核内容摘要')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '拒绝' }))
    fireEvent.change(screen.getByPlaceholderText('请说明拒绝原因，将写入审核记录与任务备注'), {
      target: { value: '缺少引用证据' },
    })
    fireEvent.click(screen.getByRole('button', { name: '确认拒绝' }))

    expect(await screen.findByText(/审核拒绝失败（POST \/ai\/evolution-review\/reject）：reject denied/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('缺少引用证据')).toBeInTheDocument()
    expect(screen.getByText('审核内容摘要')).toBeInTheDocument()
    await waitFor(() => {
      expect(aiApi.evolutionReviewReject).toHaveBeenCalledWith(51, '缺少引用证据')
    })
  })

  it('ReviewTab uses theme-aware stat colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderTabWithTheme(<ReviewTab />)

    expect(await screen.findByText('待审核')).toBeInTheDocument()
    const oldFixedColors = new Set([
      'rgb(245, 124, 0)',
      'rgb(56, 142, 60)',
      'rgb(211, 47, 47)',
    ])

    const statValues = await screen.findAllByTestId('evolution-review-stat-value-surface')
    expect(statValues).toHaveLength(3)
    statValues.forEach(value => {
      expect(oldFixedColors.has(window.getComputedStyle(value).color)).toBe(false)
    })
  })

  it('RoiTab displays independent ROI and trend endpoint failures', async () => {
    vi.mocked(aiApi.evolveRoi).mockRejectedValueOnce(new Error('roi offline'))
    vi.mocked(aiApi.scoreTrend).mockRejectedValueOnce(new Error('trend offline'))

    renderTab(<RoiTab scopeKbId="7" />)

    expect(await screen.findByText(/ROI 指标加载失败（POST \/ai\/evolution\/roi）：roi offline/)).toBeInTheDocument()
    expect(await screen.findByText(/质量分趋势加载失败（POST \/ai\/evolution\/score-trend）：trend offline/)).toBeInTheDocument()
  })

  it('RoiTab uses theme-aware KPI value colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderTabWithTheme(<RoiTab scopeKbId="7" />)

    expect(await screen.findByText('3 次')).toBeInTheDocument()
    const oldFixedColors = new Set([
      'rgb(25, 118, 210)',
      'rgb(56, 142, 60)',
      'rgb(245, 124, 0)',
      'rgb(123, 31, 162)',
    ])

    const kpiValues = screen.getAllByTestId('evolution-roi-kpi-value-surface')
    expect(kpiValues).toHaveLength(4)
    kpiValues.forEach(value => {
      expect(oldFixedColors.has(window.getComputedStyle(value).color)).toBe(false)
    })
  })

  it('QualityHeatmapTab displays quality history endpoint failure', async () => {
    vi.mocked(aiApi.qualityScoreHistory).mockRejectedValueOnce(new Error('history offline'))

    renderTab(<QualityHeatmapTab scopeKbId="7" />)

    expect(await screen.findByText(/质量分历史加载失败（POST \/ai\/evolution\/quality-score\/history）：history offline/)).toBeInTheDocument()
    expect(screen.getByText(/页面不会用静态趋势补齐/)).toBeInTheDocument()
  })

  it('QualityHeatmapTab uses theme-aware heatmap and baseline colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderTabWithTheme(<QualityHeatmapTab scopeKbId="7" />)

    const heatmapChart = await screen.findByTestId('evolution-quality-heatmap-chart-surface')
    const trendChart = await screen.findByTestId('evolution-quality-trend-chart-surface')
    await waitFor(() => expect(heatmapChart.textContent).toContain('≥9 优秀'))

    expect(heatmapChart.textContent).not.toContain('#4caf50')
    expect(heatmapChart.textContent).not.toContain('#2196f3')
    expect(heatmapChart.textContent).not.toContain('#ff9800')
    expect(heatmapChart.textContent).not.toContain('#f44336')
    expect(trendChart.textContent).not.toContain('#ff9800')
    expect(heatmapChart).toHaveAttribute('data-chart-colors', '#81c784|#4fc3f7|#ffb74d|#e57373')
    expect(trendChart).toHaveAttribute('data-baseline-color', '#ffb74d')
  })

  it('EvolutionMapTab displays topic-list endpoint failure without fake topology', async () => {
    vi.mocked(aiApi.topicList).mockRejectedValueOnce(new Error('topics offline'))

    renderTab(<EvolutionMapTab scopeKbId="7" />)

    expect(await screen.findByText(/主题拓扑加载失败（POST \/ai\/evolution\/topic\/list）：topics offline/)).toBeInTheDocument()
    expect(screen.getByText(/页面不会用静态拓扑替代真实主题池/)).toBeInTheDocument()
  })

  it('EvolutionMapTab uses theme-aware legend colors in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(aiApi.topicList).mockResolvedValueOnce([
      { id: 41, kbId: 7, topicName: '屏障修护脚本证据链', category: 'script', priority: 1, status: 1 },
      { id: 42, kbId: 7, topicName: '未归类新增主题', category: 'script', priority: 3, status: 2 },
    ] as never)

    renderTabWithTheme(<EvolutionMapTab scopeKbId="7" />)

    expect(await screen.findByText('主题示意拓扑（力导向）')).toBeInTheDocument()
    const legendDots = screen.getAllByTestId('evolution-map-legend-dot-surface')
    expect(legendDots[3]).not.toHaveStyle({
      backgroundColor: 'rgb(224, 224, 224)',
    })
  })
})
