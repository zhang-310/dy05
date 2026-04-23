/** localStorage-based feature flags */

export const FLAGS = {
  STAGE_WORKFLOW: 'stage_workflow',
} as const

const PREFIX = 'ff_'

/** 默认开启的 flag 集合 */
const ENABLED_BY_DEFAULT = new Set<string>([FLAGS.STAGE_WORKFLOW])

export function isFeatureEnabled(flag: string): boolean {
  try {
    const stored = localStorage.getItem(`${PREFIX}${flag}`)
    if (stored !== null) return stored === '1'
    return ENABLED_BY_DEFAULT.has(flag)
  } catch {
    return ENABLED_BY_DEFAULT.has(flag)
  }
}

export function setFeatureFlag(flag: string, enabled: boolean): void {
  try {
    if (enabled) {
      localStorage.setItem(`${PREFIX}${flag}`, '1')
    } else {
      localStorage.removeItem(`${PREFIX}${flag}`)
    }
  } catch { /* ignore */ }
}
