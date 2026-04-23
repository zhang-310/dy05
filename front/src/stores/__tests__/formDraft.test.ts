import { describe, it, expect, beforeEach } from 'vitest'
import { useFormDraftStore } from '../formDraft'

describe('formDraft', () => {
  beforeEach(() => {
    useFormDraftStore.setState({ drafts: {} })
  })

  it('initializes with empty drafts', () => {
    const state = useFormDraftStore.getState()
    expect(state.drafts).toEqual({})
  })

  it('setDraft stores a draft value', () => {
    const { setDraft } = useFormDraftStore.getState()

    setDraft('test-key', { name: 'test', value: 123 })

    const state = useFormDraftStore.getState()
    expect(state.drafts['test-key']).toEqual({ name: 'test', value: 123 })
  })

  it('setDraft updates existing draft', () => {
    const { setDraft } = useFormDraftStore.getState()

    setDraft('key1', 'value1')
    setDraft('key1', 'value2')

    const state = useFormDraftStore.getState()
    expect(state.drafts['key1']).toBe('value2')
  })

  it('setDraft preserves other drafts', () => {
    const { setDraft } = useFormDraftStore.getState()

    setDraft('key1', 'value1')
    setDraft('key2', 'value2')

    const state = useFormDraftStore.getState()
    expect(state.drafts).toEqual({
      key1: 'value1',
      key2: 'value2',
    })
  })

  it('clearDraft removes a draft', () => {
    const { setDraft, clearDraft } = useFormDraftStore.getState()

    setDraft('key1', 'value1')
    setDraft('key2', 'value2')
    clearDraft('key1')

    const state = useFormDraftStore.getState()
    expect(state.drafts).toEqual({ key2: 'value2' })
  })

  it('clearDraft does nothing for non-existent key', () => {
    const { setDraft, clearDraft } = useFormDraftStore.getState()

    setDraft('key1', 'value1')
    clearDraft('non-existent')

    const state = useFormDraftStore.getState()
    expect(state.drafts).toEqual({ key1: 'value1' })
  })
})
