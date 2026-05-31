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
import { alpha, type Theme } from '@mui/material/styles'
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
const READY_ENDPOINTS = '/live/script/by-session|/live/script/save|/live/script/delete|/live/script/executed|/live/ai/check-violation|/live/ai/refine-script|/live/ai/generate-slot'
const UNSUPPORTED_ACTIONS = 'direct-api-request|local-script-fallback|mock-product-header|mock-script-card'

/* ══════════════════════════════════════
   Node theme colors
   ══════════════════════════════════════ */
export type FlowTone = 'primary' | 'success' | 'warning' | 'error' | 'secondary'

const THEME: Record<string, { tone: FlowTone; Icon: React.ElementType; label: string }> = {
  opening:    { tone: 'primary',   Icon: PlayCircleIcon,  label: '开场话术' },
  product:    { tone: 'success',   Icon: ShoppingBagIcon, label: '产品话术' },
  transition: { tone: 'warning',   Icon: SwapHorizIcon,   label: '衔接话术' },
  emotional:  { tone: 'error',     Icon: FavoriteIcon,    label: '情绪话术' },
  closing:    { tone: 'secondary', Icon: StopCircleIcon,  label: '结尾话术' },
}
const FALLBACK = { tone: 'primary' as FlowTone, Icon: PlayCircleIcon, label: '话术' }

export function flowToneColor(tone: FlowTone, theme?: Theme): string {
  return theme ? theme.palette[tone].main : `var(--script-flow-tone-${tone}, currentColor)`
}

