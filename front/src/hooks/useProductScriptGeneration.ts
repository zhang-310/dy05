import { useState, useRef, useCallback } from 'react'
import {
  generateMultiStyleScriptsSse,
  type BatchScriptProgressEvent,
} from '@/api/product'

export interface ScriptGenStep {
  style: string
  styleName: string
  status: 'pending' | 'loading' | 'done' | 'failed'
  message?: string
  startTime?: number
  endTime?: number
}

interface StartParams {
  productId: number
  scriptType: string
  styles: string[]
  styleNameMap: Record<string, string>
  fusionMode?: boolean
  personaId?: number
  duration?: number
  scene?: string
  useKbRef?: boolean
}

/**
 * 产品话术 SSE 流式生成 Hook。
 * 管理生成步骤状态、SSE 连接、取消与重试。
 * 支持：多风格独立生成 / 风格融合（fusionMode）/ 场景维度。
 */
export function useProductScriptGeneration(opts?: { onAllDone?: () => void }) {
  const [steps, setSteps] = useState<ScriptGenStep[]>([])
  const [isGenerating, setIsGenerating] = useState(false)
  const [justCompleted, setJustCompleted] = useState(false)
  const abortRef = useRef<{ abort: () => void } | null>(null)
  const paramsRef = useRef<StartParams | null>(null)

  const start = useCallback((params: StartParams) => {
    // cancel previous
    abortRef.current?.abort()

    paramsRef.current = params
    setJustCompleted(false)
    setIsGenerating(true)

    // 融合模式只有一个 step
    const initial: ScriptGenStep[] = params.fusionMode
      ? [{ style: '__fusion__', styleName: '风格融合', status: 'pending' }]
      : params.styles.map((s) => ({
          style: s,
          styleName: params.styleNameMap[s] || s,
          status: 'pending',
        }))
    setSteps(initial)

    const controller = generateMultiStyleScriptsSse(
      {
        productId: params.productId,
        scriptType: params.scriptType,
        styles: params.styles,
        fusionMode: params.fusionMode,
        personaId: params.personaId,
        duration: params.duration,
        scene: params.scene || undefined,
        useKbRef: params.useKbRef,
      },
      {
        onProgress: (evt: BatchScriptProgressEvent) => {
          if (params.fusionMode) {
            // 融合模式：只有一个 step
            setSteps([{
              style: '__fusion__',
              styleName: '风格融合',
              status: evt.success ? 'done' : 'failed',
              message: evt.message,
              endTime: Date.now(),
            }])
          } else {
            setSteps((prev) =>
              prev.map((step) => {
                if (step.style !== evt.style) return step
                return {
                  ...step,
                  status: evt.success ? 'done' : 'failed',
                  message: evt.message,
                  endTime: Date.now(),
                }
              }),
            )
            // mark next pending as loading
            setSteps((prev) => {
              const nextIdx = prev.findIndex((s) => s.status === 'pending')
              if (nextIdx < 0) return prev
              return prev.map((s, i) =>
                i === nextIdx ? { ...s, status: 'loading' as const, startTime: Date.now() } : s,
              )
            })
          }
        },
        onDone: () => {
          setIsGenerating(false)
          // mark any remaining pending/loading as done (safety)
          setSteps((prev) =>
            prev.map((s) =>
              s.status === 'pending' || s.status === 'loading'
                ? { ...s, status: 'done', message: '完成', endTime: Date.now() }
                : s,
            ),
          )
          setJustCompleted(true)
          setTimeout(() => setJustCompleted(false), 300)
          opts?.onAllDone?.()
        },
        onError: (err: Error) => {
          setIsGenerating(false)
          // mark any in-flight steps as failed
          setSteps((prev) =>
            prev.map((s) =>
              s.status === 'pending' || s.status === 'loading'
                ? { ...s, status: 'failed', message: err.message, endTime: Date.now() }
                : s,
            ),
          )
        },
      },
    )

    // mark first step as loading
    setSteps((prev) =>
      prev.map((s, i) => (i === 0 ? { ...s, status: 'loading' as const, startTime: Date.now() } : s)),
    )

    abortRef.current = controller
  }, [opts])

  const cancel = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setIsGenerating(false)
    setSteps((prev) =>
      prev.map((s) =>
        s.status === 'pending' || s.status === 'loading'
          ? { ...s, status: 'failed', message: '已取消' }
          : s,
      ),
    )
  }, [])

  const retryFailed = useCallback(() => {
    const params = paramsRef.current
    if (!params) return
    if (params.fusionMode) {
      // 融合模式重试：直接重新开始
      start(params)
      return
    }
    const failedStyles = steps.filter((s) => s.status === 'failed').map((s) => s.style)
    if (failedStyles.length === 0) return
    start({
      ...params,
      styles: failedStyles,
    })
  }, [steps, start])

  const reset = useCallback(() => {
    abortRef.current?.abort()
    abortRef.current = null
    setSteps([])
    setIsGenerating(false)
    setJustCompleted(false)
    paramsRef.current = null
  }, [])

  return { steps, isGenerating, justCompleted, start, cancel, retryFailed, reset }
}
