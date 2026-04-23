import { useEffect, useRef } from 'react'
import { useGeneration } from '../contexts'
import { liveApi } from '@/api/live'
import { useToast } from '@/contexts/ToastContext'
import type { LiveScript } from '@/api/live'

export function useAutoQualityCheck(scripts: LiveScript[]) {
  const { genJustCompleted } = useGeneration()
  const toast = useToast()
  const hasRunRef = useRef(false)

  useEffect(() => {
    if (!genJustCompleted) {
      hasRunRef.current = false
      return
    }
    if (hasRunRef.current) return
    hasRunRef.current = true

    const toCheck = scripts.filter(s => (s.scriptContent ?? '').trim().length > 0)
    if (toCheck.length === 0) return

    toast(`生成完成，正在自动检测 ${toCheck.length} 条话术违规...`, 'info')

    Promise.allSettled(toCheck.map(s => liveApi.aiCheckViolation({ scriptId: s.id })))
      .then(results => {
        const failed = results.filter(r => r.status === 'rejected').length
        if (failed > 0) {
          toast(`违规检测完成，${failed} 条检测失败`, 'warning')
        } else {
          toast('自动违规检测完成', 'success')
        }
      })
  }, [genJustCompleted, scripts, toast])
}
