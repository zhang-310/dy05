/**
 * 话术流程画布 — React Flow 实现
 * Phase 2：节点连线、缩放平移、选中联动
 */
import { useState, useMemo, useCallback, useEffect, useRef } from 'react'
import {
  ReactFlow,
  Background,
  Controls,
  MiniMap,
  useNodesState,
  useEdgesState,
  type Node,
  type Edge,
} from '@xyflow/react'
import '@xyflow/react/dist/style.css'
import { Box, Typography, Button } from '@mui/material'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import type { LiveScript, LiveProduct } from '@/api/live'
import ScriptFlowNode, { type ScriptFlowNodeData } from './ScriptFlowNode'
import { SCRIPT_TYPE_LABEL } from './constants'

const NODE_HEIGHT = 80
const NODE_HEIGHT_COLLAPSED = 48
const GAP = 40

const nodeTypes = { scriptFlow: ScriptFlowNode }

function scriptsToNodesAndEdges(
  scripts: LiveScript[],
  sortedProducts: LiveProduct[],
  selectedId: number | null,
  collapsedScriptIds: Set<number>,
  toggleCollapse: (scriptId: number) => void
): { nodes: Node[]; edges: Edge[] } {
  const sorted = [...scripts].sort((a, b) => ((a.sequenceNo as number) ?? 0) - ((b.sequenceNo as number) ?? 0))
  const productMap = new Map(sortedProducts.map((p) => [p.productId, p]))

  const getLabel = (s: LiveScript, idx: number): string => {
    switch (s.scriptType) {
      case 'opening': return '开场'
      case 'closing': return '结尾'
      case 'product':
        if (s.productId) {
          const p = productMap.get(s.productId)
          return p?.productName ?? `产品 ${s.productId}`
        }
        return '产品'
      case 'transition': {
        if (idx <= 0) return '衔接'
        const prev = sorted[idx - 1]
        if (prev?.scriptType === 'product' && prev.productId) {
          const next = sorted[idx + 1]
          const from = productMap.get(prev.productId)?.productName ?? 'A'
          const to = next?.scriptType === 'product' && next.productId
            ? (productMap.get(next.productId)?.productName ?? 'B')
            : 'B'
          return `衔接 ${from} → ${to}`
        }
        return '衔接'
      }
      default: return SCRIPT_TYPE_LABEL[s.scriptType as string] ?? s.scriptType ?? '话术'
    }
  }

  let y = 0
  const nodes: Node[] = sorted.map((s, idx) => {
    const content = (s.scriptContent as string) ?? ''
    const preview = content.length > 50 ? content.slice(0, 50) + '…' : content
    const collapsed = collapsedScriptIds.has(s.id as number)
    const h = collapsed ? NODE_HEIGHT_COLLAPSED : NODE_HEIGHT
    const node: Node = {
      id: `script-${s.id}`,
      type: 'scriptFlow',
      position: { x: 20, y },
      data: {
        scriptId: s.id as number,
        scriptType: s.scriptType ?? '',
        label: getLabel(s, idx),
        preview: preview || '[待填写]',
        selected: selectedId === s.id,
        collapsed,
        onCollapseToggle: () => toggleCollapse(s.id as number),
      },
    }
    y += h + GAP
    return node
  })

  const edges: Edge[] = []
  for (let i = 0; i < nodes.length - 1; i++) {
    edges.push({
      id: `e-${nodes[i].id}-${nodes[i + 1].id}`,
      source: nodes[i].id,
      target: nodes[i + 1].id,
      animated: selectedId != null && nodes[i + 1].data?.scriptId === selectedId,
    })
  }

  return { nodes, edges }
}

export interface ScriptFlowCanvasProps {
  scripts: LiveScript[]
  sortedProducts: LiveProduct[]
  selectedId: number | null
  onSelect: (script: LiveScript) => void
  /** Phase 3：有产品无话术时，显示一键生成按钮 */
  onGenerateFull?: () => void
}

