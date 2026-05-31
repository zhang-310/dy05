/** POST /ai/evolution-review/stats */
export interface EvolutionReviewStats {
  pending?: number
  pendingCount?: number
  approved?: number
  approvedCount?: number
  rejected?: number
  rejectedCount?: number
  revised?: number
  revisedCount?: number
  approvalRate7d?: number
}
export interface EvolveRoiPayload {
  newKnowledge?: number
  avgScore?: number
  totalRuns?: number
  coveredDocs?: number
  totalTasks?: number
  successRate?: number
}

/** POST /ai/evolution/status 仪表盘 */
export interface EvolveStatusPayload {
  running?: boolean
  circuitBroken?: boolean
  lastRunTime?: string
  nextRunTime?: string
  topicCount?: number
}

/** POST /ai/evolution/quality-score/history、/score-trend 数据点 */
export interface QualityScoreTrendPoint {
  date: string
  score: number
  count?: number
}

/** POST /ai/evolution-review/list 行（与 EvolutionReviewServiceImpl.toMap 对齐） */
export interface EvolutionReviewTaskRow {
  id: number
  evolveTaskId?: number
  contentPreview?: string
  content?: string
  qualityScore?: number
  reviewStatus?: string
  reviewComment?: string
  revisedContent?: string
  createTime?: string
  reviewedAt?: string
}
