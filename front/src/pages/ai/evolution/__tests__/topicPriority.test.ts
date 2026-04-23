import { describe, it, expect } from 'vitest'
import { displayTopicTier } from '@/pages/ai/evolution/topicPriority'

describe('displayTopicTier', () => {
  it('returns 1-3 as-is', () => {
    expect(displayTopicTier(1)).toBe(1)
    expect(displayTopicTier(2)).toBe(2)
    expect(displayTopicTier(3)).toBe(3)
  })

  it('maps legacy or invalid to P2', () => {
    expect(displayTopicTier(100)).toBe(2)
    expect(displayTopicTier(undefined)).toBe(2)
    expect(displayTopicTier(null)).toBe(2)
  })
})
