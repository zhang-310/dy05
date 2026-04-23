import { describe, it, expect, beforeEach } from 'vitest'
import { useModalStore } from '../modal'

describe('modal', () => {
  beforeEach(() => {
    useModalStore.setState({ openById: {} })
  })

  it('initializes with empty openById', () => {
    const state = useModalStore.getState()
    expect(state.openById).toEqual({})
  })

  it('setModalOpen sets modal to open', () => {
    const { setModalOpen } = useModalStore.getState()

    setModalOpen('modal-1', true)

    const state = useModalStore.getState()
    expect(state.openById['modal-1']).toBe(true)
  })

  it('setModalOpen sets modal to closed', () => {
    const { setModalOpen } = useModalStore.getState()

    setModalOpen('modal-1', true)
    setModalOpen('modal-1', false)

    const state = useModalStore.getState()
    expect(state.openById['modal-1']).toBe(false)
  })

  it('setModalOpen preserves other modals', () => {
    const { setModalOpen } = useModalStore.getState()

    setModalOpen('modal-1', true)
    setModalOpen('modal-2', true)

    const state = useModalStore.getState()
    expect(state.openById).toEqual({
      'modal-1': true,
      'modal-2': true,
    })
  })

  it('toggleModal opens closed modal', () => {
    const { toggleModal } = useModalStore.getState()

    toggleModal('modal-1')

    const state = useModalStore.getState()
    expect(state.openById['modal-1']).toBe(true)
  })

  it('toggleModal closes open modal', () => {
    const { setModalOpen, toggleModal } = useModalStore.getState()

    setModalOpen('modal-1', true)
    toggleModal('modal-1')

    const state = useModalStore.getState()
    expect(state.openById['modal-1']).toBe(false)
  })

  it('toggleModal handles undefined as false', () => {
    const { toggleModal } = useModalStore.getState()

    toggleModal('new-modal')

    const state = useModalStore.getState()
    expect(state.openById['new-modal']).toBe(true)
  })
})
