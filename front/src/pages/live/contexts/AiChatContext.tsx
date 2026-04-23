import { createContext, useContext } from 'react'
import type { BuilderState } from '../hooks/useLiveScriptBuilder'

export type AiChatContextValue = Pick<
  BuilderState,
  | 'analystScript' | 'setAnalystScript'
  | 'analystContent' | 'setAnalystContent'
  | 'analystDuration' | 'setAnalystDuration'
  | 'analystRequirement' | 'setAnalystRequirement'
  | 'analystLoading' | 'analystStreaming'
  | 'handleOpenAnalyst' | 'handleAnalystAutoFill' | 'handleAnalystApply'
  | 'chatMessage' | 'setChatMessage'
  | 'chatResponse' | 'setChatResponse'
  | 'chatLoading' | 'chatStreaming' | 'chatHistory'
  | 'chatPersona' | 'setChatPersona'
  | 'chatScene' | 'setChatScene'
  | 'chatDurationSec' | 'setChatDurationSec'
  | 'chatDimensions' | 'setChatDimensions'
  | 'copySaveLoading'
  | 'handleChatSend' | 'handleChatApply'
  | 'handleSaveToCopy' | 'handleChatHistoryClear'
>

export const AiChatContext = createContext<AiChatContextValue | null>(null)

export function useAiChatCtx(): AiChatContextValue {
  const ctx = useContext(AiChatContext)
  if (!ctx) throw new Error('useAiChatCtx must be used within AiChatProvider')
  return ctx
}
