/** 素材库：类型映射、标签兜底、元数据展示（供 MaterialLibraryPage 使用） */
export const materialTypeMap: Record<string, string> = {
  image: '图片',
  video: '视频',
  audio: '音频',
}

/** A-2：前端兜底（接口失败时使用）；正常以 /library/tag-options 合并配置+字典+历史 */
export const TAG_PRESETS_FALLBACK = [
  '产品展示',
  '口播',
  '转场',
  '字幕底',
  '品牌',
  '美妆',
  '促销',
  '痛点',
  '证言',
  '切片',
]

export function fmtMaterialMeta(row: {
  width?: number
  height?: number
  duration?: number
  fileSize?: number
}): string {
  const parts: string[] = []
  if (row.width && row.height) parts.push(`${row.width}×${row.height}`)
  if (row.duration != null && row.duration > 0) parts.push(`${row.duration}s`)
  if (row.fileSize != null && row.fileSize > 0) {
    const b = row.fileSize
    if (b < 1024) parts.push(`${b} B`)
    else if (b < 1024 * 1024) parts.push(`${(b / 1024).toFixed(1)} KB`)
    else parts.push(`${(b / (1024 * 1024)).toFixed(1)} MB`)
  }
  return parts.length > 0 ? parts.join(' · ') : '—'
}

export function mergeTagCsv(current: string, add: string): string {
  const raw = `${current},${add}`
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
  return [...new Set(raw)].join(',')
}
