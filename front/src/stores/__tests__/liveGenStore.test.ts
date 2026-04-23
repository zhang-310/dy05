/**
 * liveGenStore 单元测试
 */
import { describe, it, expect, beforeEach } from 'vitest'
import { useLiveGenStore } from '../liveGenStore'

describe('liveGenStore', () => {
  beforeEach(() => {
    useLiveGenStore.setState({
      genLoading: false,
      genStyle: 'conversational',
      useKbRef: true,
      selectedModelId: '',
      ipType: 'general',
      materialType: 'script',
      scriptModule: 'full',
      retentionStrategy: 'story',
      interactionLevel: 'medium',
      fullGenProgress: 0,
      flowSteps: [],
      flowPanelVisible: false,
      cancellingGen: false,
    })
  })

  it('setGenLoading updates genLoading', () => {
    useLiveGenStore.getState().setGenLoading(true)
    expect(useLiveGenStore.getState().genLoading).toBe(true)
    useLiveGenStore.getState().setGenLoading(false)
    expect(useLiveGenStore.getState().genLoading).toBe(false)
  })

  it('setGenStyle updates genStyle', () => {
    useLiveGenStore.getState().setGenStyle('亲切')
    expect(useLiveGenStore.getState().genStyle).toBe('亲切')
  })

  it('setUseKbRef updates useKbRef', () => {
    useLiveGenStore.getState().setUseKbRef(false)
    expect(useLiveGenStore.getState().useKbRef).toBe(false)
  })

  it('setSelectedModelId updates selectedModelId', () => {
    useLiveGenStore.getState().setSelectedModelId('model-123')
    expect(useLiveGenStore.getState().selectedModelId).toBe('model-123')
  })

  it('setFlowSteps updates flowSteps', () => {
    const steps = [
      { label: '步骤1', status: 'done' as const },
      { label: '步骤2', status: 'loading' as const },
    ]
    useLiveGenStore.getState().setFlowSteps(steps)
    expect(useLiveGenStore.getState().flowSteps).toEqual(steps)
  })

  it('setFullGenProgress updates progress', () => {
    useLiveGenStore.getState().setFullGenProgress(50)
    expect(useLiveGenStore.getState().fullGenProgress).toBe(50)
  })

  it('resetGeneration clears generation state', () => {
    useLiveGenStore.getState().setGenLoading(true)
    useLiveGenStore.getState().setFullGenProgress(50)
    useLiveGenStore.getState().setFlowSteps([{ label: 'x', status: 'loading' }])
    useLiveGenStore.getState().resetGeneration()
    expect(useLiveGenStore.getState().genLoading).toBe(false)
    expect(useLiveGenStore.getState().fullGenProgress).toBe(0)
    expect(useLiveGenStore.getState().flowSteps).toEqual([])
    expect(useLiveGenStore.getState().flowPanelVisible).toBe(false)
    expect(useLiveGenStore.getState().cancellingGen).toBe(false)
  })
})
