import { memo, createContext, useContext } from 'react'
import {
  Handle,
  Position,
  type Node,
  type Edge,
  type NodeTypes,
  type DefaultEdgeOptions,
} from '@xyflow/react'
import {
  Box,
  Typography,
  Card,
  IconButton,
  Chip,
  Tooltip,
} from '@mui/material'
import EditIcon from '@mui/icons-material/Edit'
import RefreshIcon from '@mui/icons-material/Refresh'
import PlayCircleIcon from '@mui/icons-material/PlayCircle'
import ShoppingBagIcon from '@mui/icons-material/ShoppingBag'
import SwapHorizIcon from '@mui/icons-material/SwapHoriz'
import FavoriteIcon from '@mui/icons-material/Favorite'
import StopCircleIcon from '@mui/icons-material/StopCircle'
import DeleteIcon from '@mui/icons-material/Delete'
import SaveIcon from '@mui/icons-material/Save'
import PsychologyIcon from '@mui/icons-material/Psychology'
import AutoAwesomeIcon from '@mui/icons-material/AutoAwesome'
import CheckCircleIcon from '@mui/icons-material/CheckCircle'
import { estimateDurationFromText } from '@/utils/script'
import { cdnThumb } from '@/utils/cdnImage'
import { SCRIPT_TYPE_LABEL, parseProductTypes, PRODUCT_TYPE_OPTIONS } from '../constants'
import type { ScriptSectionProps } from '../ScriptSection'
import type { ScriptSectionData } from '../ScriptPanel'
import { ScriptEditCard } from './ScriptFlowConfig'

/* ══════════════════════════════════════
   Layout constants
   ══════════════════════════════════════ */
const SECTION_HEADER_W = 260
const PRODUCT_HEADER_W = 340
const TRUNK_CENTER_X = PRODUCT_HEADER_W / 2
const SCRIPT_X = 420
const SCRIPT_CARD_H = 120
const SCRIPT_EDIT_H = 220
const SCRIPT_GAP = 12
const SECTION_PAD_Y = 56
export const SCRIPT_NODE_W = 420

/* ══════════════════════════════════════
   Node theme colors
   ══════════════════════════════════════ */
const THEME: Record<string, { c: string; bg: string; border: string; Icon: React.ElementType; label: string }> = {
  opening:    { c: '#1565c0', bg: '#e8f0fe', border: '#90baf9', Icon: PlayCircleIcon,  label: '开场话术' },
  product:    { c: '#2e7d32', bg: '#e6f4ea', border: '#81c784', Icon: ShoppingBagIcon, label: '产品话术' },
  transition: { c: '#e65100', bg: '#fff3e0', border: '#ffb74d', Icon: SwapHorizIcon,   label: '衔接话术' },
  emotional:  { c: '#c2185b', bg: '#fce4ec', border: '#f48fb1', Icon: FavoriteIcon,    label: '情绪话术' },
  closing:    { c: '#7b1fa2', bg: '#f3e5f5', border: '#ce93d8', Icon: StopCircleIcon,  label: '结尾话术' },
}
const FALLBACK = { c: '#757575', bg: '#fafafa', border: '#bdbdbd', Icon: PlayCircleIcon, label: '话术' }

function themeOf(key: string, st?: string) {
  if (key.startsWith('product-')) return THEME.product
  if (key.startsWith('transition')) return THEME.transition
  return THEME[st ?? key] ?? FALLBACK
}

function scriptTheme(scriptType: string) {
  return THEME[scriptType] ?? FALLBACK
}

/* ══════════════════════════════════════
   Context — shared across all script nodes
   ══════════════════════════════════════ */
export type SP = Omit<ScriptSectionProps, 'title' | 'subtitle' | 'scripts' | 'expanded' | 'onToggle'>

const SectionPropsCtx = createContext<SP | null>(null)
export const SectionPropsProvider = SectionPropsCtx.Provider

export function useSP(): SP {
  const ctx = useContext(SectionPropsCtx)
  if (!ctx) throw new Error('SectionPropsCtx missing')
  return ctx
}

/* ══════════════════════════════════════
   Prevent ReactFlow from capturing pointer events on interactive nodes
   ══════════════════════════════════════ */
export const stopRFCapture = (e: React.PointerEvent | React.MouseEvent) => e.stopPropagation()

/* ══════════════════════════════════════════════
   Custom Node: SectionHeaderNode (compact card)
   ══════════════════════════════════════════════ */
