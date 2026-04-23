import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { Box } from '@mui/material'

const statusStyles: Record<string, { border: string; bg: string }> = {
  idle: { border: '#e0e0e0', bg: '#fff' },
  processing: { border: '#1976d2', bg: 'rgba(25, 118, 210, 0.08)' },
  completed: { border: '#2e7d32', bg: 'rgba(46, 125, 50, 0.08)' },
  failed: { border: '#d32f2f', bg: 'rgba(211, 47, 47, 0.08)' },
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
  const data = props.data as WorkflowNodeData
  const status = data?.status ?? 'idle'
  const selected = data?.selected === true
  const style = statusStyles[status] ?? statusStyles.idle

  return (
    <Box
      sx={{
        px: 2,
        py: 1.5,
        borderRadius: 2,
        border: `2px solid ${selected ? '#1976d2' : style.border}`,
        boxShadow: selected ? 4 : 2,
        bgcolor: selected ? 'rgba(25, 118, 210, 0.12)' : style.bg,
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
