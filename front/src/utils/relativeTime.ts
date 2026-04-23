/** 相对时间：刚刚 / 3 分钟前 / 2 小时前 / 5 天前 */
export function formatRelativeTime(date: Date | string | number): string {
  const d = typeof date === 'object' ? date : new Date(date)
  const now = new Date()
  const diffMs = now.getTime() - d.getTime()
  const diffMin = Math.floor(diffMs / 60000)
  const diffHour = Math.floor(diffMs / 3600000)
  const diffDay = Math.floor(diffMs / 86400000)
  if (diffMin < 1) return '刚刚'
  if (diffMin < 60) return `${diffMin} 分钟前`
  if (diffHour < 24) return `${diffHour} 小时前`
  if (diffDay < 7) return `${diffDay} 天前`
  return d.toLocaleDateString()
}
