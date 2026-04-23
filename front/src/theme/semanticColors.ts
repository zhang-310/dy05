/**
 * D7-A：语义色彩 — Dashboard / KPI 等与业务含义对齐（MUI palette 路径字符串）
 */
export const semanticColors = {
  /** 金额、营收、GMV */
  revenue: 'success.main',
  /** 数量、场次数、计数类 */
  count: 'primary.main',
  /** 告警、待处理、直播中 */
  alert: 'warning.main',
  /** AI、智能体、知识 */
  ai: 'secondary.main',
  /** 系统、日志、技术状态 */
  system: 'grey.600',
} as const

export type SemanticColorKey = keyof typeof semanticColors
