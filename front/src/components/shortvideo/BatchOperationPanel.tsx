/**
 * 批量操作面板：选中项后显示批量操作按钮
 */
import { Box, Button, Checkbox, Typography } from '@mui/material'
import { Delete as DeleteIcon, Refresh as RefreshIcon } from '@mui/icons-material'

export interface BatchOperationPanelProps {
  selectedIds: (string | number)[]
  totalCount: number
  onSelectAll?: (checked: boolean) => void
  onBatchDelete?: () => void
  onBatchRegenerate?: () => void
  onClear?: () => void
  loading?: boolean
}

export function BatchOperationPanel({
  selectedIds,
  totalCount,
  onSelectAll,
  onBatchDelete,
  onBatchRegenerate,
  onClear,
  loading = false,
}: BatchOperationPanelProps) {
  const count = selectedIds.length
  const allSelected = count > 0 && count === totalCount
  const indeterminate = count > 0 && count < totalCount

  if (count === 0) return null

  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        gap: 2,
        py: 1.5,
        px: 2,
        bgcolor: 'action.selected',
        borderRadius: 1,
        mb: 2,
      }}
    >
      <Checkbox
        checked={allSelected}
        indeterminate={indeterminate}
        onChange={(e) => onSelectAll?.(e.target.checked)}
        size="small"
      />
      <Typography variant="body2" color="text.secondary">
        已选 {count} 项
      </Typography>
      {onBatchDelete && (
        <Button
          size="small"
          color="error"
          startIcon={<DeleteIcon />}
          onClick={onBatchDelete}
          disabled={loading}
        >
          批量删除
        </Button>
      )}
      {onBatchRegenerate && (
        <Button
          size="small"
          variant="outlined"
          startIcon={<RefreshIcon />}
          onClick={onBatchRegenerate}
          disabled={loading}
        >
          批量重新生成
        </Button>
      )}
      {onClear && (
        <Button size="small" onClick={onClear}>
          取消选择
        </Button>
      )}
    </Box>
  )
}
