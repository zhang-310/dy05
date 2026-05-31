import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { brainApi } from '../brain'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('brain API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
    mockPost.mockReset()
  })

  it('normalizes wrapped trend, lifecycle, and host persona responses', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [
            {
              trendId: 't1',
              keyword: '屏障修护',
              platform: 'douyin',
              hotScore: '92%',
              detectTime: '2026-05-22T10:00:00+08:00',
              origin: 'TianAPI',
              desc: '敏感肌趋势',
            },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          items: [
            {
              trend: { id: 'life-1', title: '早C晚A', heat: '0.71', category: 'network' },
              lifeCycle: { stage: 'rising', momentum: '0.66', peakHours: '12' },
              hotWindow: { type: 'hot', hoursLeft: '8', suggestion: '今晚承接' },
            },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          rows: [
            { personaId: '7', code: 'host-a', name: '敏感肌顾问', position: '专业护肤', flowPhase: '2' },
          ],
        },
      })

    await expect(brainApi.trendsCurrent({ category: 'douyin', limit: 1 })).resolves.toEqual([
      expect.objectContaining({
        id: 't1',
        title: '屏障修护',
        heatScore: 92,
        source: 'TianAPI',
      }),
    ])
    await expect(brainApi.trendsWithLifecycle({ limit: 1 })).resolves.toEqual([
      expect.objectContaining({
        signal: expect.objectContaining({ title: '早C晚A', heatScore: 0.71 }),
        lifecycle: expect.objectContaining({ phase: 'rising', momentum: 0.66, estimatedPeakHours: 12 }),
        window: expect.objectContaining({ windowType: 'hot', remainingHours: 8, advice: '今晚承接' }),
      }),
    ])
    await expect(brainApi.hostPersonas()).resolves.toEqual([
      expect.objectContaining({ id: 7, hostCode: 'host-a', hostName: '敏感肌顾问', flowPhase: 2 }),
    ])
  })

  it('normalizes graph, GraphRAG, and relation suggestion wrappers', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          nodes: [{ nodeId: 1, name: '屏障修护', entityType: 'topic', confidence: '0.9' }],
          links: [{ sourceNodeId: 1, targetNodeId: 2, relationType: 'contains', confidence: '0.8' }],
          contradictions: [{ id: 'c1' }],
        },
      })
      .mockResolvedValueOnce({ data: { rows: [{ id: 1, name: '屏障修护' }] } })
      .mockResolvedValueOnce({ data: { text: '屏障修护 -> 神经酰胺', available: 'true', maxHops: '2' } })
      .mockResolvedValueOnce({
        data: {
          records: [
            {
              suggestionId: '21',
              source: '屏障修护',
              target: '神经酰胺',
              relation: 'contains',
              confidence: '91%',
              status: 'pending',
            },
          ],
        },
      })
      .mockResolvedValueOnce({ data: { count: '2' } })

    await expect(brainApi.knowledgeGraphSubgraph({ query: '屏障' })).resolves.toEqual({
      nodes: [expect.objectContaining({ id: '1', label: '屏障修护', type: 'topic', weight: 0.9 })],
      edges: [expect.objectContaining({ source: '1', target: '2', relation: 'contains', weight: 0.8 })],
      contradictions: [{ id: 'c1' }],
    })
    await expect(brainApi.knowledgeGraphQuery({ keyword: '屏障' })).resolves.toEqual([{ id: 1, name: '屏障修护' }])
    await expect(brainApi.graphRagContext({ query: '屏障' })).resolves.toEqual({
      context: '屏障修护 -> 神经酰胺',
      available: true,
      hops: 2,
    })
    await expect(brainApi.relationSuggestionsList()).resolves.toEqual([
      expect.objectContaining({
        id: 21,
        sourceEntityKey: '屏障修护',
        targetEntityKey: '神经酰胺',
        relationType: 'contains',
        confidence: 0.91,
      }),
    ])
    await expect(brainApi.relationSuggestionsMaterialize([{ sourceEntityKey: 'a', targetEntityKey: 'b' }])).resolves.toEqual({ inserted: 2 })
  })

  it('normalizes diagnosis, strategy, risk, and extension responses', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          conversionRate: '37%',
          factors: '强痛点,专业背书',
          risks: ['夸大功效'],
          summary: '晚场更稳',
        },
      })
      .mockResolvedValueOnce({
        data: {
          userId: '88',
          preferences: { 种草: '82%', _meta: '1' },
          styleTags: '温和,专业',
          progress: '0.64',
          pattern: '{"lastAction":"edit-script"}',
          updateTime: '2026-05-22T10:00:00+08:00',
        },
      })
      .mockResolvedValueOnce({
        data: {
          category: '护肤',
          hotTopics: '屏障修护,早C晚A',
          trendSources: ['douyin'],
          recentInsights: ['竞品强调温和修护'],
          advice: '突出证据链',
          profileFocus: ['敏感肌'],
          nextActions: ['更新热词'],
          scope: '近7天',
        },
      })
      .mockResolvedValueOnce({
        data: {
          id: '5',
          positioningScore: '80%',
          contentScore: '70%',
          growthScore: '60%',
          risk: 'LOW',
          priorities: '强化成分证据',
          analysis: '账号诊断摘要',
          isEstimated: 'true',
        },
      })
      .mockResolvedValueOnce({ data: { status: 'success', analysis: '内容诊断', strengths: ['证据清晰'] } })
      .mockResolvedValueOnce({
        data: {
          risks: [
            { severity: '3', riskType: 'absolute', text: '包含极限词', start: '0', end: '2', advice: '改写' },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          accountId: '12',
          industry: '行业分析',
          competitor: '竞品分析',
          opportunities: '成分教育',
          swot: { strength: '0.8' },
          contentMatrix: [{ contentType: '种草', advice: '场景切入', sortOrder: '1' }],
          phases: [{ phaseName: '冷启动', target: '破千粉', actions: '每日直播' }],
          diagnoses: [{ name: '定位清晰', summary: '继续强化标签' }],
        },
      })
      .mockResolvedValueOnce({
        data: {
          summary: '先破万粉',
          factors: '稳定直播',
          phases: [{ order: '1', phase: '起量期', fansTarget: '10000', actions: '热点承接', metrics: '互动率', duration: '30天' }],
        },
      })
      .mockResolvedValueOnce({ data: { total: '100', violations: '8', accuracy: '93%' } })
      .mockResolvedValueOnce({ data: { prompt: '温和专业', score: '88%' } })

    await expect(brainApi.causalInfer({})).resolves.toEqual({
      expectedConversionRate: 0.37,
      keyFactors: ['强痛点', '专业背书'],
      riskPoints: ['夸大功效'],
      explanation: '晚场更稳',
    })
    await expect(brainApi.userProfile()).resolves.toMatchObject({
      userId: 88,
      contentPreferences: { 种草: 0.82, _meta: 1 },
      expressionStyleTags: ['温和', '专业'],
      learningProgress: 0.64,
      interactionPattern: { lastAction: 'edit-script' },
    })
    await expect(brainApi.industryInsights('护肤')).resolves.toMatchObject({
      行业分类: '护肤',
      趋势热点: ['屏障修护', '早C晚A'],
      差异化建议: '突出证据链',
    })
    await expect(brainApi.accountDiagnose(5)).resolves.toMatchObject({
      accountId: 5,
      positioningClarity: 0.8,
      suggestedPriorities: ['强化成分证据'],
      isEstimated: true,
    })
    await expect(brainApi.contentDiagnosis({ category: '护肤' })).resolves.toMatchObject({
      analysis: '内容诊断',
      strengths: ['证据清晰'],
    })
    await expect(brainApi.riskWarn('最有效')).resolves.toEqual([
      expect.objectContaining({ level: 3, type: 'absolute', message: '包含极限词', suggestion: '改写' }),
    ])
    await expect(brainApi.strategicPlan({ accountId: 12, goals: ['万粉'] })).resolves.toMatchObject({
      accountId: 12,
      opportunityPoints: ['成分教育'],
      swotScores: { strength: 0.8 },
      contentMatrix: [{ type: '种草', strategy: '场景切入', priority: 1 }],
    })
    await expect(brainApi.growthPath({ targetFans: 10000 })).resolves.toMatchObject({
      summary: '先破万粉',
      criticalSuccessFactors: ['稳定直播'],
      phases: [{ phaseOrder: 1, phaseName: '起量期', targetFans: 10000 }],
    })
    await expect(brainApi.riskStats()).resolves.toEqual({
      totalChecks: 100,
      violationCount: 8,
      accuracyEstimate: 0.93,
    })
    await expect(brainApi.styleConsistency({ content: '温和' })).resolves.toEqual({
      prompt: '温和专业',
      styleVector: undefined,
      score: 0.88,
    })
  })
})
