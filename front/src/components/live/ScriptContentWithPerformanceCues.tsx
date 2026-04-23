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
  const base: SxProps<Theme> = {
    display: 'inline',
    px: variant === 'compact' ? 0.5 : 1,
    py: variant === 'compact' ? 0.25 : 0.35,
    borderRadius: 1,
    border: '1px solid',
    fontWeight: 600,
    whiteSpace: 'pre-wrap',
  }
  if (variant === 'prompter') {
    return {
      ...base,
      fontSize: '0.85em',
      verticalAlign: 'baseline',
      bgcolor: (theme) => alpha(theme.palette.warning.main, 0.16),
      borderColor: 'warning.main',
      color: 'warning.dark',
    }
  }
  if (variant === 'panel') {
    return {
      ...base,
      fontSize: '0.82em',
      verticalAlign: 'baseline',
      bgcolor: isFullscreen ? 'rgba(255,193,7,0.22)' : (theme) => alpha(theme.palette.warning.main, 0.16),
      borderColor: isFullscreen ? 'warning.light' : 'warning.main',
      color: isFullscreen ? '#ffe082' : 'warning.dark',
    }
  }
  return {
    ...base,
    fontSize: '0.95em',
    verticalAlign: 'baseline',
    bgcolor: (theme) => alpha(theme.palette.warning.main, 0.14),
    borderColor: 'warning.light',
    color: 'warning.dark',
  }
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

  const textWrapperSx: SxProps<Theme> =
    variant === 'prompter'
      ? {
          fontSize: '48px',
          fontWeight: 'bold',
          lineHeight: 1.6,
          textAlign: 'center',
          color: '#000000',
        }
      : variant === 'panel'
        ? {
            fontSize: isFullscreen ? '48px' : '36px',
            fontWeight: 700,
            color: isFullscreen ? '#fff' : '#000',
            lineHeight: 1.5,
            textAlign: 'center',
          }
        : {
            fontSize: '0.9375rem',
            lineHeight: 1.7,
            whiteSpace: 'pre-wrap',
            display: 'inline',
          }

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
      >
        {seg.text}
      </Box>
    )
  })

  if (variant === 'compact') {
    return (
      <Box component="span" sx={textWrapperSx}>
        {body}
      </Box>
    )
  }

  return (
    <Box sx={{ width: '100%' }}>
      <Typography component="div" sx={textWrapperSx}>
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
              sx={
                variant === 'panel' && isFullscreen
                  ? { borderColor: 'warning.light', color: '#ffe082' }
                  : undefined
              }
            />
          ))}
        </Stack>
      )}
    </Box>
  )
}
