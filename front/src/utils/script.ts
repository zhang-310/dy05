/** 话术工具函数 */

/** 根据字数估算时长（秒），约 3 字/秒 */
export function estimateDuration(words: number): number {
  return Math.ceil(words / 3)
}

/** 根据文本估算时长（秒） */
export function estimateDurationFromText(text: string | null | undefined): number {
  if (!text || typeof text !== 'string') return 0
  const len = text.replace(/\s/g, '').length
  return estimateDuration(len)
}

/** 秒转 mm:ss 格式 */
export function formatDurationSec(sec: number): string {
  const m = Math.floor(sec / 60)
  const s = sec % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}
