/** L-3：与后端 effectiveness completionSource 对齐（EffectivenessScoreServiceImpl） */
export function completionSourceShortLabel(src: string | undefined): string {
  switch (src) {
    case 'douyin_official_completion':
      return '官方适配'
    case 'retention_peak':
      return '监控推算'
    case 'monitor_derived':
      return '面板采样'
    case 'configured_fallback':
      return '配置回退'
    case 'legacy_unknown':
      return '历史未标'
    case 'none':
      return '无样本'
    default:
      return '代理'
  }
}

export function completionSourceChipColor(
  src: string | undefined
): 'default' | 'primary' | 'secondary' | 'success' | 'warning' | 'error' {
  if (src === 'douyin_official_completion') return 'success'
  if (src === 'retention_peak') return 'primary'
  if (src === 'monitor_derived') return 'success'
  if (src === 'configured_fallback' || src === 'legacy_unknown') return 'warning'
  if (src === 'none') return 'error'
  return 'default'
}
