/**
 * @deprecated 此 store 已规划但尚未接入业务组件。
 * undo/redo 逻辑由 `useScriptEditor` hook 的内部状态处理。
 * P1 前端状态层统一时可考虑将此 store 迁移并接入。
 */
import { create } from 'zustand'
import { persist } from 'zustand/middleware'

interface UndoEntry {
  id: number
  content: string
}

interface LiveEditState {
  editingId: number | null
  editingScript: Record<string, unknown> | null
  editContent: string
  dirty: boolean
  undoStack: UndoEntry[]
  redoStack: UndoEntry[]

  setEditingId: (id: number | null) => void
  setEditingScript: (script: Record<string, unknown> | null) => void
  setEditContent: (content: string) => void
  setDirty: (dirty: boolean) => void
  pushUndo: (entry: UndoEntry) => void
  undo: () => UndoEntry | null
  redo: () => UndoEntry | null
  clearHistory: () => void
  resetEdit: () => void
}

export const useLiveEditStore = create<LiveEditState>()(
  persist(
    (set, get) => ({
      editingId: null,
      editingScript: null,
      editContent: '',
      dirty: false,
      undoStack: [],
      redoStack: [],

      setEditingId: (id) => set({ editingId: id }),
      setEditingScript: (script) => set({ editingScript: script }),
      setEditContent: (content) => set({ editContent: content, dirty: true }),
      setDirty: (dirty) => set({ dirty }),

      pushUndo: (entry) => set((s) => ({
        undoStack: [...s.undoStack.slice(-19), entry],
        redoStack: [],
      })),

      undo: () => {
        const { undoStack, redoStack, editContent, editingId } = get()
        if (undoStack.length === 0) return null
        const last = undoStack[undoStack.length - 1]
        set({
          undoStack: undoStack.slice(0, -1),
          redoStack: [...redoStack, { id: editingId ?? 0, content: editContent }],
          editContent: last.content,
        })
        return last
      },

      redo: () => {
        const { undoStack, redoStack, editContent, editingId } = get()
        if (redoStack.length === 0) return null
        const last = redoStack[redoStack.length - 1]
        set({
          redoStack: redoStack.slice(0, -1),
          undoStack: [...undoStack, { id: editingId ?? 0, content: editContent }],
          editContent: last.content,
        })
        return last
      },

      clearHistory: () => set({ undoStack: [], redoStack: [] }),

      resetEdit: () => set({
        editingId: null,
        editingScript: null,
        editContent: '',
        dirty: false,
        undoStack: [],
        redoStack: [],
      }),
    }),
    {
      name: 'live-edit-storage',
      partialize: (state) => ({
        editingId: state.editingId,
        editContent: state.editContent,
        dirty: state.dirty,
      }),
    }
  )
)
