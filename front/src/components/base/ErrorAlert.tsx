import { Alert, AlertTitle, Button, Stack } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'

interface ErrorAlertProps {
  title?: string
  message: string
  onRetry?: () => void
  severity?: 'error' | 'warning' | 'info'
}

export function ErrorAlert({ title = '操作失败', message, onRetry, severity = 'error' }: ErrorAlertProps) {
  return (
    <Alert
      severity={severity}
      action={
        onRetry && (
          <Button
            color="inherit"
            size="small"
            startIcon={<RefreshIcon />}
            onClick={onRetry}
          >
            重试
          </Button>
        )
      }
    >
      {title && <AlertTitle>{title}</AlertTitle>}
      <Stack spacing={1}>
        <div>{message}</div>
      </Stack>
    </Alert>
  )
}
