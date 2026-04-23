import { describe, expect, it, vi } from 'vitest'
import { resolveDeepAnalyzeEvidence, type DeepAnalyzeStatusResult } from '../viral-analysis'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

vi.mock('@/utils/sse-client', () => ({
  ssePost: vi.fn(),
}))

describe('viral-analysis evidence helpers', () => {
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
})
