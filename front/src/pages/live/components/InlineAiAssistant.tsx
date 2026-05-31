import { memo, type ReactNode } from 'react'
import { Box, ToggleButtonGroup, ToggleButton, Typography } from '@mui/material'
import ChatIcon from '@mui/icons-material/Chat'
import PsychologyIcon from '@mui/icons-material/Psychology'
import { AiChatPanel, type AiChatPanelProps } from './AiChatPanel'
import { AiAnalystPanel, type AiAnalystPanelProps } from './AiAnalystPanel'

export type AiAssistantMode = 'chat' | 'analyst'

const INLINE_AI_READY_SOURCES = [
  'chat-props',
  'analyst-props',
  'mode-prop',
  'availability-props',
]

const INLINE_AI_UNSUPPORTED_ACTIONS = [
  'direct-network-call',
  'local-chat-fallback',
  'local-analyst-fallback',
  'script-mutation-in-wrapper',
  'copy-library-mutation-in-wrapper',
]

export interface InlineAiAssistantProps {
  mode: AiAssistantMode
  onModeChange: (mode: AiAssistantMode) => void
  /** 是否允许切换（chat 需 editingId，analyst 需 analystScript） */
  chatAvailable: boolean
  analystAvailable: boolean
  /** Optional: maximize button rendered inline with mode toggle */
  maximizeButton?: ReactNode
  // Chat props (forwarded to AiChatPanel)
  chatProps: Omit<AiChatPanelProps, 'fillContainer'>
  // Analyst props (forwarded to AiAnalystPanel)
  analystProps: Omit<AiAnalystPanelProps, 'fillContainer'>
}

export const InlineAiAssistant = memo(function InlineAiAssistant({
  mode,
  onModeChange,
  chatAvailable,
  analystAvailable,
  maximizeButton,
  chatProps,
  analystProps,
}: InlineAiAssistantProps) {
  return (
    <Box
      data-testid="inline-ai-assistant-root"
      data-contract-scope="live-inline-ai-assistant-props-router"
      data-ready-sources={INLINE_AI_READY_SOURCES.join('|')}
      data-unsupported-actions={INLINE_AI_UNSUPPORTED_ACTIONS.join('|')}
      data-mode={mode}
      data-chat-available={chatAvailable ? 'true' : 'false'}
      data-analyst-available={analystAvailable ? 'true' : 'false'}
      data-no-direct-api-request="true"
      data-no-local-ai-fallback="true"
      sx={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}
    >
      {/* Mode toggle + maximize */}
      <Box
        data-testid="inline-ai-assistant-toolbar"
        data-contract-source="mode-prop|availability-props"
        sx={{ px: 1, py: 0.5, flexShrink: 0, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 0.5 }}
      >
        <ToggleButtonGroup
          size="small"
          value={mode}
          exclusive
          onChange={(_, v) => v && onModeChange(v)}
          data-testid="inline-ai-assistant-mode-toggle"
          data-contract-source="onModeChange-prop"
        >
          <ToggleButton
            value="chat"
            disabled={!chatAvailable}
            data-testid="inline-ai-assistant-chat-toggle"
            data-contract-source="onModeChange-prop"
            data-disabled-reason={chatAvailable ? 'ready' : 'chat-unavailable'}
          >
            <ChatIcon fontSize="small" sx={{ mr: 0.5 }} />
            对话
          </ToggleButton>
          <ToggleButton
            value="analyst"
            disabled={!analystAvailable}
            data-testid="inline-ai-assistant-analyst-toggle"
            data-contract-source="onModeChange-prop"
            data-disabled-reason={analystAvailable ? 'ready' : 'analyst-unavailable'}
          >
            <PsychologyIcon fontSize="small" sx={{ mr: 0.5 }} />
            分析
          </ToggleButton>
        </ToggleButtonGroup>
        {maximizeButton && (
          <Box data-testid="inline-ai-assistant-maximize-slot" data-contract-source="maximizeButton-prop">
            {maximizeButton}
          </Box>
        )}
      </Box>

      {/* Panel content */}
      <Box
        data-testid="inline-ai-assistant-content"
        data-contract-source={mode === 'chat' ? 'chat-props' : 'analyst-props'}
        data-no-local-ai-fallback="true"
        sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', minHeight: 0 }}
      >
        {mode === 'chat' && chatAvailable ? (
          <AiChatPanel {...chatProps} />
        ) : mode === 'chat' && !chatAvailable ? (
          <Box
            data-testid="inline-ai-chat-unavailable"
            data-contract-source="chatAvailable-prop"
            data-no-local-chat-fallback="true"
            sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', p: 3 }}
          >
            <Typography color="text.secondary" textAlign="center">
              点击左侧任意话术，开始 AI 对话
            </Typography>
          </Box>
        ) : mode === 'analyst' && analystAvailable ? (
          <AiAnalystPanel {...analystProps} />
        ) : (
          <Box
            data-testid="inline-ai-analyst-unavailable"
            data-contract-source="analystAvailable-prop"
            data-no-local-analyst-fallback="true"
            sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', p: 3 }}
          >
            <Typography color="text.secondary" textAlign="center">
              请先选择话术进行分析
            </Typography>
          </Box>
        )}
      </Box>
    </Box>
  )
})