interface SectionHeaderData {
  sectionKey: string
  title: string
  scriptCount: number
  firstScriptType?: string
  [key: string]: unknown
}

const SectionHeaderNode = memo(function SectionHeaderNode({ data }: { data: SectionHeaderData }) {
  const { c, bg, border, Icon } = themeOf(data.sectionKey, data.firstScriptType)
  return (
    <Box>
      <Handle type="target" position={Position.Top}
        style={{ background: c, width: 10, height: 10, border: `2px solid ${bg}` }} />
      <Card variant="outlined" sx={{
        width: 260, borderRadius: 3,
        borderColor: border, borderWidth: 2,
        boxShadow: `0 3px 16px ${c}20`,
        overflow: 'hidden',
      }}>
        <Box sx={{
          display: 'flex', alignItems: 'center', gap: 1,
          px: 1.5, py: 1,
          background: `linear-gradient(135deg, ${bg} 0%, #fff 100%)`,
        }}>
          <Box sx={{
            width: 32, height: 32, borderRadius: '50%', bgcolor: c,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: `0 2px 6px ${c}40`,
            flexShrink: 0,
          }}>
            <Icon sx={{ fontSize: 17, color: '#fff' }} />
          </Box>
          <Typography sx={{ fontSize: 15, fontWeight: 700, color: c, flex: 1 }} noWrap>
            {data.title}
          </Typography>
          <Chip label={`${data.scriptCount} 条`} size="small" sx={{
            height: 22, fontSize: 12, fontWeight: 600,
            bgcolor: `${c}15`, color: c,
            '& .MuiChip-label': { px: 0.75 },
          }} />
        </Box>
      </Card>
      <Handle type="source" position={Position.Bottom}
        style={{ background: c, width: 10, height: 10, border: `2px solid ${bg}` }} />
      <Handle type="source" position={Position.Right} id="branch"
        style={{ background: c, width: 8, height: 8, border: `2px solid ${bg}`, top: '50%' }} />
    </Box>
  )
})

/* ══════════════════════════════════════════════
   Custom Node: ProductHeaderNode
   ══════════════════════════════════════════════ */
interface ProductHeaderData {
  product: Record<string, unknown>
  scriptCount: number
  [key: string]: unknown
}

