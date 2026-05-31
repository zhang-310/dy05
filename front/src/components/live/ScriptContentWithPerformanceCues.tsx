import { Box, Chip, Stack, Typography } from '@mui/material'
import TipsAndUpdatesIcon from '@mui/icons-material/TipsAndUpdates'
import { alpha } from '@mui/material/styles'
import type { SxProps, Theme } from '@mui/material/styles'
import {
  listPerformanceCues,
  parsePerformanceCueSegments,
} from '@/utils/performanceCueText'

export interface ScriptContentWithPerformanceCuesProps {
  content: string
  /** 提词大字 / 面板标题 / 编辑区紧凑预览 */
  variant: 'prompter' | 'panel' | 'compact'
  /** LiveRealtimePanel 全屏深色底 */
  isFullscreen?: boolean
  /** 是否在文下展示「表演提示」芯片条 */
  showCueChips?: boolean
}

const cueBoxSx = (
  variant: ScriptContentWithPerformanceCuesProps['variant'],
  isFullscreen?: boolean
): SxProps<Theme> => {
  const emphasis = variant === 'panel' ? 0.16 : variant === 'compact' ? 0.14 : 0.16
  return (theme) => {
    const warningColor = isFullscreen || theme.palette.mode === 'dark'
      ? theme.palette.warning.light
      : theme.palette.warning.main
    return {
      display: 'inline',
      px: variant === 'compact' ? 0.5 : 1,
      py: variant === 'compact' ? 0.25 : 0.35,
      borderRadius: 1,
      border: '1px solid',
      borderColor: alpha(warningColor, isFullscreen ? 0.78 : 0.58),
      bgcolor: alpha(warningColor, isFullscreen ? 0.22 : emphasis),
      color: isFullscreen || theme.palette.mode === 'dark' ? theme.palette.warning.light : theme.palette.warning.dark,
      fontWeight: 600,
      fontSize: variant === 'prompter' ? '0.85em' : variant === 'panel' ? '0.82em' : '0.95em',
      verticalAlign: 'baseline',
      whiteSpace: 'pre-wrap',
    }
  }
}

const textWrapperSx = (
  variant: ScriptContentWithPerformanceCuesProps['variant'],
  isFullscreen: boolean,
): SxProps<Theme> => (theme) => (
  variant === 'prompter'
    ? {
        fontSize: '48px',
        fontWeight: 'bold',
        lineHeight: 1.6,
        textAlign: 'center',
        color: theme.palette.text.primary,
      }
    : variant === 'panel'
      ? {
          fontSize: isFullscreen ? '48px' : '36px',
          fontWeight: 700,
          color: isFullscreen ? theme.palette.common.white : theme.palette.text.primary,
          lineHeight: 1.5,
          textAlign: 'center',
        }
      : {
          fontSize: '0.9375rem',
          lineHeight: 1.7,
          whiteSpace: 'pre-wrap',
          display: 'inline',
          color: theme.palette.text.primary,
        }
)

const cueChipSx = (
  variant: ScriptContentWithPerformanceCuesProps['variant'],
  isFullscreen: boolean,
): SxProps<Theme> | undefined => {
  if (variant !== 'panel' || !isFullscreen) return undefined
  return (theme) => ({
    borderColor: alpha(theme.palette.warning.light, 0.72),
    color: theme.palette.warning.light,
    '& .MuiChip-icon': {
      color: theme.palette.warning.light,
    },
  })
}

export default function ScriptContentWithPerformanceCues({
  content,
  variant,
  isFullscreen = false,
  showCueChips = true,
}: ScriptContentWithPerformanceCuesProps) {
  const segments = parsePerformanceCueSegments(content)
  const cues = listPerformanceCues(content)
  const showChips = showCueChips && cues.length > 0 && variant !== 'compact'

  const body = segments.map((seg, i) => {
    if (seg.kind === 'text') {
      if (!seg.text) return null
      return <span key={`t-${i}`}>{seg.text}</span>
    }
    return (
      <Box
        key={`c-${i}`}
        component="span"
        sx={cueBoxSx(variant, isFullscreen)}
        title="表演指导"
        data-testid="performance-cue-inline-surface"
        data-cue-variant={variant}
        data-cue-fullscreen={String(isFullscreen)}
      >
        {seg.text}
      </Box>
    )
  })

  if (variant === 'compact') {
    return (
      <Box component="span" data-testid="performance-cue-text-surface" sx={textWrapperSx(variant, isFullscreen)}>
        {body}
      </Box>
    )
  }

  return (
    <Box data-testid="performance-cue-content-surface" data-cue-variant={variant} data-cue-fullscreen={String(isFullscreen)} sx={{ width: '100%' }}>
      <Typography component="div" data-testid="performance-cue-text-surface" sx={textWrapperSx(variant, isFullscreen)}>
        {body}
      </Typography>
      {showChips && (
        <Stack
          direction="row"
          spacing={1}
          useFlexGap
          flexWrap="wrap"
          justifyContent="center"
          sx={{ mt: variant === 'prompter' ? 2 : 1.5 }}
        >
          {cues.map((c, i) => (
            <Chip
              key={`chip-${i}`}
              size="small"
              icon={<TipsAndUpdatesIcon sx={{ '&&': { fontSize: 18 } }} />}
              label={c}
              color="warning"
              variant="outlined"
              data-testid="performance-cue-chip-surface"
              data-cue-fullscreen={String(isFullscreen)}
              sx={cueChipSx(variant, isFullscreen)}
            />
          ))}
        </Stack>
      )}
    </Box>
  )
}
