import { Paper, Stack, Typography, Button } from '@mui/material'

interface UserBatchBarProps {
  count: number
  loading: boolean
  onAction: (action: 'ban' | 'unban' | 'delete') => void
  onClear: () => void
}

export function UserBatchBar({ count, loading, onAction, onClear }: UserBatchBarProps) {
  if (count === 0) return null

  return (
    <Paper
      sx={{
        position: 'sticky',
        bottom: 0,
        zIndex: 10,
        p: 1.5,
        bgcolor: 'action.hover',
        borderTop: '2px solid',
        borderColor: 'primary.main',
      }}
    >
      <Stack direction="row" alignItems="center" gap={1} flexWrap="wrap">
        <Typography variant="body2" sx={{ fontWeight: 600 }}>
          已选 {count} 人
        </Typography>
        <Button size="small" variant="outlined" disabled={loading} onClick={() => onAction('ban')}>
          批量封禁
        </Button>
        <Button size="small" variant="outlined" disabled={loading} onClick={() => onAction('unban')}>
          批量解封
        </Button>
        <Button size="small" variant="outlined" color="error" disabled={loading} onClick={() => onAction('delete')}>
          批量删除
        </Button>
        <Button size="small" variant="text" onClick={onClear}>
          取消选择
        </Button>
      </Stack>
    </Paper>
  )
}
