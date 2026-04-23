import { useState, useCallback, useEffect } from 'react'

export interface ModelPerfStats {
  avgDuration: number
  successRate: number
  lastUsed: number
  callCount: number
}

interface PerfEntry {
  modelId: number
  durations: number[]
  successes: number
  failures: number
  lastUsed: number
}

const STORAGE_KEY = 'model-perf-history'
const MAX_ENTRIES = 50 // 每个模型保留最近50次

function loadHistory(): Map<number, PerfEntry> {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (!raw) return new Map()
    const data = JSON.parse(raw) as PerfEntry[]
    const map = new Map<number, PerfEntry>()
    for (const entry of data) {
      map.set(entry.modelId, entry)
    }
    return map
  } catch {
    return new Map()
  }
}

function saveHistory(map: Map<number, PerfEntry>) {
  try {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(Array.from(map.values())))
  } catch { /* ignore */ }
}

export function useModelPerformance() {
  const [history, setHistory] = useState<Map<number, PerfEntry>>(() => loadHistory())

  // Persist on change
  useEffect(() => {
    saveHistory(history)
  }, [history])

  const recordCompletion = useCallback((modelId: number, durationMs: number, success: boolean) => {
    setHistory((prev) => {
      const next = new Map(prev)
      const entry = next.get(modelId) ?? {
        modelId,
        durations: [],
        successes: 0,
        failures: 0,
        lastUsed: 0,
      }
      const durations = [...entry.durations, durationMs]
      if (durations.length > MAX_ENTRIES) durations.shift()
      next.set(modelId, {
        ...entry,
        durations,
        successes: entry.successes + (success ? 1 : 0),
        failures: entry.failures + (success ? 0 : 1),
        lastUsed: Date.now(),
      })
      return next
    })
  }, [])

  const getStats = useCallback((modelId: number): ModelPerfStats | null => {
    const entry = history.get(modelId)
    if (!entry || entry.durations.length === 0) return null
    const avg = entry.durations.reduce((a, b) => a + b, 0) / entry.durations.length
    const total = entry.successes + entry.failures
    return {
      avgDuration: Math.round(avg),
      successRate: total > 0 ? Math.round((entry.successes / total) * 100) : 100,
      lastUsed: entry.lastUsed,
      callCount: total,
    }
  }, [history])

  return { recordCompletion, getStats }
}
