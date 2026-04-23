import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Typography, Button, Paper, Stack } from '@mui/material'
import RefreshIcon from '@mui/icons-material/Refresh'
import ContentCopyIcon from '@mui/icons-material/ContentCopy'

interface Props {
  children: ReactNode
  fallback?: ReactNode
  onReset?: () => void
}

interface State {
  hasError: boolean
  error: Error | null
  errorCount: number
  lastErrorMessage: string
  copied: boolean
}

const MAX_SAME_ERROR_COUNT = 3

/** 捕获子组件渲染错误，避免整页白屏 */
export class ErrorBoundary extends Component<Props, State> {
  constructor(props: Props) {
    super(props)
    this.state = { hasError: false, error: null, errorCount: 0, lastErrorMessage: '', copied: false }
  }

  static getDerivedStateFromError(error: Error): Partial<State> {
    return { hasError: true, error }
  }

  componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('[ErrorBoundary]', error, info.componentStack)

    this.setState((prev) => {
      const isSameError = error.message === prev.lastErrorMessage
      return {
        errorCount: isSameError ? prev.errorCount + 1 : 1,
        lastErrorMessage: error.message,
      }
    })
  }

  handleReset = () => {
    this.setState({ hasError: false, error: null, copied: false })
    this.props.onReset?.()
  }

  handleCopyError = () => {
    if (!this.state.error) return
    const errorText = [
      `Error: ${this.state.error.message}`,
      this.state.error.stack ?? '',
    ].join('\n')

    navigator.clipboard.writeText(errorText).then(() => {
      this.setState({ copied: true })
      setTimeout(() => this.setState({ copied: false }), 2000)
    }).catch(() => {
      // Fallback for older browsers
      const textarea = document.createElement('textarea')
      textarea.value = errorText
      document.body.appendChild(textarea)
      textarea.select()
      document.execCommand('copy')
      document.body.removeChild(textarea)
      this.setState({ copied: true })
      setTimeout(() => this.setState({ copied: false }), 2000)
    })
  }

  render() {
    if (this.state.hasError && this.state.error) {
      if (this.props.fallback) return this.props.fallback

      const isDegraded = this.state.errorCount >= MAX_SAME_ERROR_COUNT

      if (isDegraded) {
        return (
          <Paper variant="outlined" sx={{ p: 3, m: 2, textAlign: 'center' }}>
            <Typography variant="h6" color="error" gutterBottom>
              页面无法正常加载
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
              同一错误已连续出现 {this.state.errorCount} 次，组件已降级。
              请刷新页面或联系技术支持。
            </Typography>
            <Typography variant="body2" color="text.secondary" sx={{ mb: 2, fontFamily: 'monospace', wordBreak: 'break-all' }}>
              {this.state.error.message}
            </Typography>
            <Stack direction="row" spacing={1} justifyContent="center">
              <Button
                variant="outlined"
                startIcon={<ContentCopyIcon />}
                onClick={this.handleCopyError}
              >
                {this.state.copied ? '已复制' : '复制错误信息'}
              </Button>
              <Button
                variant="contained"
                onClick={() => window.location.reload()}
              >
                刷新页面
              </Button>
            </Stack>
          </Paper>
        )
      }

      return (
        <Paper variant="outlined" sx={{ p: 3, m: 2, textAlign: 'center' }}>
          <Typography variant="h6" color="error" gutterBottom>
            页面加载出错
          </Typography>
          <Typography variant="body2" color="text.secondary" sx={{ mb: 2, fontFamily: 'monospace', wordBreak: 'break-all' }}>
            {this.state.error.message}
          </Typography>
          {this.state.errorCount > 1 && (
            <Typography variant="caption" color="text.secondary" sx={{ mb: 2, display: 'block' }}>
              该错误已出现 {this.state.errorCount} 次（{MAX_SAME_ERROR_COUNT} 次后将显示降级界面）
            </Typography>
          )}
          <Stack direction="row" spacing={1} justifyContent="center">
            <Button variant="outlined" startIcon={<RefreshIcon />} onClick={this.handleReset}>
              重试
            </Button>
            <Button
              variant="outlined"
              startIcon={<ContentCopyIcon />}
              onClick={this.handleCopyError}
            >
              {this.state.copied ? '已复制' : '复制错误信息'}
            </Button>
          </Stack>
        </Paper>
      )
    }
    return this.props.children
  }
}
