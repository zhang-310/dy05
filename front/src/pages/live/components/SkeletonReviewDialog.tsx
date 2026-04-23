import { memo, useState, useCallback, useEffect } from 'react'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Typography,
} from '@mui/material'

const SCRIPT_TYPE_LABELS: Record<string, string> = {
  opening: '开场',
  product: '产品',
  transition: '衔接',
  closing: '结尾',
  emotional: '情绪',
}

const SCRIPT_TYPE_COLORS: Record<string, 'primary' | 'secondary' | 'success' | 'warning' | 'info'> = {
  opening: 'info',
  product: 'primary',
  transition: 'secondary',
  closing: 'warning',
  emotional: 'success',
}

export interface SkeletonItem {
  scriptId: number
  scriptType: string
  summary: string
  suggestedDurationSec: number
}

export interface SkeletonReviewDialogProps {
  open: boolean
  items: SkeletonItem[]
  onClose: () => void
  onConfirm: (edited: SkeletonItem[]) => void
}

export const SkeletonReviewDialog = memo(function SkeletonReviewDialog({
  open,
  items,
  onClose,
  onConfirm,
}: SkeletonReviewDialogProps) {
  const [editedItems, setEditedItems] = useState<SkeletonItem[]>(items)

  // Sync when items change
  useEffect(() => {
    setEditedItems(items)
  }, [items])

  const handleSummaryChange = useCallback((scriptId: number, summary: string) => {
    setEditedItems((prev) => prev.map((item) =>
      item.scriptId === scriptId ? { ...item, summary } : item
    ))
  }, [])

  const handleDurationChange = useCallback((scriptId: number, durationSec: number) => {
    setEditedItems((prev) => prev.map((item) =>
      item.scriptId === scriptId ? { ...item, suggestedDurationSec: durationSec } : item
    ))
  }, [])

  const totalDuration = editedItems.reduce((sum, item) => sum + item.suggestedDurationSec, 0)

  return (
    <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
      <DialogTitle>
        骨架审核
        <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
          共 {editedItems.length} 段 · 预计 {Math.floor(totalDuration / 60)} 分 {totalDuration % 60} 秒
        </Typography>
      </DialogTitle>
      <DialogContent>
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          请审核并调整各段骨架摘要和时长，确认后将开始生成完整话术内容。
        </Typography>
        <Box sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}>
          {editedItems.map((item, idx) => (
            <Box
              key={item.scriptId}
              sx={{
                display: 'flex',
                alignItems: 'flex-start',
                gap: 2,
                p: 2,
                border: 1,
                borderColor: 'divider',
                borderRadius: 1,
                bgcolor: idx % 2 === 0 ? 'action.hover' : 'background.paper',
              }}
            >
              <Box sx={{ flexShrink: 0, display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 0.5, width: 60 }}>
                <Typography variant="caption" color="text.secondary">#{idx + 1}</Typography>
                <Chip
                  label={SCRIPT_TYPE_LABELS[item.scriptType] ?? item.scriptType}
                  size="small"
                  color={SCRIPT_TYPE_COLORS[item.scriptType] ?? 'default'}
                  variant="outlined"
                />
              </Box>
              <TextField
                label="摘要"
                value={item.summary}
                onChange={(e) => handleSummaryChange(item.scriptId, e.target.value)}
                size="small"
                multiline
                maxRows={3}
                sx={{ flex: 1 }}
              />
              <TextField
                label="时长(秒)"
                type="number"
                value={item.suggestedDurationSec}
                onChange={(e) => handleDurationChange(item.scriptId, Math.max(0, parseInt(e.target.value) || 0))}
                size="small"
                sx={{ width: 100 }}
                inputProps={{ min: 0, max: 600 }}
              />
            </Box>
          ))}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button onClick={onClose}>取消</Button>
        <Button
          variant="contained"
          onClick={() => onConfirm(editedItems)}
          disabled={editedItems.length === 0}
        >
          确认并生成 ({editedItems.length} 段)
        </Button>
      </DialogActions>
    </Dialog>
  )
})