const ProductHeaderNode = memo(function ProductHeaderNode({ data }: { data: ProductHeaderData }) {
  const { c, bg, border } = THEME.product
  const p = data.product
  const productTypes = parseProductTypes(p.productType)
  const category = p.productCategory ? String(p.productCategory) : ''
  const costPrice = p.costPrice != null ? Number(p.costPrice) : 0
  const profitPct = p.profitMarginPct != null ? Number(p.profitMarginPct) : 0
  return (
    <Box>
      <Handle type="target" position={Position.Top}
        style={{ background: c, width: 10, height: 10, border: `2px solid ${bg}` }} />
      <Card variant="outlined" sx={{
        width: 340, borderRadius: 3,
        borderColor: border, borderWidth: 2,
        boxShadow: `0 3px 20px ${c}22`,
        overflow: 'hidden',
      }}>
        {/* Top row: large image + name + price + tags */}
        <Box sx={{
          display: 'flex', gap: 1.25, p: 1.25, alignItems: 'flex-start',
          background: `linear-gradient(135deg, ${bg} 0%, #fff 100%)`,
        }}>
          {p.imageUrl ? (
            <Box component="img" src={cdnThumb(p.imageUrl as string, 300, 250)} alt=""
              sx={{
                width: 80, height: 80, borderRadius: 2, objectFit: 'cover', flexShrink: 0,
                border: `1.5px solid ${border}60`, boxShadow: '0 2px 8px rgba(0,0,0,0.08)',
              }} />
          ) : (
            <Box sx={{
              width: 80, height: 80, borderRadius: 2, bgcolor: `${c}06`,
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
              border: `1.5px solid ${border}30`,
            }}>
              <ShoppingBagIcon sx={{ fontSize: 32, color: c, opacity: 0.3 }} />
            </Box>
          )}
          <Box sx={{ flex: 1, minWidth: 0 }}>
            <Typography sx={{ fontSize: 15, fontWeight: 700, lineHeight: 1.4, mb: 0.25 }}>
              {String(p.productName ?? '')}
            </Typography>
            {category && (
              <Typography sx={{ fontSize: 12, color: 'text.secondary', mb: 0.25 }}>
                {category}
              </Typography>
            )}
            <Box sx={{ display: 'flex', alignItems: 'baseline', gap: 0.75, mt: 0.25, flexWrap: 'wrap' }}>
              {p.price != null && Number(p.price) > 0 && (
                <Typography sx={{ fontSize: 18, fontWeight: 800, color: '#d32f2f', letterSpacing: -0.5 }}>
                  ¥{Number(p.price).toFixed(2)}
                </Typography>
              )}
              {costPrice > 0 && (
                <Typography sx={{ fontSize: 12, color: 'text.disabled', textDecoration: 'line-through' }}>
                  ¥{costPrice.toFixed(2)}
                </Typography>
              )}
            </Box>
            <Box sx={{ display: 'flex', gap: 0.5, mt: 0.5, flexWrap: 'wrap' }}>
              {productTypes.map(t => {
                const o = PRODUCT_TYPE_OPTIONS.find(x => x.value === t)
                return o ? <Chip key={t} label={o.label} size="small" color={o.color}
                  sx={{ height: 22, fontSize: 12, fontWeight: 600, '& .MuiChip-label': { px: 0.5 } }} /> : null
              })}
              {profitPct > 0 && (
                <Chip label={`利润 ${(profitPct * 100).toFixed(0)}%`} size="small" color="success" variant="outlined"
                  sx={{ height: 22, fontSize: 12, fontWeight: 600, '& .MuiChip-label': { px: 0.5 } }} />
              )}
            </Box>
          </Box>
        </Box>
        {/* Bottom bar: script count */}
        <Box sx={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          px: 1.25, py: 0.5,
          borderTop: `1px solid ${border}40`,
          bgcolor: `${c}04`,
        }}>
          <Typography sx={{ fontSize: 13, color: 'text.secondary', fontWeight: 500 }}>
            话术数量
          </Typography>
          <Chip label={`${data.scriptCount} 条`} size="small" sx={{
            height: 24, fontSize: 13, fontWeight: 600,
            bgcolor: `${c}15`, color: c,
            '& .MuiChip-label': { px: 0.75 },
          }} />
        </Box>
      </Card>
      <Handle type="source" position={Position.Bottom}
        style={{ background: c, width: 10, height: 10, border: `2px solid ${bg}` }} />
      <Handle type="source" position={Position.Right} id="branch"
        style={{ background: c, width: 8, height: 8, border: `2px solid ${bg}`, top: '50%' }} />
    </Box>
  )
})

/* ══════════════════════════════════════════════
   Custom Node: ScriptBranchNode (individual script)
   ══════════════════════════════════════════════ */
interface ScriptBranchData {
  row: Record<string, unknown>
  idx: number
  accent: string
  [key: string]: unknown
}

