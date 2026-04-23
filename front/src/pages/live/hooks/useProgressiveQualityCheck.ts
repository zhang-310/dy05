import { useState, useCallback, useRef } from 'react'
import { checkViolation, type LiveScript } from '@/api/live'

export interface ProgressiveQualityResult {
  scriptId: number
  status: 'checking' | 'pass' | 'warning' | 'fail'
  issues: string[]
}

export interface ProgressiveQualitySummary {
  pass: number
  warning: number
  fail: number
  checking: number
}

export interface UseProgressiveQualityCheckReturn {
  qualityResults: Map<number, ProgressiveQualityResult>
  aggregateSummary: ProgressiveQualitySummary
  enqueueQualityCheck: (scriptId: number) => void
  clearQualityResults: () => void
}

interface Deps {
  scripts: LiveScript[]
}

export function useProgressiveQualityCheck({ scripts }: Deps): UseProgressiveQualityCheckReturn {
  const [qualityResults, setQualityResults] = useState<Map<number, ProgressiveQualityResult>>(new Map())
  const queueRef = useRef<number[]>([])
  const processingRef = useRef(false)

  const processQueue = useCallback(async () => {
    if (processingRef.current) return
    processingRef.current = true

    while (queueRef.current.length > 0) {
      const scriptId = queueRef.current.shift()!
      // Mark as checking
      setQualityResults((prev) => {
        const next = new Map(prev)
        next.set(scriptId, { scriptId, status: 'checking', issues: [] })
        return next
      })

      try {
        const res = await checkViolation({ scriptId })
        const data = res as Record<string, unknown>
        const passed = data.passed !== false && data.violation !== true
        const issues: string[] = []
        if (data.message && typeof data.message === 'string') issues.push(data.message)
        if (Array.isArray(data.violations)) {
          for (const v of data.violations) issues.push(String(v))
        }
        if (Array.isArray(data.issues)) {
          for (const v of data.issues) issues.push(String(v))
        }

        // Client-side checks
        const script = scripts.find((s) => s.id === scriptId)
        if (script) {
          const content = String(script.scriptContent ?? '')
          if (content.length < 20) issues.push('内容过短（少于20字）')
          if (content.length > 2000) issues.push('内容过长（超过2000字）')
        }

        const status = !passed || issues.length > 2 ? 'fail' : issues.length > 0 ? 'warning' : 'pass'
        setQualityResults((prev) => {
          const next = new Map(prev)
          next.set(scriptId, { scriptId, status, issues })
          return next
        })
      } catch {
        setQualityResults((prev) => {
          const next = new Map(prev)
          next.set(scriptId, { scriptId, status: 'warning', issues: ['质检接口调用失败'] })
          return next
        })
      }
    }

    processingRef.current = false
  }, [scripts])

  const enqueueQualityCheck = useCallback((scriptId: number) => {
    queueRef.current.push(scriptId)
    processQueue()
  }, [processQueue])

  const clearQualityResults = useCallback(() => {
    setQualityResults(new Map())
    queueRef.current = []
  }, [])

  const aggregateSummary: ProgressiveQualitySummary = {
    pass: 0,
    warning: 0,
    fail: 0,
    checking: 0,
  }
  qualityResults.forEach((r) => {
    if (r.status === 'pass') aggregateSummary.pass++
    else if (r.status === 'warning') aggregateSummary.warning++
    else if (r.status === 'fail') aggregateSummary.fail++
    else if (r.status === 'checking') aggregateSummary.checking++
  })

  return {
    qualityResults,
    aggregateSummary,
    enqueueQualityCheck,
    clearQualityResults,
  }
}
