import { memo, useState } from 'react'
import {
  Box,
  Button,
  Chip,
  Dialog,
  DialogActions,
  DialogContent,
  DialogTitle,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material'
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline'
import HourglassEmptyIcon from '@mui/icons-material/HourglassEmpty'
import CancelOutlinedIcon from '@mui/icons-material/CancelOutlined'
import SendIcon from '@mui/icons-material/Send'
import UndoIcon from '@mui/icons-material/Undo'

const STATUS_MAP: Record<number, { label: string; color: 'default' | 'warning' | 'success' | 'error'; icon: React.ReactNode }> = {
  0: { label: '草稿', color: 'default', icon: null },
  1: { label: '待审核', color: 'warning', icon: <HourglassEmptyIcon sx={{ fontSize: 14 }} /> },
  2: { label: '已通过', color: 'success', icon: <CheckCircleOutlineIcon sx={{ fontSize: 14 }} /> },
  3: { label: '已拒绝', color: 'error', icon: <CancelOutlinedIcon sx={{ fontSize: 14 }} /> },
}

export interface ApprovalStatusBadgeProps {
  approvalStatus: number
  onSubmit?: (comments: string) => Promise<void>
  onRevoke?: () => Promise<void>
  submitLoading?: boolean
  revokeLoading?: boolean
}

export const ApprovalStatusBadge = memo(function ApprovalStatusBadge({
  approvalStatus,
  onSubmit,
  onRevoke,
  submitLoading,
  revokeLoading,
}: ApprovalStatusBadgeProps) {
  const [submitOpen, setSubmitOpen] = useState(false)
  const [comments, setComments] = useState('')

  const status = STATUS_MAP[approvalStatus] ?? STATUS_MAP[0]

  return (
    <Box sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}>
      <Chip
        label={status.label}
        color={status.color}
        size="small"
        icon={status.icon as React.ReactElement | undefined}
        variant="outlined"
        sx={{ height: 22, fontSize: '0.7rem' }}
      />

      {/* 草稿或已拒绝 → 可提交审核 */}
      {(approvalStatus === 0 || approvalStatus === 3) && onSubmit && (
        <Tooltip title="提交审核">
          <Button
            size="small"
            variant="text"
            startIcon={<SendIcon sx={{ fontSize: 14 }} />}
            onClick={() => setSubmitOpen(true)}
            disabled={submitLoading}
            sx={{ minWidth: 0, px: 0.5, fontSize: '0.7rem' }}
          >
            提审
          </Button>
        </Tooltip>
      )}

      {/* 待审核 → 可撤回 */}
      {approvalStatus === 1 && onRevoke && (
        <Tooltip title="撤回审核">
          <Button
            size="small"
            variant="text"
            color="warning"
            startIcon={<UndoIcon sx={{ fontSize: 14 }} />}
            onClick={async () => { await onRevoke?.() }}
            disabled={revokeLoading}
            sx={{ minWidth: 0, px: 0.5, fontSize: '0.7rem' }}
          >
            撤回
          </Button>
        </Tooltip>
      )}

      {/* 提交审核弹窗 */}
      <Dialog open={submitOpen} onClose={() => setSubmitOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>提交话术审核</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
            提交后将进入审核队列，管理员审批通过后方可用于直播。
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={2}
            label="备注（可选）"
            value={comments}
            onChange={(e) => setComments(e.target.value)}
            size="small"
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSubmitOpen(false)}>取消</Button>
          <Button
            variant="contained"
            disabled={submitLoading}
            onClick={async () => {
              await onSubmit?.(comments)
              setSubmitOpen(false)
              setComments('')
            }}
          >
            提交
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
})
