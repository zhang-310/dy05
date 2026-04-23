export const DRAMA_GENRES = ['都市', '古装', '悬疑', '甜宠', '搞笑'] as const

export type ContinuityBand = 'good' | 'watch' | 'risk' | 'unknown'

export function bandChipColor(band: ContinuityBand): 'success' | 'warning' | 'error' | 'default' {
  if (band === 'good') return 'success'
  if (band === 'watch') return 'warning'
  if (band === 'risk') return 'error'
  return 'default'
}
