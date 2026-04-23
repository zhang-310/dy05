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

export const STEP_COLORS: Record<WorkspaceTab, { bg: string; active: string }> = {
  products:  { bg: '#e3f2fd', active: '#1565c0' },
  generate:  { bg: '#f3e5f5', active: '#7b1fa2' },
  scripts:   { bg: '#fff3e0', active: '#e65100' },
  readiness: { bg: '#e8f5e9', active: '#2e7d32' },
  data:      { bg: '#e0f7fa', active: '#00838f' },
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
