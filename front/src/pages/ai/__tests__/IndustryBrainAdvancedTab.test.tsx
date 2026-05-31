import { describe, it, expect, vi, beforeEach } from 'vitest'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import { IndustryBrainAdvancedTab } from '../IndustryBrainAdvancedTab'
import { brainApi } from '@/api/brain'

vi.mock('@/api/brain', () => ({
  brainApi: {
    trendsWithLifecycle: vi.fn(),
    hostPersonas: vi.fn(),
    trendsForHost: vi.fn(),
    riskWarn: vi.fn(),
    strategicPlan: vi.fn(),
    growthPath: vi.fn(),
    relationSuggestionsList: vi.fn(),
    relationSuggestionsUpdateStatus: vi.fn(),
    relationSuggestionsMaterialize: vi.fn(),
    causalCounterfactual: vi.fn(),
    causalExplainStrategy: vi.fn(),
    trendsDetectNew: vi.fn(),
    riskWarnBatch: vi.fn(),
    riskStats: vi.fn(),
    synergy: vi.fn(),
    styleConsistency: vi.fn(),
    ipGrowthStage: vi.fn(),
    ipMetricsBaseline: vi.fn(),
  },
}))

const toast = vi.fn()

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => toast,
}))

function renderTab() {
  return renderWithProviders(<IndustryBrainAdvancedTab />)
}

