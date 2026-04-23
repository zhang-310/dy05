/**
 * 表演指导片段：与后端 LiveScriptQualityServiceImpl 中 【…】 提取规则一致（内文 2–30 字）。
 */

/** non-global：避免模块级 /g 正则污染 lastIndex */
const PERFORMANCE_CUE_INNER = /【([^】]{2,30})】/

export type PerformanceCueSegment =
  | { kind: 'text'; text: string }
  | { kind: 'cue'; text: string }

export function parsePerformanceCueSegments(content: string): PerformanceCueSegment[] {
  const s = content ?? ''
  const out: PerformanceCueSegment[] = []
  const re = new RegExp(PERFORMANCE_CUE_INNER.source, 'g')
  let last = 0
  let m: RegExpExecArray | null
  while ((m = re.exec(s)) !== null) {
    if (m.index > last) {
      out.push({ kind: 'text', text: s.slice(last, m.index) })
    }
    out.push({ kind: 'cue', text: m[1] })
    last = re.lastIndex
  }
  if (last < s.length) {
    out.push({ kind: 'text', text: s.slice(last) })
  }
  return out
}

export function listPerformanceCues(content: string): string[] {
  const s = content ?? ''
  const re = new RegExp(PERFORMANCE_CUE_INNER.source, 'g')
  const cues: string[] = []
  let m: RegExpExecArray | null
  while ((m = re.exec(s)) !== null) {
    cues.push(m[1])
  }
  return cues
}
