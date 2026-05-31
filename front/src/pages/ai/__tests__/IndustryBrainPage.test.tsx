import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { AppThemeProvider } from '@/theme/AppThemeProvider'
import IndustryBrainPage from '../IndustryBrainPage'
import { brainApi } from '@/api/brain'
import { useIndustryBrainStore } from '@/stores/industryBrainStore'

vi.mock('echarts-for-react', () => ({
  default: ({ option }: { option: unknown }) => (
    <div data-testid="mock-echarts">{JSON.stringify(option)}</div>
  ),
}))

vi.mock('../IndustryBrainAdvancedTab', () => ({
  IndustryBrainAdvancedTab: () => <div>高级工作台 Mock</div>,
}))

vi.mock('@/api/brain', () => ({
  brainApi: {
    trendsCurrent: vi.fn(),
    hostPersonas: vi.fn(),
    causalInfer: vi.fn(),
    userProfile: vi.fn(),
    knowledgeGraphSubgraph: vi.fn(),
    knowledgeGraphQuery: vi.fn(),
    graphRagContext: vi.fn(),
    industryInsights: vi.fn(),
  },
}))

const toast = vi.fn()
const writeText = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderPage() {
  return renderWithProviders(
    <MemoryRouter>
      <IndustryBrainPage />
    </MemoryRouter>,
  )
}

function renderPageWithTheme() {
  return renderWithProviders(
    <AppThemeProvider>
      <MemoryRouter>
        <IndustryBrainPage />
      </MemoryRouter>
    </AppThemeProvider>,
  )
}

