import { useRef } from 'react'

/** 素材生产页：长轮询 / 可取消任务 */
export function useMaterialProductionSession() {
  const abortRef = useRef<AbortController | null>(null)
  const pollRef = useRef<ReturnType<typeof setInterval> | null>(null)
  return { abortRef, pollRef }
}
