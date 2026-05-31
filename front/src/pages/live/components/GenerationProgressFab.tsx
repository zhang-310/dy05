import { memo } from 'react'
import { Fab, Zoom, Badge } from '@mui/material'
import { alpha } from '@mui/material/styles'
import VisibilityIcon from '@mui/icons-material/Visibility'

export interface GenerationProgressFabProps {
  visible: boolean
  genLoading: boolean
  doneCount: number
  totalCount: number
  onClick: () => void
}

export const GenerationProgressFab = memo(function GenerationProgressFab({
  visible,
  genLoading,
  doneCount,
  totalCount,
  onClick,
}: GenerationProgressFabProps) {
  return (
    <Zoom in={visible}>
      <Fab
        size="small"
        color="primary"
        onClick={onClick}
        data-testid="generation-progress-fab-surface"
        data-contract-scope="live-generation-progress-fab"
        data-ready-sources="generation-progress-props|onClick-prop"
        data-no-direct-api-request="true"
        data-generation-loading={genLoading ? 'true' : 'false'}
        data-done-count={doneCount}
        data-total-count={totalCount}
        data-disabled-reason="ready"
        sx={(theme) => {
          const pulseColor = alpha(theme.palette.primary.main, theme.palette.mode === 'dark' ? 0.5 : 0.38)
          return {
          position: 'fixed',
          bottom: 24,
          right: 24,
          zIndex: 1100,
          ...(genLoading ? {
            animation: 'pulse 1.5s ease-in-out infinite',
            '@keyframes pulse': {
              '0%, 100%': { boxShadow: `0 0 0 0 ${pulseColor}` },
              '50%': { boxShadow: `0 0 0 10px ${alpha(theme.palette.primary.main, 0)}` },
            },
          } : {}),
        }}}
        title={`查看进度 (${doneCount}/${totalCount})`}
      >
        <Badge badgeContent={totalCount > 0 ? `${doneCount}/${totalCount}` : undefined} color="error" sx={{ '& .MuiBadge-badge': { fontSize: '0.6rem', minWidth: 16, height: 16 } }}>
          <VisibilityIcon />
        </Badge>
      </Fab>
    </Zoom>
  )
})
