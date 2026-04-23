import { Paper, Stack, Typography, Button } from '@mui/material'

interface ProductBatchBarProps {
  count: number
  loading: boolean
  onAction: (action: 'publish' | 'unpublish' | 'featured' | 'unfeatured' | 'extract') => void
  onClear: () => void
}

export function ProductBatchBar({ count, loading, onAction, onClear }: ProductBatchBarProps) {
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
          已选 {count} 件
        </Typography>
        <Button size="small" variant="outlined" disabled={loading} onClick={() => onAction('publish')}>
          批量上架
        </Button>
        <Button size="small" variant="outlined" disabled={loading} onClick={() => onAction('unpublish')}>
          批量下架
        </Button>
        <Button size="small" variant="outlined" disabled={loading} onClick={() => onAction('featured')}>
          批量精选
        </Button>
        <Button size="small" variant="outlined" disabled={loading} onClick={() => onAction('unfeatured')}>
          取消精选
        </Button>
        <Button size="small" variant="outlined" disabled={loading} onClick={() => onAction('extract')}>
          批量提取
        </Button>
        <Button size="small" variant="text" onClick={onClear}>
          取消选择
        </Button>
      </Stack>
    </Paper>
  )
}