const ScriptBranchNode = memo(function ScriptBranchNode({ data }: { data: ScriptBranchData }) {
  const { row, idx, accent } = data
  const sp = useSP()
  const id = row.id as number
  const isEditing = sp.editingId === id
  const content = isEditing ? sp.editContent : (row.scriptContent as string) ?? ''
  const estSec = Number(row.estimatedDurationSeconds ?? 0) > 0
    ? Number(row.estimatedDurationSeconds) : estimateDurationFromText(row.scriptContent as string)
  const statusLabel = Number(row.executed) ? '已执行' : Number(row.aiGenerated) ? '已生成' : '待生成'
  const statusColor: 'success' | 'info' | 'default' = Number(row.executed) ? 'success' : Number(row.aiGenerated) ? 'info' : 'default'
  const focused = sp.focusedScriptId === id
  const seqNo = String(row.sequenceNo ?? idx + 1)
  const typeLabel = SCRIPT_TYPE_LABEL[row.scriptType as string] ?? '-'

  if (isEditing) {
    return (
      <ScriptEditCard
        row={row}
        seqNo={seqNo}
        typeLabel={typeLabel}
        accent={accent}
      />
    )
  }

  return (
    <Box sx={{ width: SCRIPT_NODE_W }} className="nopan nodrag nowheel"
      onPointerDown={stopRFCapture} onMouseDown={stopRFCapture}>
      <Handle type="target" position={Position.Left}
        style={{ background: accent, width: 8, height: 8, border: '2px solid #fff' }} />
      <Card
        variant="outlined"
        onClick={() => sp.onFocusScript?.(id)}
        sx={{
          cursor: 'pointer', borderRadius: 2.5,
          border: '1.5px solid', borderColor: focused ? accent : 'divider',
          borderWidth: focused ? 2 : 1.5,
          bgcolor: focused ? `${accent}08` : 'background.paper',
          transition: 'border-color 0.15s, box-shadow 0.15s',
          '&:hover': { borderColor: `${accent}88`, boxShadow: `0 3px 14px ${accent}1a` },
          overflow: 'hidden',
        }}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', px: 1, pt: 0.75, gap: 0.75 }}>
          <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: accent, flexShrink: 0 }} />
          <Typography sx={{ fontSize: 13, fontWeight: 600, color: accent }} noWrap>
            #{seqNo} {typeLabel}
          </Typography>
          {estSec > 0 && <Typography sx={{ fontSize: 12, color: 'text.disabled' }}>~{estSec}s</Typography>}
          <Box sx={{ flex: 1 }} />
          <Chip label={statusLabel} size="small" color={statusColor}
            variant={statusColor === 'default' ? 'outlined' : 'filled'}
            sx={{ height: 18, fontSize: 11, '& .MuiChip-label': { px: 0.5 } }} />
        </Box>
        <Typography sx={{
          px: 1, pt: 0.375, pb: 0.75, fontSize: '0.85rem', lineHeight: 1.6,
          display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden',
          color: content ? 'text.primary' : 'text.disabled',
        }}>
          {content || '(待生成)'}
        </Typography>
        {/* Always-visible action toolbar */}
        <Box sx={{
          display: 'flex', alignItems: 'center', gap: '2px',
          px: 0.75, py: 0.25,
          borderTop: '1px solid', borderColor: 'divider',
          bgcolor: 'action.hover',
        }}>
          {([
            { tip: '编辑', icon: EditIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onEdit(id, (row.scriptContent as string) ?? '', row) } },
            { tip: '重新生成', icon: RefreshIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onRegenerateSingle?.(id) } },
            { tip: '分析', icon: PsychologyIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onOpenAnalyst?.(row) }, color: sp.analystScriptId === id ? 'primary' as const : undefined },
            { tip: 'AI修改', icon: AutoAwesomeIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onRefineOpen({ scriptId: id }) } },
            { tip: Number(row.executed) ? '已执行' : '标记执行', icon: Number(row.executed) ? CheckCircleIcon : PlayCircleIcon,
              fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onMarkExecuted(id, (row.executed as number) ?? 0) },
              color: Number(row.executed) ? 'success' as const : undefined },
            { tip: '存入话术库', icon: SaveIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onSaveToLibrary(id) } },
            { tip: '删除', icon: DeleteIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onDelete(row) }, color: 'error' as const },
          ] as const).map(a => (
            <Tooltip key={a.tip} title={a.tip} placement="top">
              <IconButton size="small" onClick={a.fn} color={'color' in a && a.color ? a.color : 'default'} sx={{ p: '3px' }}>
                <a.icon sx={{ fontSize: 14 }} />
              </IconButton>
            </Tooltip>
          ))}
        </Box>
      </Card>
      {/* Right handle for AI connection (shown on focused card) */}
      {focused && (
        <Handle type="source" position={Position.Right} id="to-ai"
          style={{ background: '#1976d2', width: 8, height: 8, border: '2px solid #e8f0fe' }} />
      )}
    </Box>
  )
})

/* ══════════════════════════════════════
   Node type registry
   ══════════════════════════════════════ */
export const nodeTypes: NodeTypes = {
  sectionHeader: SectionHeaderNode as NodeTypes[string],
  productHeader: ProductHeaderNode as NodeTypes[string],
  scriptBranch: ScriptBranchNode as NodeTypes[string],
}

/* ══════════════════════════════════════
   Default edge options (ComfyUI bezier)
   ══════════════════════════════════════ */
export const defaultEdgeOptions: DefaultEdgeOptions = {
  type: 'default',
  style: { stroke: '#94a3b8', strokeWidth: 2 },
  animated: false,
}

/* ══════════════════════════════════════
   Pre-allocated edge style constants
   ══════════════════════════════════════ */
