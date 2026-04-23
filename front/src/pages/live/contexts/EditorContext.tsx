import { createContext, useContext } from 'react'
import type { LiveScript, LiveScriptSave } from '@/api/live'

export interface EditorContextValue {
  editingId: number | null
  editContent: string
  setEditContent: (v: string) => void
  handleStartEdit: (script: LiveScript) => void
  handleCancelEdit: () => void
  handleSaveEdit: () => Promise<void>
  handleScriptSave: (p: Partial<LiveScriptSave>) => Promise<void>
  handleScriptDelete: (id: number) => Promise<void>
  isSaving: boolean
}

export const EditorContext = createContext<EditorContextValue | null>(null)

export function useEditor(): EditorContextValue {
  const ctx = useContext(EditorContext)
  if (!ctx) throw new Error('useEditor must be used within EditorProvider')
  return ctx
}
