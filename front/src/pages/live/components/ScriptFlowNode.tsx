/**
 * 话术流程节点 — 用于 React Flow 画布
 * 支持 opening / product / transition / closing 四种类型
 * Phase 3：节点折叠，紧凑样式仅显示 label
 */
import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { Box, Typography, IconButton, Tooltip } from '@mui/material'
import UnfoldLessIcon from '@mui/icons-material/UnfoldLess'
import UnfoldMoreIcon from '@mui/icons-material/UnfoldMore'

export interface ScriptFlowNodeData extends Record<string, unknown> {
  scriptId: number
  scriptType: string
  label: string
  preview?: string
  selected?: boolean
  /** Phase 3：折叠时仅显示标题 */
  collapsed?: boolean
  onCollapseToggle?: () => void
}

const TYPE_COLORS: Record<string, { bg: string; border: string }> = {
  opening: { bg: 'primary.50', border: 'primary.main' },
  closing: { bg: 'secondary.50', border: 'secondary.main' },
  product: { bg: 'success.50', border: 'success.main' },
  transition: { bg: 'warning.50', border: 'warning.main' },
  default: { bg: 'grey.100', border: 'grey.400' },
}

function ScriptFlowNode(props: NodeProps) {
  const data = props.data as ScriptFlowNodeData
  const type = data?.scriptType ?? 'default'
  const colors = TYPE_COLORS[type] ?? TYPE_COLORS.default
  const selected = data?.selected === true
  const collapsed = data?.collapsed === true

  const handleCollapseClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    data?.onCollapseToggle?.()
  }

  return (
    <Box
      sx={{
        px: 2,
        py: collapsed ? 0.75 : 1.5,
        borderRadius: 2,
        border: 2,
        borderColor: selected ? 'primary.main' : colors.border,
        bgcolor: selected ? 'primary.50' : colors.bg,
        minWidth: collapsed ? 120 : 160,
        maxWidth: 220,
        boxShadow: selected ? 3 : 1,
      }}
    >
      <Handle type="target" position={Position.Top} />
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 0.5 }}>
        <Typography variant="subtitle2" fontWeight={600} noWrap sx={{ flex: 1, minWidth: 0 }}>
          {data?.label ?? '-'}
        </Typography>
        {data?.onCollapseToggle && (
          <Tooltip title={collapsed ? '展开' : '折叠'}>
            <IconButton size="small" onClick={handleCollapseClick} sx={{ p: 0.25 }}>
              {collapsed ? <UnfoldMoreIcon sx={{ fontSize: 16 }} /> : <UnfoldLessIcon sx={{ fontSize: 16 }} />}
            </IconButton>
          </Tooltip>
        )}
      </Box>
      {!collapsed && (
        <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mt: 0.5 }} noWrap>
          {(data?.preview as string) || '[待填写]'}
        </Typography>
      )}
      <Handle type="source" position={Position.Bottom} />
    </Box>
  )
}

export default memo(ScriptFlowNode)