describe('IndustryBrainPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    window.localStorage.removeItem('dy-theme-mode')
    useIndustryBrainStore.getState().setPendingHotKeywords([])
    Object.assign(navigator, {
      clipboard: { writeText },
    })

    vi.mocked(brainApi.trendsCurrent).mockResolvedValue([
      {
        id: 'trend-1',
        title: '屏障修护',
        category: 'douyin',
        heatScore: 0.92,
        detectedAt: Date.now(),
        source: 'TianAPI',
        description: '敏感肌相关热词',
      },
      {
        id: 'trend-2',
        title: '熬夜急救',
        category: 'weibo',
        heatScore: 0.71,
        detectedAt: Date.now(),
        source: 'Weibo',
        description: '夜间护肤热词',
      },
    ] as never)
    vi.mocked(brainApi.hostPersonas).mockResolvedValue([
      {
        id: 1,
        hostCode: 'host-a',
        hostName: '敏感肌顾问',
        positioning: '专业护肤',
      },
    ] as never)
    vi.mocked(brainApi.causalInfer).mockResolvedValue({
      expectedConversionRate: 0.37,
      keyFactors: ['强痛点', '专业背书'],
      riskPoints: ['夸大功效'],
      explanation: '晚场专业种草转化更稳。',
    } as never)
    vi.mocked(brainApi.userProfile).mockResolvedValue({
      userId: 88,
      contentPreferences: {
        种草: 0.82,
        逼单: 0.46,
        _meta: 1,
      },
      expressionStyleTags: ['温和', '专业'],
      learningProgress: 0.64,
      interactionPattern: { lastAction: 'edit-script' },
      lastUpdatedAt: new Date('2026-05-15T08:00:00Z').getTime(),
    } as never)
    vi.mocked(brainApi.knowledgeGraphSubgraph).mockResolvedValue({
      nodes: [
        { id: 'n1', name: '屏障修护', type: 'topic', confidence: 9 },
        { id: 'n2', label: '神经酰胺', type: 'ingredient', weight: 6 },
      ],
      edges: [
        { sourceNodeId: 'n1', targetNodeId: 'n2', relation: 'contains', confidence: 0.8 },
      ],
    } as never)
    vi.mocked(brainApi.knowledgeGraphQuery).mockResolvedValue([
      { id: 'n1', name: '屏障修护', type: 'topic' },
      { id: 'n2', name: '神经酰胺', type: 'ingredient' },
    ] as never)
    vi.mocked(brainApi.graphRagContext).mockResolvedValue({
      context: '屏障修护 -> 神经酰胺，可用于敏感肌话术证据链。',
      available: true,
      hops: 2,
    } as never)
    vi.mocked(brainApi.industryInsights).mockResolvedValue({
      行业分类: '护肤',
      趋势热点: ['屏障修护', '早C晚A'],
      趋势来源: ['douyin', 'network'],
      近7天竞品洞察: ['竞品强调温和修护'],
      差异化建议: '突出成分证据链',
      用户偏好焦点: ['敏感肌', '温和'],
      优先动作: ['更新直播热词', '强化合规表达'],
      数据口径: '近7天趋势与图谱融合',
      额外备注: '测试字段',
    } as never)
  })

  it('renders trends, supports local filtering, copy, and live hot keyword queueing', async () => {
    renderPage()

    expect(screen.getByRole('heading', { name: '行业大脑' })).toBeInTheDocument()
    const page = screen.getByTestId('industry-brain-page')
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/brain/trends/current'))
    expect(page).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/brain/industry/insights'))
    expect(page).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/ai/brain/mock-trends'))
    expect(page).toHaveAttribute('data-no-local-trends', 'true')
    expect(page).toHaveAttribute('data-no-static-insights', 'true')
    expect(screen.getByTestId('industry-brain-boundary-contract')).toHaveAttribute('data-no-local-fallback', 'true')
    expect(screen.getByTestId('industry-brain-tab-host')).toBeInTheDocument()
    await waitFor(() => {
      expect(brainApi.trendsCurrent).toHaveBeenCalledWith({ category: undefined, limit: 80 })
    })
    expect(screen.getByTestId('industry-brain-trends-panel')).toHaveAttribute('data-ready-endpoint', '/ai/brain/trends/current')
    expect(screen.getByTestId('industry-brain-trends-list')).toHaveAttribute('data-no-local-trends', 'true')
    expect(await screen.findByText('屏障修护')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('关键词包含（本地筛选）'), {
      target: { value: '熬夜' },
    })
    expect(screen.queryByText('屏障修护')).not.toBeInTheDocument()
    expect(screen.getByText('熬夜急救')).toBeInTheDocument()

    fireEvent.click(screen.getByLabelText('复制'))
    expect(writeText).toHaveBeenCalledWith('熬夜急救')
    expect(toast).toHaveBeenCalledWith('已复制「熬夜急救」', 'success')

    fireEvent.click(screen.getByLabelText('加入直播话术热词'))
    expect(useIndustryBrainStore.getState().pendingHotKeywords).toEqual(['熬夜急救'])
    expect(toast).toHaveBeenCalledWith('已加入直播话术热词队列（话术页将自动合并）', 'success')

    fireEvent.click(screen.getByLabelText('加入直播话术热词'))
    expect(toast).toHaveBeenCalledWith('该词已在待注入列表中', 'info')
  })

  it('runs causal inference and loads operator cognitive profile', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '因果推断' }))
    expect(screen.getByTestId('industry-brain-causal-panel')).toHaveAttribute('data-no-local-infer', 'true')
    await waitFor(() => {
      expect(brainApi.hostPersonas).toHaveBeenCalled()
    })
    fireEvent.change(screen.getByRole('textbox', { name: '人设关键词' }), {
      target: { value: '成分党顾问' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: '产品类型' }), {
      target: { value: '精华' },
    })
    fireEvent.click(screen.getByRole('button', { name: '推断转化与风险' }))
    await waitFor(() => {
      expect(brainApi.causalInfer).toHaveBeenCalledWith(
        expect.objectContaining({
          scriptType: '种草',
          persona: '成分党顾问',
          productType: '精华',
          timeSlot: '晚场',
        }),
      )
    })
    expect(await screen.findByText('预期转化率 37%')).toBeInTheDocument()
    expect(screen.getByText('强痛点')).toBeInTheDocument()
    expect(screen.getByText('夸大功效')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '用户画像' }))
    expect(screen.getByTestId('industry-brain-profile-panel')).toHaveAttribute('data-ready-endpoint', '/ai/brain/user-profile')
    await waitFor(() => {
      expect(brainApi.userProfile).toHaveBeenCalledWith({})
    })
    expect(await screen.findByText('0.64')).toBeInTheDocument()
    expect(screen.getByText('种草')).toBeInTheDocument()
    expect(screen.getByText('0.82')).toBeInTheDocument()
    expect(screen.getByText('温和')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('查看对象用户 ID（可选，默认当前登录用户）'), {
      target: { value: '99' },
    })
    fireEvent.click(screen.getByRole('button', { name: '刷新' }))
    await waitFor(() => {
      expect(brainApi.userProfile).toHaveBeenLastCalledWith({ accountId: 99 })
    })
  })

  it('loads graph data, filters nodes, queries entities, and retrieves GraphRAG context', async () => {
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '知识图谱' }))
    expect(screen.getByTestId('industry-brain-graph-panel')).toHaveAttribute('data-no-static-graph', 'true')
    await waitFor(() => {
      expect(brainApi.knowledgeGraphSubgraph).toHaveBeenCalledWith({ query: '美妆 直播', limit: 80 })
    })
    expect(await screen.findByText(/显示 2 个节点/)).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('节点筛选'), {
      target: { value: '屏障' },
    })
    fireEvent.click(screen.getByRole('button', { name: '筛选' }))
    expect(screen.getByText(/已筛选：屏障/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '检索实体' }))
    await waitFor(() => {
      expect(brainApi.knowledgeGraphQuery).toHaveBeenCalledWith({
        entityType: 'topic',
        keyword: '美妆 直播',
        limit: 24,
      })
      expect(toast).toHaveBeenCalledWith('检索到 2 条实体', 'success')
    })
    expect(await screen.findByText(/神经酰胺/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '拉取上下文' }))
    await waitFor(() => {
      expect(brainApi.graphRagContext).toHaveBeenCalledWith({ query: '美妆 直播', limit: 24 })
      expect(toast).toHaveBeenCalledWith('GraphRAG 上下文已更新', 'success')
    })
    expect(await screen.findByText(/可用于敏感肌话术证据链/)).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-graphrag-surface')).toBeInTheDocument()
    expect(screen.getByText('max-hops: 2')).toBeInTheDocument()
  })

  it('loads industry insights and enqueues unique insight hot topics', async () => {
    useIndustryBrainStore.getState().setPendingHotKeywords(['屏障修护'])
    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '行业洞察' }))
    expect(screen.getByTestId('industry-brain-insights-panel')).toHaveAttribute('data-no-static-insights', 'true')
    await waitFor(() => {
      expect(brainApi.industryInsights).toHaveBeenCalledWith('护肤')
    })
    expect(await screen.findByText('突出成分证据链')).toBeInTheDocument()
    expect(screen.getByText('竞品强调温和修护')).toBeInTheDocument()
    expect(screen.getByText('其它字段（1）')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '趋势热点入队' }))
    expect(screen.getByTestId('industry-brain-hotword-queue-action')).toBeInTheDocument()
    expect(useIndustryBrainStore.getState().pendingHotKeywords).toEqual(['屏障修护', '早C晚A'])
    expect(toast).toHaveBeenCalledWith('已将 1 个热点词加入直播话术队列', 'success')
  })

  it('shows source-specific downgrade errors for causal, graph query, and GraphRAG failures', async () => {
    vi.mocked(brainApi.causalInfer).mockRejectedValueOnce(new Error('causal engine disabled') as never)
    vi.mocked(brainApi.knowledgeGraphQuery).mockRejectedValueOnce(new Error('neo4j offline') as never)
    vi.mocked(brainApi.graphRagContext).mockRejectedValueOnce(new Error('graph rag offline') as never)

    renderPage()

    fireEvent.click(screen.getByRole('tab', { name: '因果推断' }))
    fireEvent.click(screen.getByRole('button', { name: '推断转化与风险' }))
    expect(await screen.findByTestId('industry-brain-causal-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/因果推断失败（POST \/ai\/brain\/causal\/infer）：causal engine disabled/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '知识图谱' }))
    await waitFor(() => {
      expect(brainApi.knowledgeGraphSubgraph).toHaveBeenCalledWith({ query: '美妆 直播', limit: 80 })
    })
    fireEvent.click(screen.getByRole('button', { name: '检索实体' }))
    expect(await screen.findByTestId('industry-brain-graph-entity-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/实体检索失败（POST \/ai\/brain\/knowledge-graph\/query）：neo4j offline/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '拉取上下文' }))
    expect(await screen.findByTestId('industry-brain-graphrag-error')).toHaveAttribute('data-no-static-graph', 'true')
    expect(screen.getByText(/GraphRAG 上下文拉取失败（POST \/ai\/brain\/knowledge-graph\/graphrag-context）：graph rag offline/)).toBeInTheDocument()
  })

  it('shows POST endpoint for trends, host personas, profile, subgraph, and insights failures', async () => {
    vi.mocked(brainApi.trendsCurrent).mockRejectedValueOnce(new Error('trend down') as never)
    vi.mocked(brainApi.hostPersonas).mockRejectedValueOnce(new Error('persona down') as never)
    vi.mocked(brainApi.userProfile).mockRejectedValueOnce(new Error('profile down') as never)
    vi.mocked(brainApi.knowledgeGraphSubgraph).mockRejectedValueOnce(new Error('graph down') as never)
    vi.mocked(brainApi.industryInsights).mockRejectedValueOnce(new Error('insights down') as never)

    renderPage()

    expect(await screen.findByTestId('industry-brain-trends-error')).toHaveAttribute('data-no-local-trends', 'true')
    expect(screen.getByText(/趋势数据加载失败（POST \/ai\/brain\/trends\/current）：trend down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '因果推断' }))
    expect(await screen.findByText(/主播画像列表不可用（POST \/ai\/brain\/host-personas）/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '用户画像' }))
    expect(await screen.findByTestId('industry-brain-profile-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/画像加载失败（POST \/ai\/brain\/user-profile）：profile down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '知识图谱' }))
    expect(await screen.findByTestId('industry-brain-graph-subgraph-error')).toHaveAttribute('data-no-static-graph', 'true')
    expect(screen.getByText(/子图加载失败（POST \/ai\/brain\/knowledge-graph\/subgraph-json）：graph down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '行业洞察' }))
    expect(await screen.findByTestId('industry-brain-insights-error')).toHaveAttribute('data-no-static-insights', 'true')
    expect(screen.getByText(/行业洞察加载失败（POST \/ai\/brain\/industry\/insights）：insights down/)).toBeInTheDocument()
  })

  it('uses a theme-aware GraphRAG context surface in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme()

    fireEvent.click(screen.getByRole('tab', { name: '知识图谱' }))
    await waitFor(() => {
      expect(brainApi.knowledgeGraphSubgraph).toHaveBeenCalledWith({ query: '美妆 直播', limit: 80 })
    })
    expect(await screen.findByText(/显示 2 个节点/)).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-graphrag-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })

    fireEvent.click(screen.getByRole('button', { name: '拉取上下文' }))
    await waitFor(() => {
      expect(brainApi.graphRagContext).toHaveBeenCalledWith({ query: '美妆 直播', limit: 24 })
    })
    expect(await screen.findByText(/可用于敏感肌话术证据链/)).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-graphrag-surface')).toHaveStyle({
      backgroundColor: 'rgb(18, 18, 18)',
    })
  })

  it('uses theme token for trend TOP10 chart in dark mode', async () => {
    window.localStorage.setItem('dy-theme-mode', 'dark')
    renderPageWithTheme()

    await waitFor(() => {
      expect(brainApi.trendsCurrent).toHaveBeenCalledWith({ category: undefined, limit: 80 })
    })
    expect(await screen.findByText('屏障修护')).toBeInTheDocument()

    const trendChart = screen.getByTestId('industry-brain-trend-top-chart-surface')
    expect(trendChart.textContent).not.toContain('#1976d2')
    expect(trendChart.textContent).toContain('#e3f2fd')
    expect(trendChart).toHaveAttribute('data-chart-color', '#e3f2fd')
  })
})