const LARGE_LIST_THRESHOLD = 25
export function ScriptFlowCanvas({ scripts, sortedProducts, selectedId, onSelect, onGenerateFull }: ScriptFlowCanvasProps) {
  const [collapsedScriptIds, setCollapsedScriptIds] = useState<Set<number>>(() => {
    if (scripts.length <= LARGE_LIST_THRESHOLD) return new Set()
    const ids = new Set<number>()
    scripts.forEach((s) => {
      const id = s.id as number
      if (id != null && id !== selectedId) ids.add(id)
    })
    return ids
  })
  const prevCountRef = useRef(scripts.length)
  useEffect(() => {
    if (scripts.length > LARGE_LIST_THRESHOLD && prevCountRef.current <= LARGE_LIST_THRESHOLD) {
      const ids = new Set<number>()
      scripts.forEach((s) => {
        const id = s.id as number
        if (id != null && id !== selectedId) ids.add(id)
      })
      setCollapsedScriptIds(ids)
    }
    prevCountRef.current = scripts.length
  }, [scripts.length, scripts, selectedId])
  const toggleCollapse = useCallback((scriptId: number) => {
    setCollapsedScriptIds((prev: Set<number>) => {
      const next = new Set(prev)
      if (next.has(scriptId)) next.delete(scriptId)
      else next.add(scriptId)
      return next
    })
  }, [])

  const { nodes: initialNodes, edges: initialEdges } = useMemo(
    () => scriptsToNodesAndEdges(scripts, sortedProducts, selectedId, collapsedScriptIds, toggleCollapse),
    [scripts, sortedProducts, selectedId, collapsedScriptIds, toggleCollapse]
  )

  const [nodes, setNodes, onNodesChange] = useNodesState(initialNodes)
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialEdges)

  useEffect(() => {
    const { nodes: nextNodes, edges: nextEdges } = scriptsToNodesAndEdges(scripts, sortedProducts, selectedId, collapsedScriptIds, toggleCollapse)
    setNodes(nextNodes)
    setEdges(nextEdges)
  }, [scripts, sortedProducts, selectedId, collapsedScriptIds, toggleCollapse, setNodes, setEdges])

  const onNodeClick = useCallback(
    (_: React.MouseEvent, node: Node) => {
      const scriptId = (node.data as ScriptFlowNodeData)?.scriptId
      if (scriptId != null) {
        const script = scripts.find((s) => s.id === scriptId)
        if (script) onSelect(script)
      }
    },
    [scripts, onSelect]
  )

  if (scripts.length === 0) {
    return (
      <Box sx={{ p: 4, textAlign: 'center', display: 'flex', flexDirection: 'column', alignItems: 'center', gap: 2 }}>
        <AutoAwesomeIcon sx={{ fontSize: 48, color: 'action.disabled' }} />
        <Typography variant="body1" color="text.secondary">暂无话术，一键生成全场</Typography>
        {onGenerateFull && (
          <Button size="medium" variant="contained" startIcon={<AutoAwesomeIcon />} onClick={onGenerateFull}>
            一键生成
          </Button>
        )}
      </Box>
    )
  }

  return (
    <Box sx={{ width: '100%', height: '100%', minHeight: 400 }}>
      <ReactFlow
        nodes={nodes}
        edges={edges}
        onNodesChange={onNodesChange}
        onEdgesChange={onEdgesChange}
        onNodeClick={onNodeClick}
        nodeTypes={nodeTypes}
        fitView
        fitViewOptions={{ padding: 0.2 }}
        minZoom={0.2}
        maxZoom={1.5}
        defaultViewport={{ x: 0, y: 0, zoom: 0.8 }}
        proOptions={{ hideAttribution: true }}
      >
        <Background />
        <Controls />
        <MiniMap nodeColor={(n) => {
          const d = n.data as ScriptFlowNodeData
          return d?.scriptType === 'opening' ? '#1976d2' : d?.scriptType === 'closing' ? '#9c27b0' : d?.scriptType === 'transition' ? '#ed6c02' : '#2e7d32'
        }} />
      </ReactFlow>
    </Box>
  )
}
