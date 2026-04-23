/**
 * 统一日期格式化工具
 * 格式：YYYY-MM-DD HH:mm:ss
 */
export function formatDate(v: string | null | undefined): string {
  if (!v) return '-'
  // Replace T separator and truncate to seconds
  return v.replace('T', ' ').slice(0, 19)
}

export function formatDateShort(v: string | null | undefined): string {
  if (!v) return '-'
  return v.replace('T', ' ').slice(0, 16)
}

export function formatDateOnly(v: string | null | undefined): string {
  if (!v) return '-'
  return v.slice(0, 10)
}

/**
 * 解析后端任务 createTime（ISO 字符串、毫秒时间戳、Jackson 常见日期数组等）。
 * 无法解析时返回 NaN；调用方可用「非有限值则不过滤」避免误伤列表。
 */
export function parseBackendDateTimeMs(v: unknown): number {
  if (v == null) return Number.NaN
  if (typeof v === 'number' && Number.isFinite(v)) return v
  if (typeof v === 'string') {
    const t = Date.parse(v)
    return Number.isNaN(t) ? Number.NaN : t
  }
  if (Array.isArray(v)) {
    const [y, m, d, h = 0, min = 0, sec = 0, nano = 0] = v.map((x) => Number(x))
    if (!Number.isFinite(y)) return Number.NaN
    const monthIndex = Number.isFinite(m) ? Math.max(0, Math.floor(m) - 1) : 0
    const day = Number.isFinite(d) ? Math.floor(d) : 1
    const ms = new Date(
      Math.floor(y),
      monthIndex,
      day,
      Number.isFinite(h) ? Math.floor(h) : 0,
      Number.isFinite(min) ? Math.floor(min) : 0,
      Number.isFinite(sec) ? Math.floor(sec) : 0,
      Number.isFinite(nano) ? Math.floor(nano / 1e6) : 0,
    ).getTime()
    return Number.isNaN(ms) ? Number.NaN : ms
  }
  if (typeof v === 'object' && v !== null && 'time' in v) {
    const t = (v as { time?: unknown }).time
    if (typeof t === 'number' && Number.isFinite(t)) return t
  }
  const s = String(v)
  const t = Date.parse(s)
  return Number.isNaN(t) ? Number.NaN : t
}
