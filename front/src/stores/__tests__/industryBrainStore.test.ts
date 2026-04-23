import { describe, it, expect, beforeEach } from 'vitest'
import { useIndustryBrainStore } from '../industryBrainStore'

describe('industryBrainStore', () => {
  beforeEach(() => {
    useIndustryBrainStore.setState({ pendingHotKeywords: [] })
  })

  it('initializes with empty pendingHotKeywords', () => {
    const state = useIndustryBrainStore.getState()
    expect(state.pendingHotKeywords).toEqual([])
  })

  it('setPendingHotKeywords stores keywords', () => {
    const { setPendingHotKeywords } = useIndustryBrainStore.getState()

    setPendingHotKeywords(['护肤', '美白', '抗衰'])

    const state = useIndustryBrainStore.getState()
    expect(state.pendingHotKeywords).toEqual(['护肤', '美白', '抗衰'])
  })

  it('setPendingHotKeywords replaces existing keywords', () => {
    const { setPendingHotKeywords } = useIndustryBrainStore.getState()

    setPendingHotKeywords(['keyword1', 'keyword2'])
    setPendingHotKeywords(['keyword3'])

    const state = useIndustryBrainStore.getState()
    expect(state.pendingHotKeywords).toEqual(['keyword3'])
  })

  it('consumePendingHotKeywords returns and clears keywords', () => {
    const { setPendingHotKeywords, consumePendingHotKeywords } = useIndustryBrainStore.getState()

    setPendingHotKeywords(['护肤', '美白'])
    const result = consumePendingHotKeywords()

    expect(result).toEqual(['护肤', '美白'])

    const state = useIndustryBrainStore.getState()
    expect(state.pendingHotKeywords).toEqual([])
  })

  it('consumePendingHotKeywords returns empty array when no keywords', () => {
    const { consumePendingHotKeywords } = useIndustryBrainStore.getState()

    const result = consumePendingHotKeywords()

    expect(result).toEqual([])
  })

  it('consumePendingHotKeywords can be called multiple times', () => {
    const { setPendingHotKeywords, consumePendingHotKeywords } = useIndustryBrainStore.getState()

    setPendingHotKeywords(['keyword1'])
    consumePendingHotKeywords()
    const result = consumePendingHotKeywords()

    expect(result).toEqual([])
  })
})
