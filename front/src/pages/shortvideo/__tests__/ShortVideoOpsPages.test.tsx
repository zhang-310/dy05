import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor, within } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import HotTopicPage from '../HotTopicPage'
import HotTopicCreationPage from '../HotTopicCreationPage'
import ContentCalendarPage from '../ContentCalendarPage'
import RemakeTemplatePage from '../RemakeTemplatePage'
import CompetitorMonitorPage from '../CompetitorMonitorPage'
import { shortvideoApi } from '@/api/shortvideo'
import { fetchHotTopicPool, generateHotspotFused } from '@/api/viral-analysis'
import { addCompetitor, analyzeCompetitor, generateWeeklyReport, listCompetitors, removeCompetitor } from '@/api/competitor'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => <div data-testid="echarts">{JSON.stringify(option)}</div>,
}))

vi.mock('@/api/shortvideo', async () => {
  const actual = await vi.importActual<typeof import('@/api/shortvideo')>('@/api/shortvideo')
  return {
    ...actual,
    shortvideoApi: {
      save: vi.fn(),
      aiGenerateScript: vi.fn(),
      contentCalendarView: vi.fn(),
      contentCalendarMonthStats: vi.fn(),
      seoSuggestPublishTime: vi.fn(),
      contentCalendarAutoGenerate: vi.fn(),
      contentCalendarSave: vi.fn(),
      remakeTemplateList: vi.fn(),
      remakeTemplateSave: vi.fn(),
      remakeTemplateDelete: vi.fn(),
      aiGenerateScriptRich: vi.fn(),
    },
    trendsCurrent: vi.fn().mockResolvedValue([
      { keyword: '屏障修护', hotScore: 12000, videoCount: 300, growthRate: 12, category: '护肤', source: '抖音热榜', relatedKeywords: ['敏感肌'] },
    ]),
  }
})

vi.mock('@/api/viral-analysis', () => ({
  fetchHotTopicPool: vi.fn(),
  generateHotspotFused: vi.fn(),
}))

vi.mock('@/api/competitor', () => ({
  addCompetitor: vi.fn(),
  listCompetitors: vi.fn(),
  analyzeCompetitor: vi.fn(),
  generateWeeklyReport: vi.fn(),
  removeCompetitor: vi.fn(),
}))

const toast = vi.fn()
const navigate = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

vi.mock('@/hooks/useRolePrefix', () => ({
  useRolePrefix: () => '/admin',
}))

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return {
    ...actual,
    useNavigate: () => navigate,
  }
})

