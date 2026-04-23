/** 金额格式化：null→'-', NaN→原始值, 正常→'¥1,234.56' */
export function formatMoney(val: unknown): string {
  if (val == null) return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return '¥' + n.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

/**
 * GMV 统一格式化函数。
 * @param val  GMV 数值（元为单位，支持 number/string/null/undefined）
 * @param options.compact  是否使用紧凑格式（如 1.2万、34.5万），默认 true
 * @param options.unit     货币单位前缀，默认 '¥'
 */
export function formatGmv(
  val: unknown,
  options: { compact?: boolean; unit?: string } = {}
): string {
  if (val == null) return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)

  const unit = options.unit ?? '¥'
  const compact = options.compact ?? true

  if (compact) {
    if (n >= 100_000_000) return unit + (n / 100_000_000).toFixed(2) + '亿'
    if (n >= 10_000) return unit + (n / 10_000).toFixed(2) + '万'
  }

  return unit + n.toLocaleString('zh-CN', { minimumFractionDigits: 2 })
}

/** 百分比格式化：0.28 → '28%' */
export function formatPercent(val: unknown, decimals = 1): string {
  if (val == null) return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return '-'
  return (n * 100).toFixed(decimals) + '%'
}

/** 数字格式化（千分位），null → '-' */
export function formatNumber(val: unknown): string {
  if (val == null) return '-'
  const n = Number(val)
  if (Number.isNaN(n)) return String(val)
  return n.toLocaleString('zh-CN')
}

/** 截断日期时间字符串到分钟：'2026-03-29T14:30:00' → '2026-03-29 14:30' */
export function formatDateTime(val: unknown): string {
  if (val == null) return '-'
  const s = String(val)
  if (s.length >= 16) return s.slice(0, 16).replace('T', ' ')
  return s
}
