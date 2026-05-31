/**
 * Shared types for ProductScriptManageDialog sub-components
 */
import type { ProductScript, StylePreset } from '@/api/product'
import type { ScriptGenStep } from '@/hooks/useProductScriptGeneration'

/* ─── helpers ─── */
export function formatTime(s: string | null | undefined): string {
  if (!s) return ''
  try {
    const d = new Date(s)
    const now = new Date()
    const diff = now.getTime() - d.getTime()
    if (diff < 60000) return '刚刚'
    if (diff < 3600000) return `${Math.floor(diff / 60000)} 分钟前`
    if (diff < 86400000) return `${Math.floor(diff / 3600000)} 小时前`
    if (diff < 604800000) return `${Math.floor(diff / 86400000)} 天前`
    return d.toLocaleDateString('zh-CN', { month: 'short', day: 'numeric' })
  } catch {
    return String(s)
  }
}

export function copyToClipboard(text: string) {
  navigator.clipboard.writeText(text).catch(() => {
    /* fallback not needed for modern browsers */
  })
}

/* ─── Style list props ─── */
export interface ScriptStyleListProps {
  scriptType: string
  onScriptTypeChange: (v: string) => void
  byStyle: Record<string, ProductScript[]>
  activeByStyle: Record<string, ProductScript>
  loading: boolean
  expandedStyles: Record<string, boolean>
  onToggleStyleExpand: (style: string) => void
  expandedScript: number | null
  onToggleScriptExpand: (id: number) => void
  nameMap: Record<string, string>
  totalScripts: number
  activeCount: number
  styleCount: number
  onActivate: (script: ProductScript) => void
  onDelete: (script: ProductScript) => void
  onHistory: (script: ProductScript) => void
  onExportShortVideo?: (script: ProductScript) => void
  onRefine: (script: ProductScript) => void
  onCopy: (script: ProductScript) => void
}

/* ─── Style preset item (for generation config) ─── */
export interface StylePresetItem {
  presetCode: string
  presetName: string
  description?: string
  category?: string
}

/* ─── Generate panel props ─── */
export interface ScriptGeneratePanelProps {
  /* config state */
  personas: Record<string, unknown>[]
  genPersonaId: number | ''
  setGenPersonaId: (v: number | '') => void
  genScene: string
  setGenScene: (v: string) => void
  corePresets: readonly StylePresetItem[]
  extendedPresets: readonly StylePresetItem[]
  genStyles: string[]
  recommendedStyleCodes: string[]
  showExtended: boolean
  setShowExtended: (v: boolean) => void
  onToggleStyle: (s: string) => void
  fusionMode: boolean
  setFusionMode: (v: boolean) => void
  fusionStrategy: string
  setFusionStrategy: (v: string) => void
  styleWeights: Record<string, number>
  setStyleWeights: (v: Record<string, number>) => void
  genDuration: number
  setGenDuration: (v: number) => void
  useKbRef: boolean
  setUseKbRef: (v: boolean) => void
  scriptCategories?: string[]
  selectedKbCategories?: string[]
  setSelectedKbCategories?: (v: string[]) => void
  onStart: () => void
  onPreview?: () => void
  /* progress state */
  steps: ScriptGenStep[]
  isGenerating: boolean
  doneCount: number
  failCount: number
  hasSteps: boolean
  onCancel: () => void
  onRetry: () => void
  onReset: () => void
  /* A/B experiment state */
  abExperimentId?: number | null
  abVariantId?: number | null
}

/* ─── Content viewer (refine) props ─── */
export interface ScriptContentViewerProps {
  script: ProductScript
  styleName: string
  prompt: string
  onPromptChange: (v: string) => void
  result: string
  refining: boolean
  onStart: () => void
  onAccept: () => void
  onCancel: () => void
}

export type { ProductScript, StylePreset, ScriptGenStep }
