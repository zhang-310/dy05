import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { alpha, Box, useTheme } from '@mui/material'
import type { Theme } from '@mui/material/styles'

type WorkflowStatusTone = 'default' | 'primary' | 'success' | 'error'

function semanticColor(theme: Theme, tone: Exclude<WorkflowStatusTone, 'default'>) {
  return theme.palette.mode === 'dark' ? theme.palette[tone].light : theme.palette[tone].main
}

function statusStyle(theme: Theme, status: string, selected: boolean) {
  const tone: WorkflowStatusTone =
    status === 'processing'
      ? 'primary'
      : status === 'completed'
        ? 'success'
        : status === 'failed'
          ? 'error'
          : 'default'

  if (selected) {
    const color = semanticColor(theme, 'primary')
    return {
      tone,
      border: color,
      bg: alpha(color, theme.palette.mode === 'dark' ? 0.2 : 0.12),
    }
  }

  if (tone === 'default') {
    return {
      tone,
      border: theme.palette.divider,
      bg: theme.palette.background.paper,
    }
  }

  const color = semanticColor(theme, tone)
  return {
    tone,
    border: color,
    bg: alpha(color, theme.palette.mode === 'dark' ? 0.16 : 0.08),
  }
}

interface WorkflowNodeData {
  status?: string
  selected?: boolean
  icon?: string
  label?: string
  model?: unknown
  [key: string]: unknown
}

function WorkflowNode(props: NodeProps) {
  const theme = useTheme()
  const data = props.data as WorkflowNodeData
  const status = data?.status ?? 'idle'
  const selected = data?.selected === true
  const style = statusStyle(theme, status, selected)

  return (
    <Box
      data-testid="workflow-node-surface"
      data-workflow-status={status}
      data-workflow-tone={style.tone}
      data-workflow-border={style.border}
      sx={{
        px: 2,
        py: 1.5,
        borderRadius: 2,
        border: `2px solid ${style.border}`,
        boxShadow: selected ? 4 : 2,
        bgcolor: style.bg,
        minWidth: 140,
      }}
    >
      <Handle type="target" position={Position.Left} />
      <Box sx={{ textAlign: 'center' }}>
        <Box sx={{ fontSize: 24, mb: 0.5 }}>{data?.icon ?? '📦'}</Box>
        <Box component="span" sx={{ fontSize: 13, fontWeight: 600 }}>
          {data?.label ?? ''}
        </Box>
        <Box sx={{ fontSize: 11, color: 'text.secondary', mt: 0.5 }}>
          {status === 'processing' ? '执行中...' : status === 'completed' ? '已完成' : '待执行'}
        </Box>
        {data?.model != null && (
          <Box sx={{ fontSize: 10, color: 'text.disabled', mt: 0.5 }}>模型: {String(data.model)}</Box>
        )}
      </Box>
      <Handle type="source" position={Position.Right} />
    </Box>
  )
}

export default memo(WorkflowNode)
