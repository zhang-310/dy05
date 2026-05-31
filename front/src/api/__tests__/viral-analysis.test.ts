import { beforeEach, describe, expect, it, vi } from 'vitest'
import request from '@/utils/request'
import {
  fetchHotTopicPool,
  generateFusedScript,
  generateHotspotFused,
  matchPersonas,
  resolveDeepAnalyzeEvidence,
  type DeepAnalyzeStatusResult,
} from '../viral-analysis'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

vi.mock('@/utils/sse-client', () => ({
  ssePost: vi.fn(),
}))

describe('viral-analysis evidence helpers', () => {
  const mockPost = vi.mocked(request.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('prefers explicit evidence fields returned by backend', () => {
    const status: DeepAnalyzeStatusResult = {
      evidenceLevel: 'inferred',
      evidenceDetails: {
        overallLevel: 'inferred',
        transcriptLevel: 'inferred',
        sceneLevel: 'empirical',
        commentLevel: 'missing',
        hasCommentSamples: false,
      },
      transcriptDisplayLabel: '推演口播稿（非 ASR 实录）',
      sceneDisplayLabel: '实证场景拆解',
      inferenceRisk: true,
    }

    expect(resolveDeepAnalyzeEvidence(status)).toEqual({
      overallLevel: 'inferred',
      transcriptLevel: 'inferred',
      sceneLevel: 'empirical',
      commentLevel: 'missing',
      hasCommentSamples: false,
      transcriptDisplayLabel: '推演口播稿（非 ASR 实录）',
      sceneDisplayLabel: '实证场景拆解',
      inferenceRisk: true,
    })
  })

  it('falls back to deepAnalysisResult and persisted prefixes when explicit evidence fields are absent', () => {
    const status: DeepAnalyzeStatusResult = {
      transcript: '【推演口播】先抛问题，再给方案',
      sceneDescriptions: '【推演场景】\n[0-3s] 镜前特写',
      deepAnalysisResult: JSON.stringify({
        evidenceLevel: 'inferred',
        evidenceDetails: {
          overallLevel: 'inferred',
          transcriptLevel: 'inferred',
          sceneLevel: 'inferred',
          commentLevel: 'empirical',
          hasCommentSamples: true,
        },
      }),
    }

    expect(resolveDeepAnalyzeEvidence(status)).toEqual({
      overallLevel: 'inferred',
      transcriptLevel: 'inferred',
      sceneLevel: 'inferred',
      commentLevel: 'empirical',
      hasCommentSamples: true,
      transcriptDisplayLabel: '推演口播稿（非 ASR 实录）',
      sceneDisplayLabel: '推演场景（非真实抽帧）',
      inferenceRisk: true,
    })
  })

  it('normalizes wrapped hot topic pool and persona fusion payloads', async () => {
    mockPost
      .mockResolvedValueOnce({
        data: {
          records: [
            { hot_topic_id: '7', keyword: '包装热点', hot_score: '8800', source: '统一热点池', create_time: '2026-05-22 10:00:00' },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          rows: [
            { personaId: 12, personaName: '专业护肤达人', score: 0.91 },
          ],
        },
      })
      .mockResolvedValueOnce({
        data: {
          record: { fusedScript: '爆款人设融合脚本', scriptId: '22' },
        },
      })
      .mockResolvedValueOnce({
        data: {
          item: { content: '热点三要素融合脚本', hotTopicId: '7' },
        },
      })

    await expect(fetchHotTopicPool(50)).resolves.toEqual({
      hotTopics: [
        {
          id: 7,
          topic: '包装热点',
          heat: 8800,
          source: '统一热点池',
          createdAt: '2026-05-22 10:00:00',
        },
      ],
    })
    await expect(matchPersonas(8)).resolves.toEqual([
      { personaId: 12, personaName: '专业护肤达人', score: 0.91 },
    ])
    await expect(generateFusedScript(8, 12)).resolves.toEqual({ fusedScript: '爆款人设融合脚本', scriptId: '22' })
    await expect(generateHotspotFused(7, 12, 6)).resolves.toEqual({ content: '热点三要素融合脚本', hotTopicId: '7' })

    expect(mockPost).toHaveBeenCalledWith('/short-video/cross/hot-topic-pool', { limit: 50 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/persona-fusion/match-personas', { viralVideoId: 8 })
    expect(mockPost).toHaveBeenCalledWith('/short-video/persona-fusion/generate-fused-script', {
      viralVideoId: 8,
      personaId: 12,
      remakeType: 'form_imitation',
    })
    expect(mockPost).toHaveBeenCalledWith('/short-video/persona-fusion/generate-hotspot-fused', {
      hotTopicId: 7,
      personaId: 12,
      productId: 6,
    })
  })
})
