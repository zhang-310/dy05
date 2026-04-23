
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Button,
  IconButton,
} from '@mui/material'
import CloseIcon from '@mui/icons-material/Close'

interface FormDialogProps {
  open: boolean
  title: string
  onClose: () => void
  onConfirm: () => void
  confirmText?: string
  loading?: boolean
  maxWidth?: 'xs' | 'sm' | 'md' | 'lg' | 'xl'
  children: React.ReactNode
}

/** 表单对话框：使用 MUI 默认主题样式 */
export function FormDialog({
  open,
  title,
  onClose,
  onConfirm,
  confirmText = '保存',
  loading = false,
  maxWidth = 'sm',
  children,
}: FormDialogProps) {
  return (
    <Dialog open={open} onClose={onClose} maxWidth={maxWidth} fullWidth>
      <DialogTitle
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 1,
          pr: 1,
        }}
      >
        <span>{title}</span>
        <IconButton
          onClick={onClose}
          aria-label="关闭"
          edge="end"
          size="small"
        >
          <CloseIcon />
        </IconButton>
      </DialogTitle>
      <DialogContent dividers>{children}</DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={loading}>
          取消
        </Button>
        <Button
          variant="contained"
          color="primary"
          onClick={onConfirm}
          disabled={loading}
        >
          {loading ? '保存中...' : confirmText}
        </Button>
      </DialogActions>
    </Dialog>
  )
}
