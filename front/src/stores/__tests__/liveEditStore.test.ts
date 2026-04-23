import { describe, it, expect, beforeEach } from 'vitest'
import { useLiveEditStore } from '../liveEditStore'

describe('liveEditStore', () => {
  beforeEach(() => {
    useLiveEditStore.setState({
      editingId: null,
      editingScript: null,
      editContent: '',
      dirty: false,
      undoStack: [],
      redoStack: [],
    })
  })

  it('initializes with default state', () => {
    const state = useLiveEditStore.getState()
    expect(state.editingId).toBeNull()
    expect(state.editingScript).toBeNull()
    expect(state.editContent).toBe('')
    expect(state.dirty).toBe(false)
    expect(state.undoStack).toEqual([])
    expect(state.redoStack).toEqual([])
  })

  it('setEditingId updates editingId', () => {
    const { setEditingId } = useLiveEditStore.getState()

    setEditingId(123)

    const state = useLiveEditStore.getState()
    expect(state.editingId).toBe(123)
  })

  it('setEditingScript updates editingScript', () => {
    const { setEditingScript } = useLiveEditStore.getState()
    const script = { id: 1, content: 'test' }

    setEditingScript(script)

    const state = useLiveEditStore.getState()
    expect(state.editingScript).toEqual(script)
  })

  it('setEditContent updates content and sets dirty to true', () => {
    const { setEditContent } = useLiveEditStore.getState()

    setEditContent('new content')

    const state = useLiveEditStore.getState()
    expect(state.editContent).toBe('new content')
    expect(state.dirty).toBe(true)
  })

  it('setDirty updates dirty flag', () => {
    const { setDirty } = useLiveEditStore.getState()

    setDirty(true)
    expect(useLiveEditStore.getState().dirty).toBe(true)

    setDirty(false)
    expect(useLiveEditStore.getState().dirty).toBe(false)
  })

  it('pushUndo adds entry to undoStack', () => {
    const { pushUndo } = useLiveEditStore.getState()

    pushUndo({ id: 1, content: 'content1' })
    pushUndo({ id: 1, content: 'content2' })

    const state = useLiveEditStore.getState()
    expect(state.undoStack).toHaveLength(2)
    expect(state.undoStack[1]).toEqual({ id: 1, content: 'content2' })
  })

  it('pushUndo clears redoStack', () => {
    const { pushUndo, undo } = useLiveEditStore.getState()

    pushUndo({ id: 1, content: 'content1' })
    undo()
    pushUndo({ id: 1, content: 'content2' })

    const state = useLiveEditStore.getState()
    expect(state.redoStack).toEqual([])
  })

  it('pushUndo limits undoStack to 20 entries', () => {
    const { pushUndo } = useLiveEditStore.getState()

    for (let i = 0; i < 25; i++) {
      pushUndo({ id: 1, content: `content${i}` })
    }

    const state = useLiveEditStore.getState()
    expect(state.undoStack).toHaveLength(20)
    expect(state.undoStack[0].content).toBe('content5')
  })

  it('undo returns null when undoStack is empty', () => {
    const { undo } = useLiveEditStore.getState()

    const result = undo()

    expect(result).toBeNull()
  })

  it('undo restores previous content and moves to redoStack', () => {
    const { setEditingId, setEditContent, pushUndo, undo } = useLiveEditStore.getState()

    setEditingId(1)
    setEditContent('initial')
    pushUndo({ id: 1, content: 'version1' })
    setEditContent('version2')

    const result = undo()

    expect(result).toEqual({ id: 1, content: 'version1' })
    const state = useLiveEditStore.getState()
    expect(state.editContent).toBe('version1')
    expect(state.undoStack).toHaveLength(0)
    expect(state.redoStack).toHaveLength(1)
    expect(state.redoStack[0]).toEqual({ id: 1, content: 'version2' })
  })

  it('redo returns null when redoStack is empty', () => {
    const { redo } = useLiveEditStore.getState()

    const result = redo()

    expect(result).toBeNull()
  })

  it('redo restores next content and moves to undoStack', () => {
    const { setEditingId, setEditContent, pushUndo, undo, redo } = useLiveEditStore.getState()

    setEditingId(1)
    setEditContent('initial')
    pushUndo({ id: 1, content: 'version1' })
    setEditContent('version2')
    undo()

    const result = redo()

    expect(result).toEqual({ id: 1, content: 'version2' })
    const state = useLiveEditStore.getState()
    expect(state.editContent).toBe('version2')
    expect(state.redoStack).toHaveLength(0)
    expect(state.undoStack).toHaveLength(1)
  })

  it('clearHistory clears both stacks', () => {
    const { pushUndo, undo, clearHistory } = useLiveEditStore.getState()

    pushUndo({ id: 1, content: 'content1' })
    undo()
    clearHistory()

    const state = useLiveEditStore.getState()
    expect(state.undoStack).toEqual([])
    expect(state.redoStack).toEqual([])
  })

  it('resetEdit resets all state', () => {
    const { setEditingId, setEditContent, pushUndo, resetEdit } = useLiveEditStore.getState()

    setEditingId(123)
    setEditContent('content')
    pushUndo({ id: 1, content: 'version1' })
    resetEdit()

    const state = useLiveEditStore.getState()
    expect(state.editingId).toBeNull()
    expect(state.editingScript).toBeNull()
    expect(state.editContent).toBe('')
    expect(state.dirty).toBe(false)
    expect(state.undoStack).toEqual([])
    expect(state.redoStack).toEqual([])
  })
})
