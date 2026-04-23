/**
 * 片段转场（V-2）：xfade 白名单与后端 `VideoEditServiceImpl.XFADE_VIDEO_TRANSITIONS` 一致；
 * `cut` / `none` 为成片管线语义，多段 xfade 时非法名由服务端回退 `fade` 并打 warn。
 */
export const SHORTVIDEO_XFADE_MENU: ReadonlyArray<{ value: string; label: string }> = [
  { value: 'fade', label: '淡入淡出（fade · FFmpeg xfade）' },
  { value: 'slideleft', label: '滑入向左（slideleft · xfade）' },
  { value: 'slideright', label: '滑入向右（slideright · xfade）' },
  { value: 'slideup', label: '滑入向上（slideup · xfade）' },
  { value: 'slidedown', label: '滑入向下（slidedown · xfade）' },
  { value: 'wipeleft', label: '擦除向左（wipeleft · xfade）' },
  { value: 'wiperight', label: '擦除向右（wiperight · xfade）' },
  { value: 'wipeup', label: '擦除向上（wipeup · xfade）' },
  { value: 'wipedown', label: '擦除向下（wipedown · xfade）' },
  { value: 'circleopen', label: '圆形展开（circleopen · xfade）' },
  { value: 'circleclose', label: '圆形收缩（circleclose · xfade）' },
  { value: 'radial', label: '径向（radial · xfade）' },
  { value: 'diagtl', label: '对角左上（diagtl · xfade）' },
  { value: 'diagtr', label: '对角右上（diagtr · xfade）' },
  { value: 'diagbl', label: '对角左下（diagbl · xfade）' },
  { value: 'diagbr', label: '对角右下（diagbr · xfade）' },
]

export const SHORTVIDEO_TRANSITION_NON_XFADE: ReadonlyArray<{ value: string; label: string }> = [
  { value: 'cut', label: '硬切（cut）' },
  { value: 'none', label: '无转场 / 硬切（none）' },
]
