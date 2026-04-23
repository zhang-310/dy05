import { useMemo } from 'react'
import type { LiveScript } from '@/api/live'

export interface EditProgress {
  total: number
  filled: number
  edited: number
  avgWordCount: number
  completionRate: number
}

export function useEditProgress(scripts: LiveScript[], editedIds: Set<number>): EditProgress {
  return useMemo(() => {
    const total = scripts.length
    if (total === 0) return { total: 0, filled: 0, edited: 0, avgWordCount: 0, completionRate: 0 }
    const filled = scripts.filter(s => (s.scriptContent ?? '').trim().length > 0).length
    const edited = scripts.filter(s => editedIds.has(s.id)).length
    const totalChars = scripts.reduce((sum, s) => sum + (s.scriptContent?.length ?? 0), 0)
    const avgWordCount = filled === 0 ? 0 : Math.round(totalChars / filled)
    const completionRate = Math.round((filled / total) * 100)
    return { total, filled, edited, avgWordCount, completionRate }
  }, [scripts, editedIds])
}
