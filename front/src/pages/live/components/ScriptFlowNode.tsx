/**
 * 话术流程节点 — 用于 React Flow 画布
 * 支持 opening / product / transition / closing 四种类型
 * Phase 3：节点折叠，紧凑样式仅显示 label
 */
import { memo } from 'react'
import { Handle, Position, type NodeProps } from '@xyflow/react'
import { Box, Typography, IconButton, Tooltip } from '@mui/material'
import { alpha } from '@mui/material/styles'
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

const TYPE_COLORS: Record<string, { color: 'primary' | 'secondary' | 'success' | 'warning' | 'grey' }> = {
  opening: { color: 'primary' },
  closing: { color: 'secondary' },
  product: { color: 'success' },
  transition: { color: 'warning' },
  default: { color: 'grey' },
}

function ScriptFlowNode(props: NodeProps) {
  const data = props.data as ScriptFlowNodeData
  const type = data?.scriptType ?? 'default'
  const colors = TYPE_COLORS[type] ?? TYPE_COLORS.default
  const selected = data?.selected === true
  const collapsed = data?.collapsed === true
  const scriptId = data?.scriptId ?? ''
  const preview = (data?.preview as string) || '[待填写]'

  const handleCollapseClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    data?.onCollapseToggle?.()
  }

  return (
    <Box
      data-testid={selected ? 'script-flow-node-selected-surface' : 'script-flow-node-surface'}
      data-contract-scope="live-script-flow-node-props-renderer"
      data-contract-source="react-flow-node-data"
      data-script-id={scriptId}
      data-script-type={type}
      data-selected={selected ? 'true' : 'false'}
      data-collapsed={collapsed ? 'true' : 'false'}
      data-has-preview={preview.trim() && preview !== '[待填写]' ? 'true' : 'false'}
      data-has-collapse-toggle={data?.onCollapseToggle ? 'true' : 'false'}
      data-no-direct-api="true"
      sx={(theme) => {
        const semanticColor = colors.color === 'grey'
          ? theme.palette.text.secondary
          : theme.palette[colors.color].main
        const surfaceAlpha = selected
          ? (theme.palette.mode === 'dark' ? 0.2 : 0.12)
          : (theme.palette.mode === 'dark' ? 0.14 : 0.08)
        return {
        px: 2,
        py: collapsed ? 0.75 : 1.5,
        borderRadius: 2,
        border: 2,
        borderColor: selected ? 'primary.main' : alpha(semanticColor, 0.55),
        bgcolor: alpha(selected ? theme.palette.primary.main : semanticColor, surfaceAlpha),
        minWidth: collapsed ? 120 : 160,
        maxWidth: 220,
        boxShadow: selected ? 3 : 1,
      }}}
    >
      <Handle type="target" position={Position.Top} />
      <Box sx={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: 0.5 }}>
        <Typography variant="subtitle2" fontWeight={600} noWrap sx={{ flex: 1, minWidth: 0 }}>
          {data?.label ?? '-'}
        </Typography>
        {data?.onCollapseToggle && (
          <Tooltip title={collapsed ? '展开' : '折叠'}>
            <IconButton
              data-testid="script-flow-node-collapse-button"
              data-contract-source="onCollapseToggle-prop"
              data-action={collapsed ? 'expand' : 'collapse'}
              data-script-id={scriptId}
              size="small"
              onClick={handleCollapseClick}
              sx={{ p: 0.25 }}
            >
              {collapsed ? <UnfoldMoreIcon sx={{ fontSize: 16 }} /> : <UnfoldLessIcon sx={{ fontSize: 16 }} />}
            </IconButton>
          </Tooltip>
        )}
      </Box>
      {!collapsed && (
        <Typography
          data-testid="script-flow-node-preview"
          data-contract-source="scriptContent-prop"
          variant="caption"
          color="text.secondary"
          sx={{ display: 'block', mt: 0.5 }}
          noWrap
        >
          {preview}
        </Typography>
      )}
      <Handle type="source" position={Position.Bottom} />
    </Box>
  )
}

export default memo(ScriptFlowNode)
