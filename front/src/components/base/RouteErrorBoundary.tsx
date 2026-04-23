import { Component, type ErrorInfo, type ReactNode } from 'react'
import { Box, Button, Typography } from '@mui/material'

interface Props {
  children: ReactNode
}

interface State {
  hasError: boolean
  error?: Error
}

/** 懒加载路由错误边界：chunk 加载失败或子树抛错时避免整页白屏 */
export class RouteErrorBoundary extends Component<Props, State> {
  state: State = { hasError: false }

  static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error }
  }

  override componentDidCatch(error: Error, info: ErrorInfo) {
    console.error('RouteErrorBoundary', error, info.componentStack)
  }

  handleRetry = () => {
    this.setState({ hasError: false, error: undefined })
    window.location.reload()
  }

  override render() {
    if (this.state.hasError) {
      const msg = this.state.error?.message ?? ''
      const isChunkError = msg.includes('Loading chunk') || msg.includes('Failed to fetch dynamically imported module')
      return (
        <Box sx={{ p: 4, textAlign: 'center' }}>
          <Typography variant="h6" gutterBottom>
            {isChunkError ? '页面脚本加载失败，请刷新重试' : '页面出现错误'}
          </Typography>
          {msg && (
            <Typography color="text.secondary" sx={{ mb: 2, wordBreak: 'break-all' }}>
              {msg}
            </Typography>
          )}
          <Button variant="contained" onClick={this.handleRetry}>
            刷新页面
          </Button>
        </Box>
      )
    }
    return this.props.children
  }
}
