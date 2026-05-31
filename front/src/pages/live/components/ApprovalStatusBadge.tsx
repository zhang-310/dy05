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

const APPROVAL_STATUS_READY_ENDPOINTS = ['/live/script-approval/submit', '/live/script-approval/revoke']

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
    <Box
      sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}
      data-testid="approval-status-badge-root"
      data-contract-scope="live-script-approval-status-props"
      data-ready-endpoints={APPROVAL_STATUS_READY_ENDPOINTS.join('|')}
      data-no-direct-api-request="true"
      data-approval-status={approvalStatus}
      data-can-submit={(approvalStatus === 0 || approvalStatus === 3) && onSubmit ? 'true' : 'false'}
      data-can-revoke={approvalStatus === 1 && onRevoke ? 'true' : 'false'}
      data-submit-loading={submitLoading ? 'true' : 'false'}
      data-revoke-loading={revokeLoading ? 'true' : 'false'}
    >
      <Chip
        data-testid="approval-status-chip"
        data-contract-source="approvalStatus-prop"
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
            data-testid="approval-submit-open-button"
            data-contract-source="/live/script-approval/submit|onSubmit-prop"
            data-disabled-reason={submitLoading ? 'submit-loading' : 'ready'}
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
            data-testid="approval-revoke-button"
            data-contract-source="/live/script-approval/revoke|onRevoke-prop"
            data-disabled-reason={revokeLoading ? 'revoke-loading' : 'ready'}
            sx={{ minWidth: 0, px: 0.5, fontSize: '0.7rem' }}
          >
            撤回
          </Button>
        </Tooltip>
      )}

      {/* 提交审核弹窗 */}
      <Dialog
        open={submitOpen}
        onClose={() => setSubmitOpen(false)}
        maxWidth="xs"
        fullWidth
        data-testid="approval-submit-dialog"
        data-contract-scope="live-script-approval-submit-dialog"
        data-contract-source="/live/script-approval/submit|onSubmit-prop"
        data-no-direct-api-request="true"
        data-comment-length={comments.length}
      >
        <DialogTitle data-testid="approval-submit-title" data-contract-source="/live/script-approval/submit">提交话术审核</DialogTitle>
        <DialogContent data-testid="approval-submit-content" data-contract-source="/live/script-approval/submit">
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
            inputProps={{
              'data-testid': 'approval-submit-comment-input',
              'data-contract-source': 'local-comment-state',
            }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setSubmitOpen(false)} data-testid="approval-submit-cancel-button" data-contract-source="close-submit-dialog">取消</Button>
          <Button
            variant="contained"
            disabled={submitLoading}
            data-testid="approval-submit-confirm-button"
            data-contract-source="/live/script-approval/submit|onSubmit-prop"
            data-disabled-reason={submitLoading ? 'submit-loading' : 'ready'}
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