function flowToneContrastColor(tone: FlowTone, theme: Theme): string {
  return theme.palette[tone].contrastText
}

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
  const { tone, Icon } = themeOf(data.sectionKey, data.firstScriptType)
  return (
    <Box
      data-testid="script-flow-section-header-node"
      data-contract-scope="live-script-flow-section-header-node"
      data-contract-source="buildFlowData-section-data"
      data-ready-endpoints={READY_ENDPOINTS}
      data-unsupported-actions={UNSUPPORTED_ACTIONS}
      data-section-key={data.sectionKey}
      data-section-title={data.title}
      data-script-count={data.scriptCount}
      data-tone={tone}
      data-no-direct-api="true"
      data-no-local-script-fallback="true"
      sx={(theme) => {
        const main = theme.palette[tone].main
        return {
          '--script-flow-accent': main,
          '--script-flow-handle-border': alpha(main, theme.palette.mode === 'dark' ? 0.3 : 0.14),
        }
      }}
    >
      <Handle type="target" position={Position.Top}
        style={{ background: 'var(--script-flow-accent)', width: 10, height: 10, border: '2px solid var(--script-flow-handle-border)' }} />
      <Card variant="outlined" sx={(theme) => ({
        width: 260, borderRadius: 3,
        borderColor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.55 : 0.32),
        borderWidth: 2,
        boxShadow: `0 3px 16px ${alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.22 : 0.12)}`,
        overflow: 'hidden',
      })}>
        <Box
          data-testid="script-flow-section-header-surface"
          sx={(theme) => ({
          display: 'flex', alignItems: 'center', gap: 1,
          px: 1.5, py: 1,
          background: `linear-gradient(135deg, ${alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.22 : 0.09)} 0%, ${theme.palette.background.paper} 100%)`,
        })}
        >
          <Box sx={(theme) => ({
            width: 32, height: 32, borderRadius: '50%', bgcolor: theme.palette[tone].main,
            display: 'flex', alignItems: 'center', justifyContent: 'center',
            boxShadow: `0 2px 6px ${alpha(theme.palette[tone].main, 0.28)}`,
            flexShrink: 0,
          })}>
            <Icon sx={(theme: Theme) => ({ fontSize: 17, color: flowToneContrastColor(tone, theme) })} />
          </Box>
          <Typography sx={{ fontSize: 15, fontWeight: 700, color: `${tone}.main`, flex: 1 }} noWrap>
            {data.title}
          </Typography>
          <Chip label={`${data.scriptCount} 条`} size="small" data-testid="script-flow-section-count-chip-surface" sx={(theme) => ({
            height: 22, fontSize: 12, fontWeight: 600,
            bgcolor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
            color: `${tone}.main`,
            '& .MuiChip-label': { px: 0.75 },
          })} />
        </Box>
      </Card>
      <Handle type="source" position={Position.Bottom}
        style={{ background: 'var(--script-flow-accent)', width: 10, height: 10, border: '2px solid var(--script-flow-handle-border)' }} />
      <Handle type="source" position={Position.Right} id="branch"
        style={{ background: 'var(--script-flow-accent)', width: 8, height: 8, border: '2px solid var(--script-flow-handle-border)', top: '50%' }} />
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
  const { tone } = THEME.product
  const p = data.product
  const productTypes = parseProductTypes(p.productType)
  const category = p.productCategory ? String(p.productCategory) : ''
  const costPrice = p.costPrice != null ? Number(p.costPrice) : 0
  const profitPct = p.profitMarginPct != null ? Number(p.profitMarginPct) : 0
  return (
    <Box
      data-testid="script-flow-product-header-node"
      data-contract-scope="live-script-flow-product-header-node"
      data-contract-source="buildFlowData-product-data"
      data-ready-endpoints="/live/product/by-session|/live/script/by-session"
      data-unsupported-actions={UNSUPPORTED_ACTIONS}
      data-product-id={String(p.productId ?? p.id ?? '')}
      data-product-name={String(p.productName ?? '')}
      data-script-count={data.scriptCount}
      data-product-type={String(p.productType ?? '')}
      data-has-image={p.imageUrl ? 'true' : 'false'}
      data-no-direct-api="true"
      data-no-local-product-fallback="true"
      sx={(theme) => {
        const main = theme.palette[tone].main
        return {
          '--script-flow-accent': main,
          '--script-flow-handle-border': alpha(main, theme.palette.mode === 'dark' ? 0.3 : 0.14),
        }
      }}
    >
      <Handle type="target" position={Position.Top}
        style={{ background: 'var(--script-flow-accent)', width: 10, height: 10, border: '2px solid var(--script-flow-handle-border)' }} />
      <Card variant="outlined" sx={(theme) => ({
        width: 340, borderRadius: 3,
        borderColor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.55 : 0.32),
        borderWidth: 2,
        boxShadow: `0 3px 20px ${alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.22 : 0.12)}`,
        overflow: 'hidden',
      })}>
        {/* Top row: large image + name + price + tags */}
        <Box
          data-testid="script-flow-product-header-surface"
          sx={(theme) => ({
          display: 'flex', gap: 1.25, p: 1.25, alignItems: 'flex-start',
          background: `linear-gradient(135deg, ${alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.2 : 0.08)} 0%, ${theme.palette.background.paper} 100%)`,
        })}
        >
          {p.imageUrl ? (
            <Box component="img" src={cdnThumb(p.imageUrl as string, 300, 250)} alt=""
              sx={{
                width: 80, height: 80, borderRadius: 2, objectFit: 'cover', flexShrink: 0,
                border: '1.5px solid',
                borderColor: `${tone}.main`,
                boxShadow: (theme) => `0 2px 8px ${alpha(theme.palette.common.black, theme.palette.mode === 'dark' ? 0.22 : 0.08)}`,
              }} />
          ) : (
            <Box sx={(theme) => ({
              width: 80, height: 80, borderRadius: 2, bgcolor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.16 : 0.06),
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
              border: '1.5px solid',
              borderColor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.32 : 0.18),
            })}>
              <ShoppingBagIcon sx={{ fontSize: 32, color: `${tone}.main`, opacity: 0.55 }} />
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
                <Typography sx={{ fontSize: 18, fontWeight: 800, color: 'error.main' }}>
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
        <Box sx={(theme) => ({
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          px: 1.25, py: 0.5,
          borderTop: '1px solid',
          borderColor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.32 : 0.18),
          bgcolor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.12 : 0.04),
        })}>
          <Typography sx={{ fontSize: 13, color: 'text.secondary', fontWeight: 500 }}>
            话术数量
          </Typography>
          <Chip label={`${data.scriptCount} 条`} size="small" data-testid="script-flow-product-count-chip-surface" sx={(theme) => ({
            height: 24, fontSize: 13, fontWeight: 600,
            bgcolor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.18 : 0.1),
            color: `${tone}.main`,
            '& .MuiChip-label': { px: 0.75 },
          })} />
        </Box>
      </Card>
      <Handle type="source" position={Position.Bottom}
        style={{ background: 'var(--script-flow-accent)', width: 10, height: 10, border: '2px solid var(--script-flow-handle-border)' }} />
      <Handle type="source" position={Position.Right} id="branch"
        style={{ background: 'var(--script-flow-accent)', width: 8, height: 8, border: '2px solid var(--script-flow-handle-border)', top: '50%' }} />
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
  tone?: FlowTone
  [key: string]: unknown
}

