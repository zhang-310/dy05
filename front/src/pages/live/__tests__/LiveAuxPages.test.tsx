import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import ScriptRankingPage from '../ScriptRankingPage'
import RhythmPage from '../RhythmPage'
import HistoryComparePage from '../HistoryComparePage'
import { liveApi } from '@/api/live'

vi.mock('@/api/live', () => ({
  liveApi: {
    effectivenessRanking: vi.fn(),
    effectivenessTopScripts: vi.fn(),
    rhythmOptimize: vi.fn(),
    rhythmSave: vi.fn(),
    getVersionsByScriptId: vi.fn(),
    versionDiff: vi.fn(),
  },
}))

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => (
    <div data-testid="mock-echarts">{JSON.stringify(option)}</div>
  ),
}))

function renderWithTheme(ui: React.ReactElement) {
  return renderWithProviders(
    <AppThemeProvider>
      {ui}
    </AppThemeProvider>,
  )
}

describe('Live auxiliary pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
  })

  it('loads script ranking with sessionId contract and shows load errors', async () => {
    vi.mocked(liveApi.effectivenessRanking).mockResolvedValue({
      total: 1,
      list: [{
        scriptId: 11,
        totalScore: 86,
        ranking: 1,
        tag: '高转化',
        conversionRate: 0.18,
        likes: 120,
        comments: 8,
        completionRate: 0.72,
        sampleSize: 300,
      }],
      pageNum: 0,
      pageSize: 30,
    })
    vi.mocked(liveApi.effectivenessTopScripts).mockRejectedValue(new Error('top failed'))

    renderWithProviders(<ScriptRankingPage />)

    expect(screen.getByTestId('live-script-ranking-workbench')).toHaveAttribute('data-contract-scope', 'live-script-effectiveness-ranking')
    expect(screen.getByTestId('live-script-ranking-workbench')).toHaveAttribute('data-ready-endpoints', '/live/effectiveness/ranking|/live/effectiveness/top-scripts')
    expect(screen.getByTestId('live-script-ranking-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('days-local-aggregate'))
    expect(screen.getByTestId('live-script-ranking-contract-alert')).toHaveAttribute('data-no-days-local-aggregate', 'true')
    expect(screen.getByTestId('live-script-ranking-contract-alert')).toHaveAttribute('data-no-script-type-local-aggregate', 'true')
    expect(screen.getByTestId('live-script-ranking-contract-alert')).toHaveAttribute('data-no-ai-generate', 'true')
    expect(screen.getByTestId('live-script-ranking-filter-card')).toHaveAttribute('data-only-session-filter', 'true')
    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '18' } })
    fireEvent.click(screen.getByRole('button', { name: '加载排行' }))

    await waitFor(() => {
      expect(liveApi.effectivenessRanking).toHaveBeenCalledWith({ sessionId: 18, page: 0, pageSize: 30 })
      expect(liveApi.effectivenessTopScripts).toHaveBeenCalledWith({ sessionId: 18, limit: 10 })
    })
    expect(await screen.findByTestId('live-script-top-scripts-error')).toHaveTextContent('/live/effectiveness/top-scripts Top 话术加载失败')
    expect(screen.getByTestId('live-script-top-scripts-error')).toHaveAttribute('data-no-static-top-scripts-fallback', 'true')
    expect(screen.getByText(/sessionId=18; limit=10/)).toBeInTheDocument()
  })

  it('normalizes wrapped ranking and top script responses', async () => {
    vi.mocked(liveApi.effectivenessRanking).mockResolvedValue({
      records: [{
        scriptId: 12,
        totalScore: 91,
        ranking: 1,
        tag: '包装排行',
        conversionRate: 0.22,
        likes: 220,
        comments: 18,
        completionRate: 0.82,
        sampleSize: 500,
      }],
      totalElements: 1,
    } as never)
    vi.mocked(liveApi.effectivenessTopScripts).mockResolvedValue({
      items: [{
        scriptId: 12,
        totalScore: 91,
        tag: '包装排行',
        conversionRate: 0.22,
        likes: 220,
        comments: 18,
        completionRate: 0.82,
        sampleSize: 500,
      }],
      count: 1,
    } as never)

    renderWithProviders(<ScriptRankingPage />)

    expect(screen.getByTestId('live-script-ranking-empty-session')).toHaveAttribute('data-no-days-local-aggregate', 'true')
    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '19' } })
    fireEvent.click(screen.getByRole('button', { name: '加载排行' }))

    expect(await screen.findByText('包装排行')).toBeInTheDocument()
    expect(screen.getByText('话术 #12')).toBeInTheDocument()
  })

  it('uses theme-aware script ranking chart and rank badges in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(liveApi.effectivenessRanking).mockResolvedValue({
      total: 4,
      list: [
        { scriptId: 11, totalScore: 96, ranking: 1 },
        { scriptId: 12, totalScore: 91, ranking: 2 },
        { scriptId: 13, totalScore: 88, ranking: 3 },
        { scriptId: 14, totalScore: 80, ranking: 4 },
      ],
      pageNum: 0,
      pageSize: 30,
    })
    vi.mocked(liveApi.effectivenessTopScripts).mockResolvedValue([
      { scriptId: 11, totalScore: 96, tag: '冠军', conversionRate: 0.31, completionRate: 0.86, likes: 320, comments: 42, sampleSize: 600 },
      { scriptId: 12, totalScore: 91, tag: '亚军', conversionRate: 0.28, completionRate: 0.81, likes: 260, comments: 32, sampleSize: 520 },
      { scriptId: 13, totalScore: 88, tag: '季军', conversionRate: 0.24, completionRate: 0.78, likes: 210, comments: 25, sampleSize: 460 },
      { scriptId: 14, totalScore: 80, tag: '稳定', conversionRate: 0.18, completionRate: 0.7, likes: 160, comments: 16, sampleSize: 360 },
    ] as never)

    renderWithTheme(<ScriptRankingPage />)

    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '18' } })
    fireEvent.click(screen.getByRole('button', { name: '加载排行' }))

    expect(await screen.findByText('冠军')).toBeInTheDocument()
    expect(screen.getByTestId('live-script-ranking-chart-surface')).toHaveAttribute('data-chart-color', '#e3f2fd')
    const badges = screen.getAllByTestId('live-script-ranking-badge-surface')
    expect(badges.map(node => node.getAttribute('data-rank-color'))).toEqual([
      '#ffb74d',
      '#ffb74d',
      '#ffb74d',
      '#e3f2fd',
    ])
    const chartText = screen.getByTestId('mock-echarts').textContent ?? ''
    expect(chartText).not.toContain('#1976d2')
    for (const badge of badges) {
      expect(window.getComputedStyle(badge).color).not.toBe('rgb(255, 255, 255)')
    }
  })

  it('shows ranking and top empty states without static fallback', async () => {
    vi.mocked(liveApi.effectivenessRanking).mockResolvedValue({ list: [], total: 0 } as never)
    vi.mocked(liveApi.effectivenessTopScripts).mockResolvedValue([] as never)

    renderWithProviders(<ScriptRankingPage />)

    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '20' } })
    fireEvent.click(screen.getByRole('button', { name: '加载排行' }))

    expect(await screen.findByTestId('live-script-top-scripts-empty')).toHaveTextContent('该场次暂无 Top 话术')
    expect(screen.getByTestId('live-script-top-scripts-empty')).toHaveAttribute('data-no-static-top-scripts-fallback', 'true')
    expect(screen.getByTestId('live-script-ranking-workbench')).toHaveAttribute('data-ranking-count', '0')
    expect(screen.getByTestId('live-script-ranking-workbench')).toHaveAttribute('data-top-count', '0')
  })

  it('shows ranking error without static fallback', async () => {
    vi.mocked(liveApi.effectivenessRanking).mockRejectedValue(new Error('ranking down'))
    vi.mocked(liveApi.effectivenessTopScripts).mockResolvedValue([] as never)

    renderWithProviders(<ScriptRankingPage />)

    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '21' } })
    fireEvent.click(screen.getByRole('button', { name: '加载排行' }))

    expect(await screen.findByTestId('live-script-ranking-error')).toHaveTextContent('/live/effectiveness/ranking 排行榜加载失败：ranking down')
    expect(screen.getByTestId('live-script-ranking-error')).toHaveAttribute('data-no-static-ranking-fallback', 'true')
  })


  it('optimizes and saves rhythm slots through real backend payload', async () => {
    vi.mocked(liveApi.rhythmOptimize).mockResolvedValue({
      status: 'success',
      optimization: '{"overallDuration":60}',
    })
    vi.mocked(liveApi.rhythmSave).mockResolvedValue({ updatedSlots: 1 })

    renderWithProviders(<RhythmPage />)

    expect(screen.getByTestId('live-rhythm-workbench')).toHaveAttribute('data-contract-scope', 'live-rhythm-slot-editor')
    expect(screen.getByTestId('live-rhythm-workbench')).toHaveAttribute('data-ready-endpoints', '/live/rhythm/optimize|/live/rhythm/save-rhythm')
    expect(screen.getByTestId('live-rhythm-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('rhythm-get-rhythm'))
    expect(screen.getByTestId('live-rhythm-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('local-plan-fallback'))
    expect(screen.getByTestId('live-rhythm-contract-alert')).toHaveAttribute('data-no-get-rhythm', 'true')
    expect(screen.getByTestId('live-rhythm-contract-alert')).toHaveAttribute('data-no-suggest-legacy', 'true')
    expect(screen.getByTestId('live-rhythm-contract-alert')).toHaveAttribute('data-no-local-plan-fallback', 'true')
    expect(screen.getByTestId('live-rhythm-control-card')).toHaveAttribute('data-no-read-before-edit', 'true')
    expect(screen.getByTestId('live-rhythm-slot-editor')).toHaveAttribute('data-no-extra-fields', 'true')
    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '18' } })
    fireEvent.change(screen.getByLabelText('话术 ID'), { target: { value: '101' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI 优化建议' }))

    await waitFor(() => expect(liveApi.rhythmOptimize).toHaveBeenCalledWith(18))
    expect(await screen.findByText(/overallDuration/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '保存槽位' }))

    await waitFor(() => {
      expect(liveApi.rhythmSave).toHaveBeenCalledWith({
        sessionId: 18,
        slots: [{ scriptId: 101, sequenceNo: 1, durationLimitSec: 60 }],
      })
    })
    expect(await screen.findByTestId('live-rhythm-save-success')).toHaveAttribute('data-updated-slots', '1')
    expect(screen.getByText(/当前控制器只提供优化建议和保存槽位/)).toBeInTheDocument()
  })

  it('RhythmPage uses a theme-aware optimization preview surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(liveApi.rhythmOptimize).mockResolvedValue({
      status: 'success',
      optimization: '{"overallDuration":60}',
    })
    vi.mocked(liveApi.rhythmSave).mockResolvedValue({ updatedSlots: 1 })

    renderWithTheme(<RhythmPage />)

    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '18' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI 优化建议' }))

    expect(await screen.findByText(/overallDuration/)).toBeInTheDocument()
    expect(screen.getByTestId('live-rhythm-optimization-card')).toHaveAttribute('data-no-local-optimization-fallback', 'true')
    expect(screen.getByTestId('live-rhythm-optimization-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('shows rhythm endpoint sources when optimize or save fails', async () => {
    vi.mocked(liveApi.rhythmOptimize).mockRejectedValue(new Error('optimizer down'))
    vi.mocked(liveApi.rhythmSave).mockRejectedValue(new Error('save down'))

    renderWithProviders(<RhythmPage />)

    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '18' } })
    fireEvent.change(screen.getByLabelText('话术 ID'), { target: { value: '101' } })
    fireEvent.click(screen.getByRole('button', { name: 'AI 优化建议' }))
    expect(await screen.findByTestId('live-rhythm-optimize-error')).toHaveTextContent('/live/rhythm/optimize 节奏优化失败：optimizer down')
    expect(screen.getByTestId('live-rhythm-optimize-error')).toHaveAttribute('data-no-local-optimization-fallback', 'true')
    expect(screen.getByText(/route=\/admin\/live\/rhythm; sessionId=18/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '保存槽位' }))
    expect(await screen.findByTestId('live-rhythm-save-error')).toHaveTextContent('/live/rhythm/save-rhythm 保存节奏失败：save down')
    expect(screen.getByTestId('live-rhythm-save-error')).toHaveAttribute('data-no-local-save-fallback', 'true')
    expect(screen.getByTestId('live-rhythm-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/slotCount=1; slots=101:1\/60s/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('101')).toBeInTheDocument()
  })

  it('keeps rhythm slot validation local and disables save without fallback plans', async () => {
    vi.mocked(liveApi.rhythmOptimize).mockResolvedValue({ status: 'success', optimization: '{}' })
    vi.mocked(liveApi.rhythmSave).mockResolvedValue({ updatedSlots: 1 })

    renderWithProviders(<RhythmPage />)

    fireEvent.change(screen.getByLabelText('直播场次 ID'), { target: { value: '18' } })
    fireEvent.change(screen.getByLabelText('话术 ID'), { target: { value: '101' } })
    fireEvent.change(screen.getByLabelText('顺序'), { target: { value: '0' } })

    expect(screen.getByTestId('live-rhythm-validation-error')).toHaveAttribute('data-no-local-plan-fallback', 'true')
    expect(screen.getByTestId('live-rhythm-workbench')).toHaveAttribute('data-has-invalid-slot', 'true')
    expect(screen.getByTestId('live-rhythm-slot-card')).toHaveAttribute('data-sequence-no', '0')
    expect(screen.getByRole('button', { name: '保存槽位' })).toBeDisabled()
    expect(liveApi.rhythmSave).not.toHaveBeenCalled()
  })

  it('loads versions and compares with normalized diff data', async () => {
    vi.mocked(liveApi.getVersionsByScriptId).mockResolvedValue([
      {
        id: 1,
        scriptId: 9,
        versionNo: 1,
        versionLabel: '初版',
        content: '旧话术',
        scriptContent: '旧话术',
        createTime: '2026-05-20T10:00:00',
      },
      {
        id: 2,
        scriptId: 9,
        versionNo: 2,
        versionLabel: '新版',
        content: '新话术',
        scriptContent: '新话术',
        createTime: '2026-05-21T10:00:00',
        isActive: true,
      },
    ])
    vi.mocked(liveApi.versionDiff).mockResolvedValue({
      oldVersionId: 1,
      newVersionId: 2,
      oldVersionNo: 1,
      newVersionNo: 2,
      oldContent: '旧话术',
      newContent: '新话术',
      similarity: 88,
      recommendation: '推荐使用新版',
    })

    renderWithProviders(<HistoryComparePage />)

    expect(screen.getByTestId('live-history-compare-workbench')).toHaveAttribute('data-contract-scope', 'live-script-version-compare')
    expect(screen.getByTestId('live-history-compare-workbench')).toHaveAttribute('data-ready-endpoints', '/live/script/version/getByScriptId|/live/script/version/diff')
    expect(screen.getByTestId('live-history-compare-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('version-save'))
    expect(screen.getByTestId('live-history-compare-workbench')).toHaveAttribute('data-unsupported-actions', expect.stringContaining('shortvideo-export'))
    expect(screen.getByTestId('live-history-contract-alert')).toHaveAttribute('data-no-version-save', 'true')
    expect(screen.getByTestId('live-history-contract-alert')).toHaveAttribute('data-no-version-activate', 'true')
    expect(screen.getByTestId('live-history-contract-alert')).toHaveAttribute('data-no-local-diff-fallback', 'true')
    expect(screen.getByTestId('live-history-control-card')).toHaveAttribute('data-only-script-id-filter', 'true')
    fireEvent.change(screen.getByLabelText('话术脚本 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '加载版本列表' }))

    await waitFor(() => expect(liveApi.getVersionsByScriptId).toHaveBeenCalledWith(9))
    expect(await screen.findByLabelText('旧版本')).toBeInTheDocument()
    expect(screen.getByTestId('live-history-version-list-card')).toHaveAttribute('data-no-version-mutation', 'true')
    expect(screen.getByTestId('live-history-version-list-card')).toHaveAttribute('data-version-count', '2')
    const versionChips = screen.getAllByTestId('live-history-version-chip')
    expect(versionChips[1]).toHaveAttribute('data-version-active', 'true')

    fireEvent.mouseDown(screen.getByLabelText('旧版本'))
    fireEvent.click(await screen.findByRole('option', { name: /初版/ }))
    fireEvent.mouseDown(screen.getByLabelText('新版本'))
    const listbox = screen.getByRole('listbox')
    fireEvent.click(within(listbox).getByRole('option', { name: /新版 \(/ }))

    await waitFor(() => {
      expect(liveApi.versionDiff).toHaveBeenCalledWith({ versionId1: 1, versionId2: 2 })
    })
    const diffResult = await screen.findByTestId('live-history-diff-result')
    expect(diffResult).toHaveAttribute('data-no-local-diff-fallback', 'true')
    expect(diffResult).toHaveAttribute('data-old-version-id', '1')
    expect(diffResult).toHaveAttribute('data-new-version-id', '2')
    expect(screen.getByTestId('live-history-diff-recommendation')).toHaveAttribute('data-contract-source', '/live/script/version/diff')
    expect(await screen.findByText('推荐使用新版')).toBeInTheDocument()
    expect(screen.getByText('旧话术')).toBeInTheDocument()
    expect(screen.getByText('新话术')).toBeInTheDocument()
  })

  it('HistoryComparePage uses a theme-aware diff summary surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    vi.mocked(liveApi.getVersionsByScriptId).mockResolvedValue([
      {
        id: 1,
        scriptId: 9,
        versionNo: 1,
        versionLabel: '初版',
        content: '旧话术',
        scriptContent: '旧话术',
        createTime: '2026-05-20T10:00:00',
      },
      {
        id: 2,
        scriptId: 9,
        versionNo: 2,
        versionLabel: '新版',
        content: '新话术',
        scriptContent: '新话术',
        createTime: '2026-05-21T10:00:00',
      },
    ])
    vi.mocked(liveApi.versionDiff).mockResolvedValue({
      oldVersionId: 1,
      newVersionId: 2,
      oldVersionNo: 1,
      newVersionNo: 2,
      oldContent: '旧话术',
      newContent: '新话术',
      diffHtml: '<del>旧卖点</del><ins>新卖点</ins>',
      changedFields: ['scriptContent'],
    })

    renderWithTheme(<HistoryComparePage />)

    fireEvent.change(screen.getByLabelText('话术脚本 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '加载版本列表' }))
    expect(await screen.findByLabelText('旧版本')).toBeInTheDocument()

    fireEvent.mouseDown(screen.getByLabelText('旧版本'))
    fireEvent.click(await screen.findByRole('option', { name: /初版/ }))
    fireEvent.mouseDown(screen.getByLabelText('新版本'))
    fireEvent.click(within(screen.getByRole('listbox')).getByRole('option', { name: /^新版 \(/ }))

    expect(await screen.findByText('差异摘要')).toBeInTheDocument()
    expect(screen.getByTestId('live-history-diff-summary-card')).toHaveAttribute('data-contract-source', '/live/script/version/diff')
    expect(screen.getByTestId('live-history-diff-summary-surface')).toHaveAttribute('data-no-local-diff-fallback', 'true')
    expect(screen.getByTestId('live-history-diff-summary-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('shows version endpoint sources when version list or diff fails', async () => {
    vi.mocked(liveApi.getVersionsByScriptId).mockRejectedValueOnce(new Error('version list down'))

    const { unmount } = renderWithProviders(<HistoryComparePage />)

    fireEvent.change(screen.getByLabelText('话术脚本 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '加载版本列表' }))
    expect(await screen.findByTestId('live-history-version-list-error')).toHaveTextContent('/live/script/version/getByScriptId 版本列表加载失败：version list down')
    expect(screen.getByTestId('live-history-version-list-error')).toHaveAttribute('data-no-static-version-fallback', 'true')
    expect(screen.getByText(/route=\/admin\/live\/history-compare; scriptId=9/)).toBeInTheDocument()

    unmount()
    vi.clearAllMocks()
    vi.mocked(liveApi.getVersionsByScriptId).mockResolvedValue([
      { id: 1, scriptId: 9, versionNo: 1, versionLabel: '初版', scriptContent: '旧话术', createTime: '2026-05-20T10:00:00' },
      { id: 2, scriptId: 9, versionNo: 2, versionLabel: '新版', scriptContent: '新话术', createTime: '2026-05-21T10:00:00' },
    ])
    vi.mocked(liveApi.versionDiff).mockRejectedValue(new Error('diff down'))

    renderWithProviders(<HistoryComparePage />)
    fireEvent.change(screen.getByLabelText('话术脚本 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '加载版本列表' }))
    expect(await screen.findByLabelText('旧版本')).toBeInTheDocument()

    fireEvent.mouseDown(screen.getByLabelText('旧版本'))
    fireEvent.click(await screen.findByRole('option', { name: /初版/ }))
    fireEvent.mouseDown(screen.getByLabelText('新版本'))
    fireEvent.click(within(screen.getByRole('listbox')).getByRole('option', { name: /^新版 \(/ }))

    expect(await screen.findByTestId('live-history-diff-error')).toHaveTextContent('/live/script/version/diff 版本对比失败：diff down')
    expect(screen.getByTestId('live-history-diff-error')).toHaveAttribute('data-no-local-diff-fallback', 'true')
    expect(screen.getByTestId('live-history-diff-error')).toHaveAttribute('data-selection-retained', 'true')
    expect(screen.getByText(/oldVersionId=1; newVersionId=2/)).toBeInTheDocument()
    expect(screen.getByLabelText('旧版本')).toHaveTextContent(/初版/)
    expect(screen.getByLabelText('新版本')).toHaveTextContent(/新版/)
  })

  it('shows empty versions and same-version warning without static diff fallback', async () => {
    vi.mocked(liveApi.getVersionsByScriptId).mockResolvedValueOnce([])

    const { unmount } = renderWithProviders(<HistoryComparePage />)

    fireEvent.change(screen.getByLabelText('话术脚本 ID'), { target: { value: '10' } })
    fireEvent.click(screen.getByRole('button', { name: '加载版本列表' }))

    expect(await screen.findByTestId('live-history-empty-versions')).toHaveAttribute('data-no-static-version-fallback', 'true')
    expect(screen.getByTestId('live-history-compare-workbench')).toHaveAttribute('data-version-count', '0')
    expect(liveApi.versionDiff).not.toHaveBeenCalled()

    unmount()
    vi.clearAllMocks()
    vi.mocked(liveApi.getVersionsByScriptId).mockResolvedValue([
      { id: 1, scriptId: 9, versionNo: 1, versionLabel: '初版', scriptContent: '旧话术', createTime: '2026-05-20T10:00:00' },
      { id: 2, scriptId: 9, versionNo: 2, versionLabel: '新版', scriptContent: '新话术', createTime: '2026-05-21T10:00:00' },
    ])

    renderWithProviders(<HistoryComparePage />)
    fireEvent.change(screen.getByLabelText('话术脚本 ID'), { target: { value: '9' } })
    fireEvent.click(screen.getByRole('button', { name: '加载版本列表' }))
    expect(await screen.findByLabelText('旧版本')).toBeInTheDocument()

    fireEvent.mouseDown(screen.getByLabelText('旧版本'))
    fireEvent.click(await screen.findByRole('option', { name: /初版/ }))
    fireEvent.mouseDown(screen.getByLabelText('新版本'))
    fireEvent.click(within(screen.getByRole('listbox')).getByRole('option', { name: /初版/ }))

    expect(screen.getByTestId('live-history-same-version-warning')).toHaveAttribute('data-no-local-diff-fallback', 'true')
    expect(liveApi.versionDiff).not.toHaveBeenCalled()
  })
})