vi.mock('@/components/base', async () => {
  const actual = await vi.importActual<typeof import('@/components/base')>('@/components/base')
  return {
    ...actual,
    StandardDataGrid: ({ rows, columns, loading }: any) => (
      <div>
        {loading && <span>loading</span>}
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

describe('ShortVideo ops pages', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.clear()
    Object.defineProperty(navigator, 'clipboard', {
      configurable: true,
      value: { writeText: vi.fn().mockResolvedValue(undefined) },
    })
    vi.mocked(shortvideoApi.save).mockResolvedValue(9 as never)
    vi.mocked(shortvideoApi.aiGenerateScript).mockResolvedValue('热点脚本创意' as never)
    vi.mocked(shortvideoApi.aiGenerateScriptRich).mockResolvedValue({ content: '热点脚本创意', officialReferences: [] } as never)
    vi.mocked(shortvideoApi.contentCalendarView).mockResolvedValue({ days: {} } as never)
    vi.mocked(shortvideoApi.contentCalendarMonthStats).mockResolvedValue({ plannedCount: 1, publishedCount: 0, completionRate: 0 } as never)
    vi.mocked(shortvideoApi.seoSuggestPublishTime).mockResolvedValue(['今晚 20:00'] as never)
    vi.mocked(shortvideoApi.contentCalendarAutoGenerate).mockResolvedValue(7 as never)
    vi.mocked(shortvideoApi.contentCalendarSave).mockResolvedValue(18 as never)
    vi.mocked(shortvideoApi.remakeTemplateList).mockResolvedValue({
      total: 1,
      list: [{ id: 1, templateName: '爆款结构', remakeType: 'form_copy', adaptationGuide: '三段式', usageCount: 2, createTime: '2026-05-21 10:00:00' }],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(shortvideoApi.remakeTemplateSave).mockResolvedValue({ id: 1, templateName: '爆款结构', remakeType: 'form_copy', createTime: '2026-05-21' } as never)
    vi.mocked(fetchHotTopicPool).mockResolvedValue({
      hotTopics: [{ id: 5, topic: '屏障修护', heat: 12000, source: '抖音热榜' }],
    } as never)
    vi.mocked(generateHotspotFused).mockResolvedValue({ fusedScript: '融合脚本' } as never)
    vi.mocked(listCompetitors).mockResolvedValue([{ id: 3, accountId: 'acc-1', accountName: '竞品 A', platform: 'douyin' }])
    vi.mocked(addCompetitor).mockResolvedValue('添加成功')
    vi.mocked(analyzeCompetitor).mockResolvedValue({ analysis: '内容策略' })
    vi.mocked(generateWeeklyReport).mockResolvedValue('竞品周报')
    vi.mocked(removeCompetitor).mockResolvedValue(undefined)
  })

  it('HotTopicPage uses real ai script endpoint instead of local template', async () => {
    renderWithProviders(
      <MemoryRouter>
        <HotTopicPage />
      </MemoryRouter>,
    )

    expect(screen.getByText(/脚本创意生成走/)).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-page')).toHaveAttribute(
      'data-ready-endpoints',
      '/short-video/cross/hot-topic-pool|/short-video/ai/generate-script-rich|/short-video/project/save',
    )
    expect(screen.getByTestId('hot-topic-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/ai/template-script'),
    )
    expect(screen.getByTestId('hot-topic-boundary-contract')).toHaveAttribute('data-client-only-favorite', 'true')
    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-row-card')).toHaveAttribute('data-source-endpoint', '/short-video/cross/hot-topic-pool')
    fireEvent.click(screen.getByLabelText('生成脚本创意'))
    fireEvent.click(await screen.findByRole('button', { name: '生成创意' }))

    await waitFor(() => {
      expect(shortvideoApi.aiGenerateScriptRich).toHaveBeenCalledWith({
        copyText: '屏障修护',
        sceneType: '热点借势',
        style: 'trend',
        duration: 60,
      })
    })
    expect(await screen.findByText('热点脚本创意')).toBeInTheDocument()
  })

  it('HotTopicPage tolerates wrapped trend rows from page mocks', async () => {
    const shortvideoModule = await import('@/api/shortvideo')
    vi.mocked(shortvideoModule.trendsCurrent).mockResolvedValueOnce({
      records: [
        { keyword: '包装热点', hotScore: 9000, videoCount: 12, growthRate: 6, category: '护肤', source: '抖音热榜' },
      ],
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <HotTopicPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装热点')).toBeInTheDocument()
    expect(screen.getByText('热度 9.0k')).toBeInTheDocument()
  })

  it('HotTopicPage uses theme-aware rank chart colors in dark mode', async () => {
    const shortvideoModule = await import('@/api/shortvideo')
    vi.mocked(shortvideoModule.trendsCurrent).mockResolvedValueOnce([
      { keyword: '热点一', hotScore: 12000, videoCount: 300, growthRate: 12, category: '护肤', source: '抖音热榜' },
      { keyword: '热点二', hotScore: 11000, videoCount: 260, growthRate: 9, category: '护肤', source: '抖音热榜' },
      { keyword: '热点三', hotScore: 9000, videoCount: 200, growthRate: 7, category: '护肤', source: '微博' },
      { keyword: '热点四', hotScore: 6000, videoCount: 120, growthRate: 3, category: '护肤', source: 'B站' },
    ] as never)
    window.localStorage.setItem('dy-theme-mode', 'dark')

    renderWithProviders(
      <AppThemeProvider>
        <MemoryRouter>
          <HotTopicPage />
        </MemoryRouter>
      </AppThemeProvider>,
    )

    const chart = await screen.findByTestId('hot-topic-rank-chart-surface')
    expect(chart).toHaveAttribute('data-chart-colors', '#e57373|#ffb74d|#f3e5f5|#4fc3f7')
    expect(chart).toHaveAttribute('data-no-static-rank', 'true')
    const optionText = (await screen.findByTestId('echarts')).textContent ?? ''
    expect(optionText).not.toContain('#f44336')
    expect(optionText).not.toContain('#ff5722')
    expect(optionText).not.toContain('#ff9800')
    expect(optionText).not.toContain('#42a5f5')
  })

  it('HotTopicCreationPage calls hotspot fusion with personaId contract', async () => {
    renderWithProviders(
      <MemoryRouter>
        <HotTopicCreationPage />
      </MemoryRouter>,
    )

    expect(screen.getByText(/后端必填 hotTopicId 和 personaId/)).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-creation-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/persona-fusion/generate-hotspot-fused'),
    )
    expect(screen.getByTestId('hot-topic-creation-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/persona-fusion/mock-hotspot-fused'),
    )
    expect(screen.getByTestId('hot-topic-creation-boundary-contract')).toHaveAttribute('data-no-local-fusion-template', 'true')
    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-card')).toHaveAttribute('data-no-local-tier-source', 'client-time-only')
    fireEvent.click(screen.getByRole('button', { name: '融合' }))
    fireEvent.change(await screen.findByLabelText(/Persona ID/), { target: { value: '12' } })
    fireEvent.change(screen.getByLabelText(/Product ID/), { target: { value: '6' } })
    fireEvent.click(screen.getByRole('button', { name: '开始融合' }))

    await waitFor(() => {
      expect(generateHotspotFused).toHaveBeenCalledWith(5, 12, 6)
    })
    expect(screen.getByTestId('hot-topic-fusion-result')).toBeInTheDocument()
    expect(await screen.findByDisplayValue('融合脚本')).toBeInTheDocument()
  })

  it('HotTopicCreationPage tolerates wrapped hot topic pool mocks', async () => {
    vi.mocked(fetchHotTopicPool).mockResolvedValueOnce({
      records: [{ id: 9, topic: '包装热点池', heat: 8800, source: '统一热点池' }],
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <HotTopicCreationPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装热点池')).toBeInTheDocument()
    expect(screen.getByText('来源：统一热点池')).toBeInTheDocument()
  })

  it('HotTopicCreationPage keeps hotspot fusion failures visible in dialog', async () => {
    vi.mocked(generateHotspotFused).mockRejectedValueOnce(new Error('人设不存在') as never)

    renderWithProviders(
      <MemoryRouter>
        <HotTopicCreationPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '融合' }))
    fireEvent.change(await screen.findByLabelText(/Persona ID/), { target: { value: '999' } })
    fireEvent.click(screen.getByRole('button', { name: '开始融合' }))

    expect(await screen.findByText(/融合生成失败（POST \/short-video\/persona-fusion\/generate-hotspot-fused）：人设不存在/)).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-fusion-error')).toHaveAttribute('data-no-local-fusion-template', 'true')
    expect(screen.getAllByText(/generate-hotspot-fused/).length).toBeGreaterThan(1)
  })

  it('HotTopicCreationPage shows hot topic pool endpoint when list loading fails', async () => {
    vi.mocked(fetchHotTopicPool).mockRejectedValueOnce(new Error('hot topic sync down') as never)

    renderWithProviders(
      <MemoryRouter>
        <HotTopicCreationPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/加载热点话题失败（POST \/short-video\/cross\/hot-topic-pool）：hot topic sync down/)).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-pool-error')).toHaveAttribute('data-no-local-hot-topic-fallback', 'true')
    expect(screen.getByText(/页面不会使用本地模拟热榜/)).toBeInTheDocument()
  })

  it('HotTopicCreationPage writes fused content by navigation only', async () => {
    renderWithProviders(
      <MemoryRouter>
        <HotTopicCreationPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '融合' }))
    fireEvent.change(await screen.findByLabelText(/Persona ID/), { target: { value: '12' } })
    fireEvent.click(screen.getByRole('button', { name: '开始融合' }))
    expect(await screen.findByDisplayValue('融合脚本')).toBeInTheDocument()
    fireEvent.click(screen.getByTestId('hot-topic-write-to-script-link'))

    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('/shortvideo/script-planning?'))
    expect(navigate).toHaveBeenCalledWith(expect.stringContaining('preset='))
    expect(screen.getByTestId('hot-topic-write-to-script-link')).toHaveAttribute('data-navigation-only', 'true')
  })

  it('HotTopicPage keeps selected topics when project save fails and does not fabricate a project', async () => {
    vi.mocked(shortvideoApi.save).mockRejectedValueOnce(new Error('project save denied') as never)

    renderWithProviders(
      <MemoryRouter>
        <HotTopicPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    fireEvent.click(screen.getByText('屏障修护'))
    fireEvent.click(await screen.findByRole('button', { name: /保存为项目/ }))

    expect(await screen.findByText(/保存热点项目失败（POST \/short-video\/project\/save）：project save denied/)).toBeInTheDocument()
    expect(screen.getByText(/已选话题和 publishTitle 会保留/)).toBeInTheDocument()
    expect(screen.getByText('已选话题（1）')).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-project-save-error')).toHaveAttribute('data-no-local-project-create', 'true')
    expect(screen.getByTestId('hot-topic-selected-topics')).toHaveAttribute('data-input-retained', 'true')
  })

  it('HotTopicPage keeps keyword and reports ai script endpoint when idea generation fails', async () => {
    vi.mocked(shortvideoApi.aiGenerateScriptRich).mockRejectedValueOnce(new Error('script llm down') as never)

    renderWithProviders(
      <MemoryRouter>
        <HotTopicPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    fireEvent.click(screen.getByLabelText('生成脚本创意'))
    fireEvent.click(await screen.findByRole('button', { name: '生成创意' }))

    expect(await screen.findByText(/脚本创意生成失败（POST \/short-video\/ai\/generate-script-rich）：script llm down/)).toBeInTheDocument()
    expect(screen.getByDisplayValue('屏障修护')).toBeInTheDocument()
    expect(screen.getByTestId('hot-topic-idea-error')).toHaveAttribute('data-no-local-script-template', 'true')
  })

  it('ContentCalendarPage exposes content-calendar auto-generate contract', async () => {
    vi.mocked(shortvideoApi.contentCalendarMonthStats).mockResolvedValueOnce({ plannedCount: 2, publishedCount: 1, completionRate: 0.5 } as never)

    renderWithProviders(
      <MemoryRouter>
        <ContentCalendarPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '内容日历' })).toBeInTheDocument()
    expect(screen.getByText(/自动排期写入/)).toBeInTheDocument()
    expect(await screen.findByText('50%')).toBeInTheDocument()
    fireEvent.change(screen.getByLabelText('达人 Persona ID'), { target: { value: '12' } })
    fireEvent.click(screen.getByRole('button', { name: '自动生成本月排期' }))

    await waitFor(() => {
      expect(shortvideoApi.contentCalendarAutoGenerate).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 12,
      }))
    })
  })

  it('ContentCalendarPage tolerates wrapped publish time mocks', async () => {
    vi.mocked(shortvideoApi.seoSuggestPublishTime).mockResolvedValueOnce({
      data: { items: ['明晚 20:00'] },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <ContentCalendarPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('明晚 20:00')).toBeInTheDocument()
  })

  it('ContentCalendarPage saves manual calendar plans through real save endpoint', async () => {
    vi.mocked(shortvideoApi.contentCalendarMonthStats).mockResolvedValueOnce({ plannedCount: 0, publishedCount: 0, completionRate: 0 } as never)

    renderWithProviders(
      <MemoryRouter>
        <ContentCalendarPage />
      </MemoryRouter>,
    )

    fireEvent.change(screen.getByLabelText('达人 Persona ID'), { target: { value: '12' } })
    await waitFor(() => expect(screen.getByRole('button', { name: '新建计划' })).not.toBeDisabled())
    fireEvent.click(screen.getByRole('button', { name: '新建计划' }))
    const dialog = await screen.findByRole('dialog', { name: '新建内容计划' })
    expect(dialog).toBeInTheDocument()
    const inDialog = within(dialog)
    fireEvent.change(inDialog.getByLabelText(/计划标题/), { target: { value: '屏障修护测评' } })
    fireEvent.change(inDialog.getByLabelText(/计划日期/), { target: { value: '2026-05-22' } })
    fireEvent.change(inDialog.getByLabelText('项目 ID（可选）'), { target: { value: '99' } })
    fireEvent.change(inDialog.getByLabelText('账号 ID（可选）'), { target: { value: '7' } })
    fireEvent.change(inDialog.getByLabelText('标签'), { target: { value: '屏障修护,测评' } })
    fireEvent.change(inDialog.getByLabelText('计划说明'), { target: { value: '先拍产品质地，再讲敏感肌痛点。' } })
    fireEvent.click(inDialog.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(shortvideoApi.contentCalendarSave).toHaveBeenCalledWith(expect.objectContaining({
        personaId: 12,
        planDate: '2026-05-22',
        contentType: 'video',
        title: '屏障修护测评',
        projectId: 99,
        accountId: 7,
        priority: 2,
        tags: '屏障修护,测评',
        status: 0,
        brief: '先拍产品质地，再讲敏感肌痛点。',
      }))
    })
  }, 20000)

  it('RemakeTemplatePage aligns save payload with remakeType and structureTemplate', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RemakeTemplatePage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '二创模板' })).toBeInTheDocument()
    expect(screen.getByTestId('remake-template-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/remake-template/save'),
    )
    expect(screen.getByTestId('remake-template-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/remake-template/create-from-viral'),
    )
    expect(screen.getByTestId('remake-template-boundary-contract')).toHaveAttribute('data-no-template-generation-on-page', 'true')
    expect(screen.getByTestId('remake-template-grid-contract')).toHaveAttribute('data-server-filter-payload', 'true')
    expect(await screen.findByText('爆款结构')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '新建模板' }))
    fireEvent.change(screen.getByLabelText('模板名称'), { target: { value: '新模板' } })
    fireEvent.change(screen.getByLabelText('结构模板 JSON'), { target: { value: '{"hook":"前三秒"}' } })
    fireEvent.change(screen.getByLabelText('改编指引'), { target: { value: '保留情绪曲线' } })
    fireEvent.click(screen.getByRole('button', { name: '保存' }))

    await waitFor(() => {
      expect(shortvideoApi.remakeTemplateSave).toHaveBeenCalledWith({
        id: undefined,
        templateName: '新模板',
        remakeType: 'form_copy',
        structureTemplate: { hook: '前三秒' },
        adaptationGuide: '保留情绪曲线',
        emotionCurve: undefined,
        bgmStyle: undefined,
        durationRange: undefined,
      })
    })
  })

  it('RemakeTemplatePage renders normalized wrapped template rows', async () => {
    vi.mocked(shortvideoApi.remakeTemplateList).mockResolvedValueOnce({
      total: 1,
      list: [
        {
          id: 9,
          templateName: '包装二创模板',
          remakeType: 'scene_copy',
          adaptationGuide: '保留场景调度',
          usageCount: 5,
          avgViralScore: 92,
          structureTemplate: { hook: '开场反差' },
          createTime: '2026-05-22 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <RemakeTemplatePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('包装二创模板')).toBeInTheDocument()
    expect(screen.getByText('场景复刻')).toBeInTheDocument()
    expect(screen.getByText('保留场景调度')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '编辑' }))
    const dialog = await screen.findByRole('dialog', { name: '编辑模板' })
    expect(within(dialog).getByDisplayValue(/"hook": "开场反差"/)).toBeInTheDocument()
  })

  it('RemakeTemplatePage keeps dialog input and row when save or delete fails', async () => {
    vi.mocked(shortvideoApi.remakeTemplateSave).mockRejectedValueOnce(new Error('template duplicate') as never)
    vi.mocked(shortvideoApi.remakeTemplateDelete).mockRejectedValueOnce(new Error('template linked') as never)

    renderWithProviders(
      <MemoryRouter>
        <RemakeTemplatePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('爆款结构')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '新建模板' }))
    const dialog = await screen.findByRole('dialog', { name: '新建模板' })
    fireEvent.change(within(dialog).getByLabelText('模板名称'), { target: { value: '重复模板' } })
    fireEvent.change(within(dialog).getByLabelText('结构模板 JSON'), { target: { value: '{"hook":"保留"}' } })
    fireEvent.click(within(dialog).getByRole('button', { name: '保存' }))

    expect((await screen.findAllByText(/模板保存失败（POST \/short-video\/remake-template\/save）：template duplicate/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('remake-template-save-error')).toHaveAttribute('data-input-retained', 'true')
    expect(within(dialog).getByDisplayValue('重复模板')).toBeInTheDocument()

    fireEvent.click(within(dialog).getByRole('button', { name: '取消' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '新建模板' })).not.toBeInTheDocument()
    })
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect((await screen.findAllByText(/模板删除失败（POST \/short-video\/remake-template\/delete）：template linked/)).length).toBeGreaterThan(0)
    expect(screen.getByTestId('remake-template-operation-error')).toHaveAttribute('data-no-local-template-mutation', 'true')
    expect(screen.getByText(/上次删除失败：模板删除失败（POST \/short-video\/remake-template\/delete）：template linked/)).toBeInTheDocument()
    expect(screen.getByText('爆款结构')).toBeInTheDocument()
  }, 20000)

  it('CompetitorMonitorPage shows real monitor diagnostics and remove action', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CompetitorMonitorPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '竞品监控' })).toBeInTheDocument()
    expect(screen.getByTestId('competitor-monitor-page')).toHaveAttribute(
      'data-ready-endpoints',
      expect.stringContaining('/short-video/competitor/analyze'),
    )
    expect(screen.getByTestId('competitor-monitor-page')).toHaveAttribute(
      'data-unsupported-endpoints',
      expect.stringContaining('/short-video/competitor/local-analysis'),
    )
    expect(screen.getByTestId('competitor-monitor-boundary-contract')).toHaveAttribute('data-no-browser-direct-scrape', 'true')
    expect(screen.getByText(/分析依赖可用 AI 模型/)).toBeInTheDocument()
    expect(await screen.findByText('竞品 A')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    await waitFor(() => {
      expect(removeCompetitor).toHaveBeenCalledWith(3)
    })
  })

  it('CompetitorMonitorPage keeps add dialog input when add fails', async () => {
    vi.mocked(addCompetitor).mockRejectedValueOnce(new Error('duplicate account') as never)

    renderWithProviders(
      <MemoryRouter>
        <CompetitorMonitorPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('竞品 A')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '添加竞品' }))
    const addDialog = await screen.findByRole('dialog', { name: '添加竞品账号' })
    fireEvent.change(within(addDialog).getByRole('textbox', { name: /账号ID/ }), { target: { value: 'acc-2' } })
    fireEvent.change(within(addDialog).getByRole('textbox', { name: /账号名称/ }), { target: { value: '竞品 B' } })
    fireEvent.click(within(addDialog).getByRole('button', { name: '确认添加' }))

    expect(await screen.findByText(/添加竞品失败（POST \/short-video\/competitor\/add）：duplicate account/)).toBeInTheDocument()
    expect(screen.getByTestId('competitor-add-error')).toHaveAttribute('data-no-local-add-mutation', 'true')
    expect(screen.getByText(/弹窗输入会保留/)).toBeInTheDocument()
    expect(within(addDialog).getByDisplayValue('acc-2')).toBeInTheDocument()
    expect(within(addDialog).getByDisplayValue('竞品 B')).toBeInTheDocument()
  })

  it('CompetitorMonitorPage shows list, analyze, report and remove endpoint failures', async () => {
    vi.mocked(listCompetitors).mockRejectedValueOnce(new Error('list down') as never)

    renderWithProviders(
      <MemoryRouter>
        <CompetitorMonitorPage />
      </MemoryRouter>,
    )

    expect(await screen.findByText(/竞品列表加载失败（POST \/short-video\/competitor\/list）：list down/)).toBeInTheDocument()
    expect(screen.getByTestId('competitor-list-error')).toHaveAttribute('data-no-local-competitor-fallback', 'true')
    expect(screen.getByText(/不会填充本地竞品账号/)).toBeInTheDocument()

    vi.mocked(listCompetitors).mockResolvedValueOnce([{ id: 3, accountId: 'acc-1', accountName: '竞品 A', platform: 'douyin' }])
    fireEvent.click(screen.getByRole('button', { name: '重试' }))
    expect(await screen.findByText('竞品 A')).toBeInTheDocument()

    vi.mocked(analyzeCompetitor).mockRejectedValueOnce(new Error('llm down') as never)
    fireEvent.click(screen.getByRole('button', { name: '分析' }))
    expect(await screen.findByText(/竞品分析失败（POST \/short-video\/competitor\/analyze）：llm down/)).toBeInTheDocument()
    expect(screen.getByTestId('competitor-analysis-dialog')).toHaveAttribute('data-no-local-analysis-fallback', 'true')
    expect(screen.getByText(/不生成本地分析结论/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '关闭' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '竞品分析' })).not.toBeInTheDocument()
    })

    vi.mocked(generateWeeklyReport).mockRejectedValueOnce(new Error('report down') as never)
    fireEvent.click(screen.getByRole('button', { name: '周报' }))
    expect(await screen.findByText(/竞品周报生成失败（POST \/short-video\/competitor\/weekly-report）：report down/)).toBeInTheDocument()
    expect(screen.getByTestId('competitor-report-dialog')).toHaveAttribute('data-no-template-report-fallback', 'true')
    expect(screen.getByText(/不会展示模板周报/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '关闭' }))
    await waitFor(() => {
      expect(screen.queryByRole('dialog', { name: '竞品周报' })).not.toBeInTheDocument()
    })

    vi.mocked(removeCompetitor).mockRejectedValueOnce(new Error('linked snapshot') as never)
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))
    expect(await screen.findByText(/删除竞品失败（POST \/short-video\/competitor\/remove）：linked snapshot/)).toBeInTheDocument()
    expect(screen.getByTestId('competitor-action-error')).toHaveAttribute('data-no-local-delete-mutation', 'true')
    expect(screen.getByText(/不做前端本地删除/)).toBeInTheDocument()
    expect(screen.getByText('竞品 A')).toBeInTheDocument()
  })
})