const ScriptBranchNode = memo(function ScriptBranchNode({ data }: { data: ScriptBranchData }) {
  const { row, idx, accent, tone = 'primary' } = data
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
        accentTone={tone}
      />
    )
  }

  return (
    <Box
      data-testid="script-flow-script-branch-node"
      data-contract-scope="live-script-flow-script-branch-node"
      data-contract-source="buildFlowData-script-data|SectionPropsProvider-context"
      data-ready-endpoints={READY_ENDPOINTS}
      data-unsupported-actions={UNSUPPORTED_ACTIONS}
      data-script-id={id}
      data-script-type={String(row.scriptType ?? '')}
      data-sequence-no={seqNo}
      data-status={statusLabel}
      data-focused={focused ? 'true' : 'false'}
      data-editing={isEditing ? 'true' : 'false'}
      data-tone={tone}
      data-no-direct-api="true"
      data-no-local-script-fallback="true"
      sx={(theme) => ({
        width: SCRIPT_NODE_W,
        '--script-flow-accent': theme.palette[tone].main,
        '--script-flow-handle-border': alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.3 : 0.14),
      })}
      className="nopan nodrag nowheel"
      onPointerDown={stopRFCapture} onMouseDown={stopRFCapture}>
      <Handle type="target" position={Position.Left}
        style={{ background: 'var(--script-flow-accent)', width: 8, height: 8, border: '2px solid var(--script-flow-handle-border)' }} />
      <Card
        variant="outlined"
        onClick={() => sp.onFocusScript?.(id)}
        sx={(theme) => ({
          cursor: 'pointer', borderRadius: 2.5,
          border: '1.5px solid',
          borderColor: focused ? alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.72 : 0.55) : 'divider',
          borderWidth: focused ? 2 : 1.5,
          bgcolor: focused ? alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.16 : 0.06) : 'background.paper',
          transition: 'border-color 0.15s, box-shadow 0.15s',
          '&:hover': {
            borderColor: alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.78 : 0.55),
            boxShadow: `0 3px 14px ${alpha(theme.palette[tone].main, theme.palette.mode === 'dark' ? 0.22 : 0.1)}`,
          },
          overflow: 'hidden',
        })}
      >
        <Box sx={{ display: 'flex', alignItems: 'center', px: 1, pt: 0.75, gap: 0.75 }}>
          <Box sx={{ width: 8, height: 8, borderRadius: '50%', bgcolor: `${tone}.main`, flexShrink: 0 }} />
          <Typography sx={{ fontSize: 13, fontWeight: 600, color: `${tone}.main` }} noWrap>
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
            { testId: 'script-flow-branch-edit-button', source: '/live/script/save', owner: 'onEdit-prop', tip: '编辑', icon: EditIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onEdit(id, (row.scriptContent as string) ?? '', row) } },
            { testId: 'script-flow-branch-regenerate-button', source: '/live/ai/generate-slot', owner: 'onRegenerateSingle-prop', tip: '重新生成', icon: RefreshIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onRegenerateSingle?.(id) } },
            { testId: 'script-flow-branch-analyst-button', source: 'onOpenAnalyst-prop', owner: 'onOpenAnalyst-prop', tip: '分析', icon: PsychologyIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onOpenAnalyst?.(row) }, color: sp.analystScriptId === id ? 'primary' as const : undefined },
            { testId: 'script-flow-branch-refine-button', source: '/live/ai/refine-script', owner: 'onRefineOpen-prop', tip: 'AI修改', icon: AutoAwesomeIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onRefineOpen({ scriptId: id }) } },
            { testId: 'script-flow-branch-executed-button', source: '/live/script/executed', owner: 'onMarkExecuted-prop', tip: Number(row.executed) ? '已执行' : '标记执行', icon: Number(row.executed) ? CheckCircleIcon : PlayCircleIcon,
              fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onMarkExecuted(id, (row.executed as number) ?? 0) },
              color: Number(row.executed) ? 'success' as const : undefined },
            { testId: 'script-flow-branch-save-library-button', source: '/live/script/save-to-library', owner: 'onSaveToLibrary-prop', tip: '存入话术库', icon: SaveIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onSaveToLibrary(id) } },
            { testId: 'script-flow-branch-delete-button', source: '/live/script/delete', owner: 'onDelete-prop', tip: '删除', icon: DeleteIcon, fn: (e: React.MouseEvent) => { e.stopPropagation(); sp.onDelete(row) }, color: 'error' as const },
          ] as const).map(a => (
            <Tooltip key={a.tip} title={a.tip} placement="top">
              <IconButton
                data-testid={a.testId}
                data-contract-source={a.source}
                data-action-owner={a.owner}
                data-script-id={id}
                size="small"
                onClick={a.fn}
                color={'color' in a && a.color ? a.color : 'default'}
                sx={{ p: '3px' }}
              >
                <a.icon sx={{ fontSize: 14 }} />
              </IconButton>
            </Tooltip>
          ))}
        </Box>
      </Card>
      {/* Right handle for AI connection (shown on focused card) */}
      {focused && (
        <Box
          data-testid="script-flow-branch-ai-handle-surface"
          data-contract-source="focusedScriptId-prop"
          data-script-id={id}
        >
          <Handle type="source" position={Position.Right} id="to-ai"
            style={{ background: 'var(--script-flow-accent)', width: 8, height: 8, border: '2px solid var(--script-flow-handle-border)' }} />
        </Box>
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
  style: { stroke: 'var(--script-flow-edge-muted, currentColor)', strokeWidth: 2 },
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
export function miniMapNodeColor(node: Node, theme?: Theme): string {
  if (node.type === 'productHeader') return flowToneColor(THEME.product.tone, theme)
  if (node.type === 'scriptBranch') {
    const d = node.data as { accent?: string; tone?: FlowTone }
    return d.accent ?? flowToneColor(d.tone ?? FALLBACK.tone, theme)
  }
  const d = node.data as { sectionKey?: string }
  if (d.sectionKey) return flowToneColor(THEME[d.sectionKey]?.tone ?? FALLBACK.tone, theme)
  return 'var(--script-flow-edge-muted, currentColor)'
}

/* ══════════════════════════════════════════════════════════════
   buildFlowData — manual tree layout
   Left column: section headers (trunk)
   Middle column: script nodes (branches)
   ══════════════════════════════════════════════════════════════ */
export function buildFlowData(
  scriptSections: ScriptSectionData[],
  editingId: number | null,
  theme?: Theme,
): { nodes: Node[]; edges: Edge[] } {
  const nodes: Node[] = []
  const edges: Edge[] = []

  let curY = 0

  for (let i = 0; i < scriptSections.length; i++) {
    const sec = scriptSections[i]
    const isProd = sec.key.startsWith('product-') && sec.product
    const sectionTheme = themeOf(sec.key, sec.scripts[0]?.scriptType as string | undefined)

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
        data: { edgeTone: sectionTheme.tone },
        style: trunkEdgeStyle(flowToneColor(sectionTheme.tone, theme)),
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
        data: { row: s, idx: j, accent: theme ? flowToneColor(sTheme.tone, theme) : undefined, tone: sTheme.tone },
      })

      edges.push({
        id: `branch-${sec.key}-${s.id}`,
        source: sec.key,
        sourceHandle: 'branch',
        target: sId,
        type: 'smoothstep',
        data: { edgeTone: sTheme.tone },
        style: branchEdgeStyle(flowToneColor(sTheme.tone, theme)),
      })

      scriptY += (isEd ? SCRIPT_EDIT_H : SCRIPT_CARD_H) + SCRIPT_GAP
    }

    curY += sectionH + SECTION_PAD_Y
  }

  return { nodes, edges }
}