const TRUNK_EDGE_STYLES: Record<string, React.CSSProperties> = {}
function trunkEdgeStyle(color: string): React.CSSProperties {
  if (!TRUNK_EDGE_STYLES[color]) TRUNK_EDGE_STYLES[color] = { stroke: color, strokeWidth: 2.5, opacity: 0.45 }
  return TRUNK_EDGE_STYLES[color]
}
const BRANCH_EDGE_STYLES: Record<string, React.CSSProperties> = {}
function branchEdgeStyle(color: string): React.CSSProperties {
  if (!BRANCH_EDGE_STYLES[color]) BRANCH_EDGE_STYLES[color] = { stroke: color, strokeWidth: 2, opacity: 0.5 }
  return BRANCH_EDGE_STYLES[color]
}

/* ══════════════════════════════════════
   MiniMap node color
   ══════════════════════════════════════ */
export function miniMapNodeColor(node: Node): string {
  if (node.type === 'productHeader') return THEME.product.c
  if (node.type === 'scriptBranch') {
    const d = node.data as { accent?: string }
    return d.accent ?? '#94a3b8'
  }
  const d = node.data as { sectionKey?: string }
  if (d.sectionKey) return THEME[d.sectionKey]?.c ?? '#94a3b8'
  return '#94a3b8'
}

/* ══════════════════════════════════════════════════════════════
   buildFlowData — manual tree layout
   Left column: section headers (trunk)
   Middle column: script nodes (branches)
   ══════════════════════════════════════════════════════════════ */
export function buildFlowData(
  scriptSections: ScriptSectionData[],
  editingId: number | null,
): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = []
  const edges: Edge[] = []

  let curY = 0

  for (let i = 0; i < scriptSections.length; i++) {
    const sec = scriptSections[i]
    const isProd = sec.key.startsWith('product-') && sec.product
    const theme = themeOf(sec.key, sec.scripts[0]?.scriptType as string | undefined)

    const scriptCount = sec.scripts.length
    const scriptsFanH = scriptCount > 0
      ? sec.scripts.reduce((h, s) => {
          const isEd = editingId === (s.id as number)
          return h + (isEd ? SCRIPT_EDIT_H : SCRIPT_CARD_H) + SCRIPT_GAP
        }, -SCRIPT_GAP)
      : 0

    const headerH = isProd ? 140 : 50
    const sectionH = Math.max(headerH, scriptsFanH)
    const headerY = curY + (sectionH - headerH) / 2

    const headerW = isProd ? PRODUCT_HEADER_W : SECTION_HEADER_W
    const headerX = TRUNK_CENTER_X - headerW / 2

    if (isProd && sec.product) {
      nodes.push({
        id: sec.key,
        type: 'productHeader',
        position: { x: headerX, y: headerY },
        draggable: false,
        data: { product: sec.product, scriptCount },
      })
    } else {
      nodes.push({
        id: sec.key,
        type: 'sectionHeader',
        position: { x: headerX, y: headerY },
        draggable: false,
        data: {
          sectionKey: sec.key,
          title: sec.title,
          scriptCount,
          firstScriptType: sec.scripts[0]?.scriptType as string | undefined,
        },
      })
    }

    if (i > 0) {
      const prevKey = scriptSections[i - 1].key
      edges.push({
        id: `trunk-${prevKey}-${sec.key}`,
        source: prevKey,
        target: sec.key,
        type: 'straight',
        style: trunkEdgeStyle(theme.c),
      })
    }

    const scriptStartY = curY + (sectionH - scriptsFanH) / 2
    let scriptY = scriptCount > 0 ? scriptStartY : curY
    for (let j = 0; j < sec.scripts.length; j++) {
      const s = sec.scripts[j]
      const sId = `script-${s.id}`
      const sType = String(s.scriptType ?? '')
      const sTheme = scriptTheme(sType)
      const isEd = editingId === (s.id as number)

      nodes.push({
        id: sId,
        type: 'scriptBranch',
        position: { x: SCRIPT_X, y: scriptY },
        draggable: false,
        data: { row: s, idx: j, accent: sTheme.c },
      })

      edges.push({
        id: `branch-${sec.key}-${s.id}`,
        source: sec.key,
        sourceHandle: 'branch',
        target: sId,
        type: 'smoothstep',
        style: branchEdgeStyle(sTheme.c),
      })

      scriptY += (isEd ? SCRIPT_EDIT_H : SCRIPT_CARD_H) + SCRIPT_GAP
    }

    curY += sectionH + SECTION_PAD_Y
  }

  return { nodes, edges }
}
