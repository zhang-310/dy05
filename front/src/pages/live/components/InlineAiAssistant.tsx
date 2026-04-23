import { memo, type ReactNode } from 'react'
import { Box, ToggleButtonGroup, ToggleButton, Typography } from '@mui/material'
import ChatIcon from '@mui/icons-material/Chat'
import PsychologyIcon from '@mui/icons-material/Psychology'
import { AiChatPanel, type AiChatPanelProps } from './AiChatPanel'
import { AiAnalystPanel, type AiAnalystPanelProps } from './AiAnalystPanel'

export type AiAssistantMode = 'chat' | 'analyst'

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
    <Box sx={{ display: 'flex', flexDirection: 'column', flex: 1, overflow: 'hidden' }}>
      {/* Mode toggle + maximize */}
      <Box sx={{ px: 1, py: 0.5, flexShrink: 0, display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 0.5 }}>
        <ToggleButtonGroup
          size="small"
          value={mode}
          exclusive
          onChange={(_, v) => v && onModeChange(v)}
        >
          <ToggleButton value="chat" disabled={!chatAvailable}>
            <ChatIcon fontSize="small" sx={{ mr: 0.5 }} />
            对话
          </ToggleButton>
          <ToggleButton value="analyst" disabled={!analystAvailable}>
            <PsychologyIcon fontSize="small" sx={{ mr: 0.5 }} />
            分析
          </ToggleButton>
        </ToggleButtonGroup>
        {maximizeButton}
      </Box>

      {/* Panel content */}
      <Box sx={{ flex: 1, overflow: 'auto', display: 'flex', flexDirection: 'column', minHeight: 0 }}>
        {mode === 'chat' && chatAvailable ? (
          <AiChatPanel {...chatProps} />
        ) : mode === 'chat' && !chatAvailable ? (
          <Box sx={{ flex: 1, display: 'flex', alignItems: 'center', justifyContent: 'center', p: 3 }}>
            <Typography color="text.secondary" textAlign="center">
              点击左侧任意话术，开始 AI 对话
            </Typography>
          </Box>
        ) : mode === 'analyst' && analystAvailable ? (
          <AiAnalystPanel {...analystProps} />
        ) : null}
      </Box>
    </Box>
  )
})