describe('IndustryBrainAdvancedTab', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(brainApi.trendsWithLifecycle).mockResolvedValue([
      {
        signal: {
          id: 'trend-1',
          title: '屏障修护',
          category: 'douyin',
          heatScore: 0.9,
          detectedAt: Date.now(),
          source: 'TianAPI',
          description: '敏感肌热词',
        },
        lifecycle: {
          phase: 'rising',
          momentum: 0.66,
          estimatedPeakHours: 12,
          currentHeat: 0.9,
          predictedPeakHeat: 0.96,
        },
        window: {
          windowType: 'hot',
          remainingHours: 8,
          advice: '今晚直播重点承接',
        },
      },
    ] as never)
    vi.mocked(brainApi.hostPersonas).mockResolvedValue([
      {
        id: 1,
        hostCode: 'host-a',
        hostName: '敏感肌顾问',
      },
    ] as never)
    vi.mocked(brainApi.trendsForHost).mockResolvedValue([
      {
        id: 'host-trend-1',
        title: '成分党热问',
        category: 'douyin',
        heatScore: 0.77,
        detectedAt: Date.now(),
        source: 'host-ranker',
        description: '',
      },
    ] as never)
    vi.mocked(brainApi.riskWarn).mockResolvedValue([
      {
        level: 3,
        type: 'absolute',
        message: '包含极限词',
        startOffset: 0,
        endOffset: 2,
        suggestion: '改为温和表达',
      },
    ] as never)
    vi.mocked(brainApi.strategicPlan).mockResolvedValue({
      accountId: 12,
      industryAnalysis: '护肤行业修护需求增长',
      competitorAnalysis: '竞品强化专家背书',
      opportunityPoints: ['成分教育'],
      swotScores: { strength: 0.8, weakness: 0.2 },
      contentMatrix: [{ type: '种草', strategy: '场景痛点切入', priority: 1 }],
      growthPhases: [{ phase: '冷启动', goal: '破千粉', strategies: ['每日直播'] }],
      diagnoses: [{ title: '定位清晰', detail: '继续强化敏感肌标签' }],
    } as never)
    vi.mocked(brainApi.growthPath).mockResolvedValue({
      summary: '先破万粉再扩品类',
      criticalSuccessFactors: ['稳定直播'],
      phases: [
        {
          phaseOrder: 1,
          phaseName: '起量期',
          targetFans: 10000,
          strategies: ['热点承接'],
          keyMetrics: ['互动率'],
          estimatedDuration: '30天',
        },
      ],
    } as never)
    vi.mocked(brainApi.relationSuggestionsList).mockResolvedValue([
      {
        id: 21,
        sourceEntityKey: '屏障修护',
        targetEntityKey: '神经酰胺',
        relationType: 'contains',
        confidence: 0.91,
        status: 'pending',
      },
    ] as never)
    vi.mocked(brainApi.relationSuggestionsUpdateStatus).mockResolvedValue(null as never)
    vi.mocked(brainApi.relationSuggestionsMaterialize).mockResolvedValue({ inserted: 1 } as never)
    vi.mocked(brainApi.causalCounterfactual).mockResolvedValue({
      expectedConversionRateBefore: 0.21,
      expectedConversionRateAfter: 0.31,
      conversionRateDelta: 0.1,
      impactPath: ['topic', 'conversion'],
      suggestion: '增加成分证明',
    } as never)
    vi.mocked(brainApi.causalExplainStrategy).mockResolvedValue({
      strategyId: 'combo_A',
      explanation: '组合策略适合晚场促转。',
    } as never)
    vi.mocked(brainApi.trendsDetectNew).mockResolvedValue([
      {
        id: 'new-1',
        title: '早C晚A',
        category: 'network',
        heatScore: 0.7,
        detectedAt: Date.now(),
        source: 'detector',
        description: '',
      },
    ] as never)
    vi.mocked(brainApi.riskWarnBatch).mockResolvedValue([
      {
        level: 2,
        type: 'claim',
        message: '功效表达需谨慎',
        startOffset: 0,
        endOffset: 2,
        suggestion: '补充限定语',
      },
    ] as never)
    vi.mocked(brainApi.riskStats).mockResolvedValue({
      totalChecks: 100,
      violationCount: 8,
      accuracyEstimate: 0.93,
    } as never)
    vi.mocked(brainApi.synergy).mockResolvedValue({
      summary: '五主播协同覆盖不同人群',
    } as never)
    vi.mocked(brainApi.styleConsistency).mockResolvedValue({
      prompt: '保持温和专业',
      score: 0.88,
    } as never)
    vi.mocked(brainApi.ipGrowthStage).mockResolvedValue({
      stage: 'growth',
      advice: '强化栏目化内容',
    } as never)
    vi.mocked(brainApi.ipMetricsBaseline).mockResolvedValue({
      targetInteractionRate: 0.12,
    } as never)
  })

  it('loads lifecycle trends and host-ranked trends', async () => {
    renderTab()

    const root = screen.getByTestId('industry-brain-advanced-tab')
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/brain/trends/with-lifecycle'))
    expect(root).toHaveAttribute('data-ready-endpoints', expect.stringContaining('/ai/brain/ip-metrics-baseline'))
    expect(root).toHaveAttribute('data-unsupported-endpoints', expect.stringContaining('/ai/brain/advanced/mock'))
    expect(root).toHaveAttribute('data-no-static-strategy', 'true')
    expect(screen.getByTestId('industry-brain-advanced-sub-tab-host')).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-advanced-lifecycle-panel')).toHaveAttribute('data-ready-endpoint', '/ai/brain/trends/with-lifecycle')
    await waitFor(() => {
      expect(brainApi.trendsWithLifecycle).toHaveBeenCalledWith({ category: undefined, limit: 25 })
    })
    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-advanced-lifecycle-table')).toBeInTheDocument()
    expect(screen.getByText(/rising/)).toBeInTheDocument()
    expect(screen.getByText('今晚直播重点承接')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '主播趋势' }))
    expect(screen.getByTestId('industry-brain-advanced-host-trends-panel')).toHaveAttribute('data-no-local-trends', 'true')
    await waitFor(() => {
      expect(brainApi.hostPersonas).toHaveBeenCalled()
      expect(brainApi.trendsForHost).toHaveBeenCalledWith({ hostCode: undefined, limit: 20 })
    })
    expect(await screen.findByText('成分党热问')).toBeInTheDocument()
    expect(screen.getByText(/热度 0.77/)).toBeInTheDocument()
  })

  it('runs risk detection and strategy growth generation', async () => {
    renderTab()

    fireEvent.click(screen.getByRole('tab', { name: '风险检测' }))
    expect(screen.getByTestId('industry-brain-advanced-risk-panel')).toHaveAttribute('data-no-static-risk', 'true')
    fireEvent.change(screen.getByLabelText('待检测话术/文案'), {
      target: { value: '这是最有效的修护方案' },
    })
    fireEvent.click(screen.getByRole('button', { name: '检测风险' }))
    await waitFor(() => {
      expect(brainApi.riskWarn).toHaveBeenCalledWith('这是最有效的修护方案')
    })
    expect(await screen.findByText(/\[absolute\] 包含极限词/)).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-advanced-risk-result')).toHaveAttribute('data-no-static-risk', 'true')

    fireEvent.click(screen.getByRole('tab', { name: '战略与增长' }))
    expect(screen.getByTestId('industry-brain-advanced-strategy-growth-panel')).toHaveAttribute('data-no-static-strategy', 'true')
    fireEvent.change(screen.getByRole('textbox', { name: '抖音账号 ID（可选）' }), {
      target: { value: '12' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: '战略目标（逗号分隔）' }), {
      target: { value: '万粉突破,复购提升' },
    })
    fireEvent.click(screen.getByRole('button', { name: '生成战略规划' }))
    await waitFor(() => {
      expect(brainApi.strategicPlan).toHaveBeenCalledWith({
        accountId: 12,
        goals: ['万粉突破', '复购提升'],
      })
      expect(toast).toHaveBeenCalledWith('战略报告已生成', 'success')
    })
    expect(await screen.findByText('护肤行业修护需求增长')).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-advanced-strategy-result')).toHaveAttribute('data-no-static-strategy', 'true')
    expect(screen.getByText('成分教育')).toBeInTheDocument()

    fireEvent.change(screen.getByRole('textbox', { name: '目标粉丝数' }), {
      target: { value: '50000' },
    })
    fireEvent.click(screen.getByRole('button', { name: '生成增长路径' }))
    await waitFor(() => {
      expect(brainApi.growthPath).toHaveBeenCalledWith({
        accountId: 12,
        targetFans: 50000,
        currentState: {},
      })
      expect(toast).toHaveBeenCalledWith('增长路径已生成', 'success')
    })
    expect(await screen.findByText('先破万粉再扩品类')).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-advanced-growth-result')).toHaveAttribute('data-no-static-strategy', 'true')
    expect(screen.getByText(/起量期/)).toBeInTheDocument()
  })

  it('materializes and reviews graph relation suggestions', async () => {
    renderTab()

    fireEvent.click(screen.getByRole('tab', { name: '图谱运维' }))
    expect(screen.getByTestId('industry-brain-advanced-graph-ops-panel')).toHaveAttribute('data-no-local-graph-mutation', 'true')
    await waitFor(() => {
      expect(brainApi.relationSuggestionsList).toHaveBeenCalledWith({ status: 'pending', limit: 30 })
    })
    expect(await screen.findByText('屏障修护')).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-advanced-graph-list-table')).toHaveAttribute('data-no-local-graph-mutation', 'true')
    expect(screen.getByText('神经酰胺')).toBeInTheDocument()

    fireEvent.change(screen.getByRole('textbox', { name: '源实体 key' }), {
      target: { value: '敏感肌' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: '目标实体 key' }), {
      target: { value: '屏障修护' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: '关系类型' }), {
      target: { value: 'related_to' },
    })
    fireEvent.click(screen.getByRole('button', { name: '写入队列' }))
    await waitFor(() => {
      expect(brainApi.relationSuggestionsMaterialize).toHaveBeenCalledWith([
        {
          sourceEntityKey: '敏感肌',
          targetEntityKey: '屏障修护',
          relationType: 'related_to',
        },
      ])
      expect(toast).toHaveBeenCalledWith('已写入建议队列 1 条', 'success')
    })

    fireEvent.click(screen.getByRole('button', { name: '通过' }))
    await waitFor(() => {
      expect(brainApi.relationSuggestionsUpdateStatus).toHaveBeenCalledWith(21, 'approved')
      expect(toast).toHaveBeenCalledWith('审核状态已更新', 'success')
    })

    fireEvent.click(screen.getByRole('button', { name: '驳回' }))
    await waitFor(() => {
      expect(brainApi.relationSuggestionsUpdateStatus).toHaveBeenCalledWith(21, 'rejected')
    })
  })

  it('runs extension APIs for counterfactual, trends, compliance, synergy, style, and IP baselines', async () => {
    renderTab()

    fireEvent.click(screen.getByRole('tab', { name: '扩展 API' }))
    expect(screen.getByTestId('industry-brain-advanced-extensions-panel')).toHaveAttribute('data-no-static-extensions', 'true')
    expect(screen.getByTestId('industry-brain-advanced-counterfactual-surface')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('currentState JSON'), {
      target: { value: '{"scriptType":"种草"}' },
    })
    fireEvent.change(screen.getByLabelText('intervention JSON'), {
      target: { value: '{"persona":"专家"}' },
    })
    fireEvent.click(screen.getByRole('button', { name: '执行' }))
    await waitFor(() => {
      expect(brainApi.causalCounterfactual).toHaveBeenCalledWith({
        currentState: { scriptType: '种草' },
        intervention: { persona: '专家' },
      })
      expect(toast).toHaveBeenCalledWith('反事实推理完成', 'success')
    })
    expect(await screen.findByText(/conversionRateDelta/)).toBeInTheDocument()

    fireEvent.change(screen.getByRole('textbox', { name: 'strategyId' }), {
      target: { value: 'combo_B' },
    })
    fireEvent.click(screen.getByRole('button', { name: '解释' }))
    await waitFor(() => {
      expect(brainApi.causalExplainStrategy).toHaveBeenCalledWith({ strategyId: 'combo_B', context: {} })
      expect(toast).toHaveBeenCalledWith('策略解释已生成', 'success')
    })
    expect(await screen.findByText('组合策略适合晚场促转。')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '拉取 detect-new' }))
    await waitFor(() => {
      expect(brainApi.trendsDetectNew).toHaveBeenCalled()
      expect(toast).toHaveBeenCalledWith('检测到 1 条趋势', 'success')
    })
    expect(await screen.findByText('早C晚A')).toBeInTheDocument()

    fireEvent.change(screen.getByLabelText('每行一条话术'), {
      target: { value: '最有效\n根治' },
    })
    fireEvent.click(screen.getByRole('button', { name: '检测' }))
    await waitFor(() => {
      expect(brainApi.riskWarnBatch).toHaveBeenCalledWith(['最有效', '根治'])
      expect(toast).toHaveBeenCalledWith('批量检测完成', 'success')
    })
    expect(await screen.findByText('功效表达需谨慎')).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '拉取 risk/stats' }))
    await waitFor(() => {
      expect(brainApi.riskStats).toHaveBeenCalled()
      expect(toast).toHaveBeenCalledWith('已拉取风险统计', 'success')
    })
    expect(await screen.findByText(/总检 100/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '拉取 synergy' }))
    await waitFor(() => {
      expect(brainApi.synergy).toHaveBeenCalled()
      expect(toast).toHaveBeenCalledWith('协同摘要已更新', 'success')
    })
    expect(await screen.findByText(/五主播协同覆盖不同人群/)).toBeInTheDocument()

    fireEvent.change(screen.getByRole('textbox', { name: 'hostCode' }), {
      target: { value: 'host-a' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: 'content' }), {
      target: { value: '温和专业的直播话术' },
    })
    fireEvent.click(screen.getByRole('button', { name: '计算' }))
    await waitFor(() => {
      expect(brainApi.styleConsistency).toHaveBeenCalledWith({
        hostCode: 'host-a',
        content: '温和专业的直播话术',
      })
      expect(toast).toHaveBeenCalledWith('风格一致性已计算', 'success')
    })
    expect(await screen.findByText('score=0.88')).toBeInTheDocument()

    fireEvent.change(screen.getByRole('textbox', { name: '粉丝数' }), {
      target: { value: '36000' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: '运营月数' }), {
      target: { value: '9' },
    })
    fireEvent.click(screen.getByRole('button', { name: '增长阶段' }))
    await waitFor(() => {
      expect(brainApi.ipGrowthStage).toHaveBeenCalledWith({
        ipType: 'phenomenal',
        followerCount: 36000,
        operatingMonths: 9,
      })
      expect(toast).toHaveBeenCalledWith('IP 阶段已计算', 'success')
    })
    expect(await screen.findByText(/强化栏目化内容/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('button', { name: '指标基线' }))
    await waitFor(() => {
      expect(brainApi.ipMetricsBaseline).toHaveBeenCalledWith({ ipType: 'phenomenal' })
      expect(toast).toHaveBeenCalledWith('基线已拉取', 'success')
    })
    expect(await screen.findByText(/targetInteractionRate/)).toBeInTheDocument()
    expect(screen.getByTestId('industry-brain-advanced-ip-surface')).toBeInTheDocument()
  }, 20000)

  it('shows explicit source errors for advanced panels instead of silent empty mocks', async () => {
    vi.mocked(brainApi.trendsWithLifecycle).mockRejectedValueOnce(new Error('trend lifecycle down') as never)
    vi.mocked(brainApi.hostPersonas).mockRejectedValueOnce(new Error('persona down') as never)
    vi.mocked(brainApi.trendsForHost).mockRejectedValueOnce(new Error('host trend down') as never)
    vi.mocked(brainApi.riskWarn).mockRejectedValueOnce(new Error('risk service down') as never)
    vi.mocked(brainApi.strategicPlan).mockRejectedValueOnce(new Error('plan down') as never)
    vi.mocked(brainApi.growthPath).mockRejectedValueOnce(new Error('growth down') as never)
    vi.mocked(brainApi.relationSuggestionsList).mockRejectedValueOnce(new Error('relation down') as never)
    vi.mocked(brainApi.relationSuggestionsMaterialize).mockRejectedValueOnce(new Error('materialize down') as never)
    vi.mocked(brainApi.relationSuggestionsUpdateStatus).mockRejectedValueOnce(new Error('review down') as never)
    vi.mocked(brainApi.causalCounterfactual).mockRejectedValueOnce(new Error('counterfactual down') as never)

    renderTab()

    expect(await screen.findByTestId('industry-brain-advanced-lifecycle-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/趋势生命周期加载失败（\/ai\/brain\/trends\/with-lifecycle）：trend lifecycle down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '主播趋势' }))
    expect(await screen.findByText(/主播人设列表加载失败（\/ai\/brain\/host-personas）/)).toBeInTheDocument()
    expect(await screen.findByTestId('industry-brain-advanced-host-trends-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/主播趋势加载失败（\/ai\/brain\/trends\/for-host）：host trend down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '风险检测' }))
    fireEvent.change(screen.getByLabelText('待检测话术/文案'), {
      target: { value: '风险话术' },
    })
    fireEvent.click(screen.getByRole('button', { name: '检测风险' }))
    expect(await screen.findByTestId('industry-brain-advanced-risk-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/风险检测失败（\/ai\/brain\/risk\/warn）：risk service down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '战略与增长' }))
    fireEvent.click(screen.getByRole('button', { name: '生成战略规划' }))
    expect(await screen.findByTestId('industry-brain-advanced-strategy-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/战略规划失败（\/ai\/brain\/strategic\/plan）：plan down/)).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '生成增长路径' }))
    expect(await screen.findByTestId('industry-brain-advanced-growth-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/增长路径失败（\/ai\/brain\/growth-path）：growth down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '图谱运维' }))
    expect(await screen.findByTestId('industry-brain-advanced-graph-list-error')).toHaveAttribute('data-no-local-graph-mutation', 'true')
    expect(screen.getByText(/关系建议加载失败（\/ai\/brain\/knowledge-graph\/relation-suggestions\/list）：relation down/)).toBeInTheDocument()
    fireEvent.change(screen.getByRole('textbox', { name: '源实体 key' }), {
      target: { value: '敏感肌' },
    })
    fireEvent.change(screen.getByRole('textbox', { name: '目标实体 key' }), {
      target: { value: '屏障修护' },
    })
    fireEvent.click(screen.getByRole('button', { name: '写入队列' }))
    expect(await screen.findByTestId('industry-brain-advanced-graph-materialize-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/写入关系建议失败（\/ai\/brain\/knowledge-graph\/relation-suggestions\/materialize）：materialize down/)).toBeInTheDocument()
    vi.mocked(brainApi.relationSuggestionsList).mockResolvedValueOnce([
      {
        id: 22,
        sourceEntityKey: '敏感肌',
        targetEntityKey: '屏障修护',
        relationType: 'related_to',
        confidence: 0.8,
        status: 'pending',
      },
    ] as never)
    fireEvent.click(screen.getByRole('button', { name: '刷新待审核关系' }))
    expect(await screen.findByText('敏感肌')).toBeInTheDocument()
    fireEvent.click(screen.getByRole('button', { name: '通过' }))
    expect(await screen.findByTestId('industry-brain-advanced-graph-status-error')).toHaveAttribute('data-no-local-graph-mutation', 'true')
    expect(screen.getByText(/审核关系建议失败（\/ai\/brain\/knowledge-graph\/relation-suggestions\/update-status）：review down/)).toBeInTheDocument()

    fireEvent.click(screen.getByRole('tab', { name: '扩展 API' }))
    fireEvent.click(screen.getByRole('button', { name: '执行' }))
    expect(await screen.findByTestId('industry-brain-advanced-extensions-error')).toHaveAttribute('data-input-retained', 'true')
    expect(screen.getByText(/反事实推理失败（\/ai\/brain\/causal\/counterfactual）：counterfactual down/)).toBeInTheDocument()
  }, 20000)
})
