import { memo } from 'react'
import {
  Box,
  LinearProgress,
  Tooltip,
  Typography,
  IconButton,
} from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import CloseIcon from '@mui/icons-material/Close'
import ExpandMoreIcon from '@mui/icons-material/ExpandMore'
import ExpandLessIcon from '@mui/icons-material/ExpandLess'
import type { FlowStep } from './types'

export interface FlowProgressHeaderProps {
  steps: FlowStep[]
  modelName?: string
  isGenerating?: boolean
  isInline: boolean
  sseReconnecting?: boolean
  onClose?: () => void
  onRetryAllFailed?: () => void
  expandedKeys: Set<string>
  onExpandAll: () => void
  onCollapseAll: () => void
  /** P1-03: 生成开始时间戳（ms），用于计算预估剩余时间 */
  genStartTimeMs?: number
}

export const FlowProgressHeader = memo(function FlowProgressHeader({
  steps,
  modelName,
  isGenerating,
  isInline,
  sseReconnecting,
  onClose,
  onRetryAllFailed,
  expandedKeys: _expandedKeys,
  onExpandAll,
  onCollapseAll,
  genStartTimeMs,
}: FlowProgressHeaderProps) {
  const doneCount = steps.filter((s) => s.status === 'done').length
  const loadingCount = steps.filter((s) => s.status === 'loading').length
  const failedCount = steps.filter((s) => s.status === 'failed').length
  const pendingCount = steps.filter((s) => s.status === 'pending').length
  const allFinished = steps.length > 0 && doneCount + failedCount === steps.length && !isGenerating

  const qPass = steps.filter((s) => s.qualityScore === 'pass').length
  const qWarn = steps.filter((s) => s.qualityScore === 'warning').length
  const qFail = steps.filter((s) => s.qualityScore === 'fail').length
  const hasQuality = qPass + qWarn + qFail > 0

  const pct = steps.length > 0 ? Math.round((doneCount / steps.length) * 100) : 0

  // P1-03: 预估剩余时间
  const estimatedRemaining = (() => {
    if (!isGenerating || !genStartTimeMs || doneCount === 0 || steps.length === 0) return null
    const elapsed = Date.now() - genStartTimeMs
    const avgPerSlot = elapsed / doneCount
    const remaining = avgPerSlot * (steps.length - doneCount)
    if (remaining < 1000) return null
    const sec = Math.ceil(remaining / 1000)
    return sec >= 60 ? `约 ${Math.ceil(sec / 60)} 分钟` : `约 ${sec} 秒`
  })()

  const hasExpandableSteps = steps.some(
    (s) => (s.status === 'done' || s.status === 'failed') && (s.content || s.errorMsg),
  )

  return (
    <>
      {/* Title bar */}
      <Box
        sx={{
          px: 1,
          py: 0.25,
          borderBottom: 1,
          borderColor: 'divider',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          gap: 0.5,
          flexShrink: 0,
          minHeight: 26,
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5 }}>
          <AutoAwesomeIcon color="primary" sx={{ fontSize: 13 }} />
          <Typography variant="body2" fontWeight={600} sx={{ fontSize: 11.5, lineHeight: 1 }}>
            {(() => {
              if (allFinished && failedCount > 0)
                return (
                  <Box component="span" sx={{ color: 'error.main' }}>
                    生成详情 · {failedCount}失败
                  </Box>
                )
              if (allFinished && failedCount === 0)
                return (
                  <Box component="span" sx={{ color: 'success.main' }}>
                    完成
                  </Box>
                )
              return '生成详情'
            })()}
          </Typography>
          {modelName && (
            <Typography variant="caption" color="text.disabled" sx={{ fontSize: 10.5 }}>
              {modelName}
            </Typography>
          )}
        </Box>

        {/* Expand/collapse all */}
        {hasExpandableSteps && (
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0 }}>
            <Tooltip title="全部展开">
              <IconButton size="small" sx={{ p: 0.25 }} onClick={onExpandAll}>
                <ExpandMoreIcon sx={{ fontSize: 16 }} />
              </IconButton>
            </Tooltip>
            <Tooltip title="全部收缩">
              <IconButton size="small" sx={{ p: 0.25 }} onClick={onCollapseAll}>
                <ExpandLessIcon sx={{ fontSize: 16 }} />
              </IconButton>
            </Tooltip>
          </Box>
        )}

        {!isInline && onClose && (
          <IconButton size="small" onClick={onClose} title="收起" sx={{ p: 0.25 }}>
            <CloseIcon sx={{ fontSize: 16 }} />
          </IconButton>
        )}
      </Box>

      {/* Statistics summary */}
      {steps.length > 0 && (
        <Box sx={{ px: 1, py: 0.25, borderBottom: 1, borderColor: 'divider', flexShrink: 0 }}>
          <Box sx={{ display: 'flex', alignItems: 'center', gap: 0.5, mb: 0.125, flexWrap: 'wrap' }}>
            <Typography variant="caption" sx={{ fontSize: '0.75rem', fontWeight: 600, color: 'text.primary' }}>
              {doneCount}
              <Typography component="span" variant="caption" sx={{ fontSize: '0.7rem', color: 'success.main', ml: 0.25 }}>
                完成
              </Typography>
            </Typography>
            {loadingCount > 0 && (
              <Typography variant="caption" sx={{ fontSize: '0.75rem', fontWeight: 600, color: 'text.primary' }}>
                {loadingCount}
                <Typography component="span" variant="caption" sx={{ fontSize: '0.7rem', color: 'primary.main', ml: 0.25 }}>
                  生成中
                </Typography>
              </Typography>
            )}
            {pendingCount > 0 && (
              <Typography variant="caption" sx={{ fontSize: '0.75rem', fontWeight: 600, color: 'text.primary' }}>
                {pendingCount}
                <Typography component="span" variant="caption" sx={{ fontSize: '0.7rem', color: 'text.disabled', ml: 0.25 }}>
                  等待
                </Typography>
              </Typography>
            )}
            {failedCount > 0 && (
              <Typography variant="caption" sx={{ fontSize: '0.75rem', fontWeight: 600, color: 'text.primary' }}>
                {failedCount}
                <Typography component="span" variant="caption" sx={{ fontSize: '0.7rem', color: 'error.main', ml: 0.25 }}>
                  失败
                </Typography>
              </Typography>
            )}
            {failedCount > 0 && !isGenerating && onRetryAllFailed && (
              <Typography
                component="span"
                variant="caption"
                sx={{
                  fontSize: '0.7rem',
                  color: 'primary.main',
                  cursor: 'pointer',
                  ml: 0.25,
                  '&:hover': { textDecoration: 'underline' },
                }}
                onClick={onRetryAllFailed}
              >
                重试失败
              </Typography>
            )}
            {/* P1-03: 预估剩余时间 */}
            {estimatedRemaining && isGenerating && (
              <Typography variant="caption" sx={{ fontSize: '0.7rem', color: 'text.secondary', ml: 'auto' }}>
                {estimatedRemaining}
              </Typography>
            )}
            {/* 完成时显示百分比 */}
            {!isGenerating && steps.length > 0 && (
              <Typography variant="caption" sx={{ fontSize: '0.7rem', color: pct === 100 ? 'success.main' : 'text.secondary', ml: 'auto', fontWeight: 600 }}>
                {pct}%
              </Typography>
            )}
            {hasQuality && (
              <>
                <Box sx={{ width: 1, height: 10, bgcolor: 'divider', mx: 0.25 }} />
                <Typography variant="caption" sx={{ fontSize: '0.7rem', color: 'text.secondary', fontWeight: 500 }}>
                  质检
                </Typography>
                {qPass > 0 && (
                  <Typography variant="caption" sx={{ fontSize: '0.7rem', color: 'success.main' }}>
                    {qPass}通过
                  </Typography>
                )}
                {qWarn > 0 && (
                  <Typography variant="caption" sx={{ fontSize: '0.7rem', color: 'warning.main' }}>
                    {qWarn}警告
                  </Typography>
                )}
                {qFail > 0 && (
                  <Typography variant="caption" sx={{ fontSize: '0.7rem', color: 'error.main' }}>
                    {qFail}失败
                  </Typography>
                )}
              </>
            )}
          </Box>
          <LinearProgress variant="determinate" value={pct} sx={{ height: 3, borderRadius: 1.5 }} />
        </Box>
      )}

      {/* SSE reconnecting banner */}
      {sseReconnecting && (
        <Box
          sx={{
            px: 2,
            py: 0.75,
            bgcolor: 'warning.light',
            color: 'warning.contrastText',
            display: 'flex',
            alignItems: 'center',
            gap: 1,
            flexShrink: 0,
          }}
        >
          <Box
            component="span"
            sx={{ fontSize: 16, display: 'inline-flex', animation: 'spin 1s linear infinite' }}
          >
            &#x21BB;
          </Box>
          <Typography variant="caption" fontWeight={500}>
            连接中断，正在重连...
          </Typography>
        </Box>
      )}
    </>
  )
})
