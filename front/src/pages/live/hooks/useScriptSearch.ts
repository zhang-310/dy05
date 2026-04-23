import { useState, useMemo, useCallback } from 'react'
import type { LiveScript } from '@/api/live'

export function useScriptSearch(scripts: LiveScript[]) {
  const [query, setQuery] = useState('')

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase()
    if (!q) return scripts
    return scripts.filter(s =>
      String(s.scriptType ?? '').toLowerCase().includes(q) ||
      (s.scriptContent ?? '').toLowerCase().includes(q)
    )
  }, [scripts, query])

  const clear = useCallback(() => setQuery(''), [])

  return { query, setQuery, filtered, clear }
}
