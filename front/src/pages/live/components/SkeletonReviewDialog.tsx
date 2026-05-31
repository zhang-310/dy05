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

const SKELETON_REVIEW_READY_ENDPOINTS = [
  '/live/ai/generate-skeleton-sse',
  '/live/script/save',
  '/live/ai/generate-slot',
]

const SKELETON_REVIEW_UNSUPPORTED_ACTIONS = [
  'direct-network-call',
  'local-skeleton-fallback',
  'local-script-generation',
  'product-mutation',
  'shortvideo-mutation',
]

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
    <Dialog
      open={open}
      onClose={onClose}
      maxWidth="md"
      fullWidth
      data-testid="skeleton-review-dialog"
      data-contract-scope="live-skeleton-review-props-editor"
      data-ready-endpoints={SKELETON_REVIEW_READY_ENDPOINTS.join('|')}
      data-unsupported-actions={SKELETON_REVIEW_UNSUPPORTED_ACTIONS.join('|')}
      data-item-count={editedItems.length}
      data-total-duration-sec={totalDuration}
      data-no-direct-api-request="true"
      data-no-local-skeleton-fallback="true"
    >
      <DialogTitle
        data-testid="skeleton-review-title"
        data-contract-source="/live/ai/generate-skeleton-sse"
      >
        骨架审核
        <Typography variant="caption" color="text.secondary" sx={{ ml: 1 }}>
          共 {editedItems.length} 段 · 预计 {Math.floor(totalDuration / 60)} 分 {totalDuration % 60} 秒
        </Typography>
      </DialogTitle>
      <DialogContent>
        <Typography
          variant="body2"
          color="text.secondary"
          data-testid="skeleton-review-context-copy"
          data-contract-source="/live/ai/generate-skeleton-sse"
          sx={{ mb: 2 }}
        >
          请审核并调整各段骨架摘要和时长，确认后将开始生成完整话术内容。
        </Typography>
        <Box
          data-testid="skeleton-review-list"
          data-contract-source="/live/ai/generate-skeleton-sse"
          data-no-local-skeleton-fallback="true"
          sx={{ display: 'flex', flexDirection: 'column', gap: 2 }}
        >
          {editedItems.length === 0 && (
            <Box
              data-testid="skeleton-review-empty-state"
              data-contract-source="/live/ai/generate-skeleton-sse"
              data-no-local-skeleton-fallback="true"
              sx={{ py: 5, textAlign: 'center', color: 'text.secondary', border: 1, borderStyle: 'dashed', borderColor: 'divider', borderRadius: 1 }}
            >
              <Typography variant="body2">暂无骨架可审核</Typography>
            </Box>
          )}
          {editedItems.map((item, idx) => (
            <Box
              key={item.scriptId}
              data-testid="skeleton-review-row"
              data-contract-source="/live/ai/generate-skeleton-sse"
              data-script-id={item.scriptId}
              data-script-type={item.scriptType}
              data-duration-sec={item.suggestedDurationSec}
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
                  data-testid="skeleton-review-type-chip"
                  data-contract-source="/live/ai/generate-skeleton-sse"
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
                inputProps={{
                  'data-testid': 'skeleton-review-summary-input',
                  'data-contract-source': 'editedItems-local-review-state',
                }}
                sx={{ flex: 1 }}
              />
              <TextField
                label="时长(秒)"
                type="number"
                value={item.suggestedDurationSec}
                onChange={(e) => handleDurationChange(item.scriptId, Math.max(0, parseInt(e.target.value) || 0))}
                size="small"
                sx={{ width: 100 }}
                inputProps={{
                  min: 0,
                  max: 600,
                  'data-testid': 'skeleton-review-duration-input',
                  'data-contract-source': 'editedItems-local-review-state',
                }}
              />
            </Box>
          ))}
        </Box>
      </DialogContent>
      <DialogActions sx={{ px: 3, pb: 2 }}>
        <Button
          onClick={onClose}
          data-testid="skeleton-review-cancel-button"
          data-contract-source="onClose-prop"
        >
          取消
        </Button>
        <Button
          variant="contained"
          onClick={() => onConfirm(editedItems)}
          disabled={editedItems.length === 0}
          data-testid="skeleton-review-confirm-button"
          data-contract-source="/live/script/save|/live/ai/generate-slot"
          data-action-owner="onConfirm-prop"
          data-disabled-reason={editedItems.length === 0 ? 'no-skeleton-items' : 'ready'}
        >
          确认并生成 ({editedItems.length} 段)
        </Button>
      </DialogActions>
    </Dialog>
  )
})
