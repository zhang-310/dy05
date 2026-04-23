import { memo } from 'react'
import {
  Box,
  Button,
  LinearProgress,
  Tooltip,
  Typography,
} from '@mui/material'
import SaveIcon from '@mui/icons-material/Save'
import DownloadIcon from '@mui/icons-material/Download'
import type { FlowStep } from './types'

export interface FlowActionBarProps {
  steps: FlowStep[]
  total: number
  isGenerating?: boolean
  estimatedTotalSeconds?: number
  cancellingGen: boolean
  saveLibLoading: boolean
  exportLoading: boolean
  onCancel?: () => void
  onClose?: () => void
  onDismiss?: () => void
  onSaveToLibrary?: () => void
  onExport?: () => void
}

export const FlowActionBar = memo(function FlowActionBar({
  steps,
  total,
  isGenerating,
  estimatedTotalSeconds,
  cancellingGen,
  saveLibLoading,
  exportLoading,
  onCancel,
  onClose,
  onDismiss,
  onSaveToLibrary,
  onExport,
}: FlowActionBarProps) {
  const doneCount = steps.filter((s) => s.status === 'done').length
  const failedCount = steps.filter((s) => s.status === 'failed').length
  const hasDoneSteps = steps.some((s) => s.status === 'done')

  return (
    <>
      {/* Generating: cancel button */}
      {isGenerating && onCancel && (
        <Box
          sx={{
            px: 1.5,
            py: 0.75,
            borderTop: 1,
            borderColor: 'divider',
            flexShrink: 0,
            display: 'flex',
            gap: 1,
            justifyContent: 'flex-end',
            alignItems: 'center',
          }}
        >
          <Button
            variant="contained"
            color="warning"
            size="small"
            onClick={onCancel}
            disabled={cancellingGen}
            sx={{ fontSize: 12, minWidth: 90 }}
          >
            {cancellingGen ? '取消中...' : '取消生成'}
          </Button>
          {onClose && (
            <Button variant="outlined" size="small" onClick={onClose} sx={{ fontSize: 12 }}>
              收起
            </Button>
          )}
        </Box>
      )}

      {/* Complete: save/export actions */}
      {!isGenerating && hasDoneSteps && (onSaveToLibrary || onExport) && (
        <Box
          sx={{
            px: 1.5,
            py: 0.75,
            borderTop: 1,
            borderColor: 'divider',
            flexShrink: 0,
            display: 'flex',
            gap: 0.75,
            alignItems: 'center',
          }}
        >
          {onSaveToLibrary && (
            <Tooltip title="保存后可到文案库复用">
              <span>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<SaveIcon sx={{ fontSize: '14px !important' }} />}
                  onClick={onSaveToLibrary}
                  disabled={saveLibLoading}
                  sx={{ fontSize: 12 }}
                >
                  {saveLibLoading ? '保存中...' : '保存到话术库'}
                </Button>
              </span>
            </Tooltip>
          )}
          {onExport && (
            <Tooltip title="导出为 Markdown 格式">
              <span>
                <Button
                  size="small"
                  variant="outlined"
                  startIcon={<DownloadIcon sx={{ fontSize: '14px !important' }} />}
                  onClick={onExport}
                  disabled={exportLoading}
                  sx={{ fontSize: 12 }}
                >
                  {exportLoading ? '导出中...' : '导出'}
                </Button>
              </span>
            </Tooltip>
          )}
          <Box sx={{ flex: 1 }} />
          {onDismiss && (
            <Button size="small" variant="text" color="inherit" onClick={onDismiss}>
              关闭
            </Button>
          )}
        </Box>
      )}

      {/* Bottom progress bar with total */}
      {total > 0 && (
        <Box sx={{ px: 1.5, py: 0.75, borderTop: 1, borderColor: 'divider', flexShrink: 0 }}>
          <LinearProgress
            variant="determinate"
            value={(doneCount / total) * 100}
            sx={{ height: 4, borderRadius: 2 }}
          />
          <Typography
            variant="caption"
            color="text.secondary"
            sx={{ display: 'block', mt: 0.5, fontSize: 11 }}
          >
            {doneCount} / {total} 已完成
            {failedCount > 0 ? ` · ${failedCount} 失败` : ''}
          </Typography>
          {isGenerating &&
            estimatedTotalSeconds != null &&
            estimatedTotalSeconds > 0 &&
            total > 0 &&
            (() => {
              const remaining = total - doneCount
              if (remaining <= 0) return null
              const secPerStep = estimatedTotalSeconds / total
              const remainingSec = Math.round(secPerStep * remaining)
              if (remainingSec < 10) return null
              const mins = Math.floor(remainingSec / 60)
              const secs = remainingSec % 60
              const timeStr = mins > 0 ? `约 ${mins} 分 ${secs > 0 ? `${secs} 秒` : ''}` : `约 ${secs} 秒`
              return (
                <Typography
                  variant="caption"
                  color="text.secondary"
                  sx={{ display: 'block', mt: 0.25, fontSize: 11 }}
                >
                  预计剩余：{timeStr}
                </Typography>
              )
            })()}
        </Box>
      )}
    </>
  )
})
