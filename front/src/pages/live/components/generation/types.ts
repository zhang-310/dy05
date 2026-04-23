export interface FlowStep {
  label: string
  status: 'pending' | 'loading' | 'done' | 'failed'
  content?: string
  scriptId?: number
  errorMsg?: string
  stepKey?: string
  startTime?: number
  endTime?: number
  /** AI 置信度评分 0-100，低于 60 标记为需人工复核 */
  confidence?: number
  /** RAG 参考来源 */
  ragRefs?: { docTitle?: string; score?: number; snippet?: string }[]
  /** 流式生成中的部分内容 */
  streamingContent?: string
  /** 重新生成前的旧内容 */
  previousContent?: string
  /** 逐段质检评分 */
  qualityScore?: 'checking' | 'pass' | 'warning' | 'fail'
  /** 质检问题列表 */
  qualityIssues?: string[]
}

export interface GroupedSteps {
  groupLabel: string
  steps: FlowStep[]
}
