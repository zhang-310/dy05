import { alpha, type Theme } from '@mui/material/styles'

export type WorkspaceTab = 'products' | 'generate' | 'scripts' | 'readiness' | 'data'

export const STEP_TO_TAB: WorkspaceTab[] = ['products', 'generate', 'scripts', 'readiness', 'data']

export const TAB_TO_STEP: Record<WorkspaceTab, number> = {
  products: 0, generate: 1, scripts: 2, readiness: 3, data: 4,
}

export const STEP_LABELS: Record<WorkspaceTab, string> = {
  products: '选品排品',
  generate: 'AI生成',
  scripts:  '话术微调',
  readiness:'准备发布',
  data:     '数据复盘',
}

type StepPalette = 'primary' | 'secondary' | 'warning' | 'success' | 'info'

export const STEP_COLOR_TONES: Record<WorkspaceTab, StepPalette> = {
  products: 'primary',
  generate: 'secondary',
  scripts: 'warning',
  readiness: 'success',
  data: 'info',
}

export const STEP_COLORS: Record<WorkspaceTab, { tone: StepPalette; bg: string; active: string }> = {
  products: { tone: STEP_COLOR_TONES.products, bg: 'primary.soft', active: 'primary.main' },
  generate: { tone: STEP_COLOR_TONES.generate, bg: 'secondary.soft', active: 'secondary.main' },
  scripts: { tone: STEP_COLOR_TONES.scripts, bg: 'warning.soft', active: 'warning.main' },
  readiness: { tone: STEP_COLOR_TONES.readiness, bg: 'success.soft', active: 'success.main' },
  data: { tone: STEP_COLOR_TONES.data, bg: 'info.soft', active: 'info.main' },
}

export function getStepThemeColors(theme: Theme, tab: WorkspaceTab, active: boolean) {
  const tone = STEP_COLOR_TONES[tab]
  const palette = theme.palette[tone]
  const main = palette.main
  const contrast = palette.contrastText
  const softAlpha = theme.palette.mode === 'dark' ? 0.18 : 0.1

  return {
    tone,
    chipBg: active ? main : alpha(main, softAlpha),
    chipColor: active ? contrast : main,
    chipBorder: active ? main : alpha(main, theme.palette.mode === 'dark' ? 0.36 : 0.22),
    chipHoverBg: active ? main : alpha(main, theme.palette.mode === 'dark' ? 0.26 : 0.16),
    badgeBg: active ? contrast : main,
    badgeColor: active ? main : contrast,
  }
}

export function parseStepFromSearch(sp: URLSearchParams): number {
  const explicit = sp.get('step')
  if (explicit != null && explicit !== '') {
    const n = parseInt(explicit, 10)
    if (Number.isFinite(n) && n >= 0 && n < STEP_TO_TAB.length) return n
  }
  const t = sp.get('tab') as WorkspaceTab | null
  if (t && t in TAB_TO_STEP) return TAB_TO_STEP[t]
  return 0
}

export function buildStepParams(step: number, prev: URLSearchParams): URLSearchParams {
  const sp = new URLSearchParams(prev)
  sp.set('step', String(step))
  sp.set('tab', STEP_TO_TAB[step] ?? 'products')
  return sp
}
