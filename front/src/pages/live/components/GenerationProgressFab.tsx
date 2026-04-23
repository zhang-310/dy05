import { memo } from 'react'
import { Fab, Zoom, Badge } from '@mui/material'
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
        sx={{
          position: 'fixed',
          bottom: 24,
          right: 24,
          zIndex: 1100,
          ...(genLoading ? {
            animation: 'pulse 1.5s ease-in-out infinite',
            '@keyframes pulse': {
              '0%, 100%': { boxShadow: '0 0 0 0 rgba(25, 118, 210, 0.4)' },
              '50%': { boxShadow: '0 0 0 10px rgba(25, 118, 210, 0)' },
            },
          } : {}),
        }}
        title={`查看进度 (${doneCount}/${totalCount})`}
      >
        <Badge badgeContent={totalCount > 0 ? `${doneCount}/${totalCount}` : undefined} color="error" sx={{ '& .MuiBadge-badge': { fontSize: '0.6rem', minWidth: 16, height: 16 } }}>
          <VisibilityIcon />
        </Badge>
      </Fab>
    </Zoom>
  )
})
