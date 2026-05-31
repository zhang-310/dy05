import { alpha, type SxProps, type Theme } from '@mui/material/styles'

interface AgentMessageBubbleOptions {
  maxWidth?: string
  bordered?: boolean
  userVariant?: 'solid' | 'soft'
}

export function agentMessageBubbleSx(
  role: string,
  options: AgentMessageBubbleOptions = {},
): SxProps<Theme> {
  const { maxWidth = '75%', bordered = false, userVariant = 'solid' } = options

  return (theme) => {
    const isUser = role === 'user'
    const isDark = theme.palette.mode === 'dark'
    const borderColor = isDark ? theme.palette.divider : theme.palette.grey[200]

    if (isUser && userVariant === 'solid') {
      return {
        p: 1.5,
        maxWidth,
        borderRadius: 2,
        bgcolor: theme.palette.primary.main,
        color: theme.palette.primary.contrastText,
        ...(bordered ? { border: '1px solid', borderColor: alpha(theme.palette.primary.main, isDark ? 0.45 : 0.28) } : {}),
      }
    }

    if (isUser) {
      return {
        p: 1.5,
        maxWidth,
        borderRadius: 2,
        bgcolor: alpha(theme.palette.primary.main, isDark ? 0.24 : 0.1),
        color: theme.palette.text.primary,
        border: '1px solid',
        borderColor: alpha(theme.palette.primary.main, isDark ? 0.45 : 0.24),
      }
    }

    return {
      p: 1.5,
      maxWidth,
      borderRadius: 2,
      bgcolor: isDark ? theme.palette.background.paper : theme.palette.grey[100],
      color: theme.palette.text.primary,
      ...(bordered ? { border: '1px solid', borderColor } : {}),
    }
  }
}

export function agentStreamingBubbleSx(options: AgentMessageBubbleOptions = {}): SxProps<Theme> {
  return agentMessageBubbleSx('assistant', options)
}

export function agentStreamStatusBarSx(isStreaming: boolean): SxProps<Theme> {
  return (theme) => {
    const accent = theme.palette.primary.main
    return {
      px: 1.25,
      py: 0.875,
      border: '1px solid',
      borderColor: isStreaming
        ? alpha(accent, theme.palette.mode === 'dark' ? 0.5 : 0.38)
        : theme.palette.divider,
      borderRadius: 1,
      bgcolor: isStreaming
        ? alpha(accent, theme.palette.mode === 'dark' ? 0.18 : 0.08)
        : theme.palette.action.hover,
    }
  }
}
