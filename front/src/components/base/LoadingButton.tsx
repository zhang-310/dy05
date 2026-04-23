import { Button, CircularProgress, type ButtonProps } from '@mui/material'

interface LoadingButtonProps extends ButtonProps {
  loading?: boolean
  loadingText?: string
}

export function LoadingButton({
  loading,
  loadingText,
  children,
  disabled,
  startIcon,
  ...props
}: LoadingButtonProps) {
  return (
    <Button
      {...props}
      disabled={loading || disabled}
      startIcon={loading ? <CircularProgress size={20} color="inherit" /> : startIcon}
    >
      {loading && loadingText ? loadingText : children}
    </Button>
  )
}
