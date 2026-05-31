import { createContext, useContext } from 'react'

export interface SlotTimelineEntry {
  scriptId: number
  slotLabel: string
  scriptType: string
  sequenceNo?: number
  index?: number
  content?: string
  percent?: number
  current?: number
  total?: number
  stage?: string
  event?: 'progress' | 'slot_done' | 'slot_failed' | 'status'
  timestamp?: number
  failed?: boolean
  errorMsg?: string
}

export interface GenerationContextValue {
  isGenerating: boolean
  generationProgress: number
  generationMessage: string
  genJustCompleted: boolean
  slotTimeline: SlotTimelineEntry[]
  startGeneration: (options: Record<string, unknown>) => Promise<void>
  cancelGeneration: () => void
}

export const GenerationContext = createContext<GenerationContextValue | null>(null)

export function useGeneration(): GenerationContextValue {
  const ctx = useContext(GenerationContext)
  if (!ctx) throw new Error('useGeneration must be used within GenerationProvider')
  return ctx
}
