
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogContentText,
  DialogActions,
  Button,
} from '@mui/material'

interface ConfirmDialogProps {
  open: boolean
  title?: string
  content?: string
  onClose: () => void
  onConfirm: () => void
  loading?: boolean
}

/** 确认对话框：使用 MUI 默认主题样式 */
export function ConfirmDialog({
  open,
  title = '确认操作',
  content = '确定要执行此操作吗？',
  onClose,
  onConfirm,
  loading = false,
}: ConfirmDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{content}</DialogContentText>
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>
          取消
        </Button>
        <Button
          color="error"
          variant="contained"
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? '处理中...' : '确认'}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
