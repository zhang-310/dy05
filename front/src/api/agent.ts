import request from '@/utils/request'
import {
  isRecord,
  normalizeArray,
  normalizePage,
  normalizeRecord,
  normalizeStringArray,
} from '@/utils/response-normalize'

export interface Agent {
  id: number; agentName: string; description: string; agentType: number
  systemPrompt?: string; modelConfig?: string; responseMode: number
  status: number; createTime: string; availableTools?: string
  ratingCount?: number; averageRating?: number; conversationCount?: number
}
export interface AgentQuery { page?: number; rows?: number; agentName?: string; agentType?: number; status?: number; sortBy?: string }
export interface AgentSave {
  id?: number; agentName: string; description?: string; agentType: number
  systemPrompt?: string; modelConfig?: string; responseMode?: number; status?: number; availableTools?: string
}

export interface Conversation {
  id: number
  title: string
  lastMessage?: string
  updateTime?: string
  createTime: string
}

export interface ToolCall {
  id: string
  type: string
  function?: { name: string; arguments: string }
  result?: string
}

export interface ChatMessage {
  id: number
  role: 'user' | 'assistant' | 'tool'
  content: string
  toolCalls?: ToolCall[]
  rating?: 'up' | 'down' | null
  createdAt: string
  tokenUsage?: { input: number; output: number }
}

function toNumber(value: unknown, fallback = 0): number {
  if (typeof value === 'number' && Number.isFinite(value)) return value
  if (typeof value === 'string' && value.trim() !== '') {
    const parsed = Number(value)
    if (Number.isFinite(parsed)) return parsed
  }
  return fallback
}

function toText(value: unknown, fallback = ''): string {
  if (value === undefined || value === null) return fallback
  return String(value)
}

function toOptionalText(value: unknown): string | undefined {
  if (value === undefined || value === null) return undefined
  return String(value)
}

function toOptionalNumber(value: unknown): number | undefined {
  if (value === undefined || value === null || value === '') return undefined
  const parsed = toNumber(value, Number.NaN)
  return Number.isFinite(parsed) ? parsed : undefined
}

function toJsonText(value: unknown): string | undefined {
  if (value === undefined || value === null) return undefined
  return typeof value === 'string' ? value : JSON.stringify(value)
}

function normalizeAgent(raw: unknown): Agent {
  const row = normalizeRecord(raw)
  return {
    id: toNumber(row.id),
    agentName: toText(row.agentName ?? row.name, '未命名智能体'),
    description: toText(row.description),
    agentType: toNumber(row.agentType ?? row.type),
    systemPrompt: toOptionalText(row.systemPrompt ?? row.prompt),
    modelConfig: toOptionalText(row.modelConfig ?? row.model),
    responseMode: toNumber(row.responseMode ?? row.mode),
    status: toNumber(row.status, 1),
    createTime: toText(row.createTime ?? row.createdAt),
    availableTools: toJsonText(row.availableTools ?? row.tools),
    ratingCount: toOptionalNumber(row.ratingCount),
    averageRating: toOptionalNumber(row.averageRating),
    conversationCount: toOptionalNumber(row.conversationCount),
  }
}

function normalizeConversation(raw: unknown): Conversation {
  const row = normalizeRecord(raw)
  const id = toNumber(row.id)
  const title = toText(row.title ?? row.conversationTopic ?? row.topic ?? id, '对话')
  return {
    id,
    title,
    lastMessage: row.lastMessage != null ? String(row.lastMessage) : undefined,
    updateTime: row.updateTime != null
      ? String(row.updateTime)
      : row.lastMessageTime != null
        ? String(row.lastMessageTime)
        : undefined,
    createTime: toText(row.createTime ?? row.createdAt),
  }
}

function normalizeRole(role: unknown, senderType: unknown): ChatMessage['role'] {
  if (role === 'user' || role === 'assistant' || role === 'tool') return role
  const type = toNumber(senderType, 2)
  if (type === 1) return 'user'
  if (type === 3) return 'tool'
  return 'assistant'
}

function normalizeToolCall(raw: unknown, index: number): ToolCall | null {
  if (typeof raw === 'string') {
    const name = raw.trim()
    return name ? { id: `tool-${index}`, type: 'function', function: { name, arguments: '' } } : null
  }
  if (!isRecord(raw)) return null

  const fn = isRecord(raw.function) ? raw.function : undefined
  const name = toText(fn?.name ?? raw.toolName ?? raw.name ?? raw.tool ?? raw.type, 'tool')
  const args = toText(fn?.arguments ?? raw.arguments)
  return {
    id: toText(raw.id ?? `${name}-${toNumber(raw.round, index)}`),
    type: toText(raw.type ?? 'function', 'function'),
    function: { name, arguments: args },
    result: raw.result != null
      ? String(raw.result)
      : raw.error != null
        ? String(raw.error)
        : undefined,
  }
}

function normalizeToolCalls(raw: unknown): ToolCall[] {
  if (typeof raw === 'string') {
    const value = raw.trim()
    if (!value) return []
    try {
      return normalizeToolCalls(JSON.parse(value))
    } catch {
      return []
    }
  }
  return normalizeArray<unknown>(raw)
    .map(normalizeToolCall)
    .filter((toolCall): toolCall is ToolCall => toolCall !== null)
}

function normalizeChatMessage(raw: unknown): ChatMessage {
  const row = normalizeRecord(raw)
  const role = normalizeRole(row.role, row.senderType)
  const tokens = toNumber(row.tokens)
  const rawUsage = isRecord(row.tokenUsage) ? row.tokenUsage : undefined
  const tokenUsage = rawUsage
    ? { input: toNumber(rawUsage.input), output: toNumber(rawUsage.output) }
    : {
        input: role === 'user' ? tokens : 0,
        output: role === 'assistant' ? tokens : 0,
      }
  const rating = row.rating === 'up' || row.rating === 'down' ? row.rating : null

  return {
    id: toNumber(row.id),
    role,
    content: toText(row.content),
    toolCalls: normalizeToolCalls(row.toolCalls),
    rating,
    createdAt: toText(row.createdAt ?? row.createTime),
    tokenUsage,
  }
}

function normalizeWorkflowStep(raw: unknown, index: number): WorkflowStep {
  const row = normalizeRecord(raw)
  return {
    id: row.id != null ? toNumber(row.id) : undefined,
    stepOrder: toNumber(row.stepOrder ?? row.orderNo ?? row.sequenceNo, index + 1),
    agentId: toNumber(row.agentId),
    agentName: row.agentName != null ? String(row.agentName) : undefined,
    stepName: row.stepName != null ? String(row.stepName) : undefined,
    inputTemplate: row.inputTemplate != null ? String(row.inputTemplate) : undefined,
    outputKey: row.outputKey != null ? String(row.outputKey) : undefined,
    skipCondition: row.skipCondition != null ? String(row.skipCondition) : undefined,
    dependsOn: normalizeStringArray(row.dependsOn),
    executionMode: toNumber(row.executionMode, 0),
    retryCount: toNumber(row.retryCount, 1),
    timeoutSeconds: toNumber(row.timeoutSeconds, 300),
  }
}

function normalizeWorkflow(raw: unknown): AgentWorkflow {
  const row = normalizeRecord(raw)
  const steps = normalizeArray<unknown>(row.steps ?? row.workflowSteps).map(normalizeWorkflowStep)
  return {
    id: toNumber(row.id),
    userId: toNumber(row.userId),
    name: toText(row.name ?? row.workflowName, '未命名工作流'),
    description: row.description != null ? String(row.description) : undefined,
    version: toNumber(row.version, 1),
    status: toNumber(row.status, 1),
    createTime: toText(row.createTime ?? row.createdAt),
    updateTime: row.updateTime != null ? String(row.updateTime) : undefined,
    steps,
  }
}

function normalizeShare(raw: unknown): AgentShare {
  const row = normalizeRecord(raw)
  return {
    id: toNumber(row.id),
    shareCode: toText(row.shareCode ?? row.code),
    conversationId: toNumber(row.conversationId),
    agentId: toNumber(row.agentId),
    agentName: toOptionalText(row.agentName),
    ownerName: toOptionalText(row.ownerName),
    title: toText(row.title, '分享对话'),
    summary: toOptionalText(row.summary),
    messageCount: toNumber(row.messageCount),
    viewCount: toNumber(row.viewCount),
    isPublic: toNumber(row.isPublic, 1),
    expiresAt: toOptionalText(row.expiresAt),
    createTime: toOptionalText(row.createTime ?? row.createdAt),
    shareUrl: toOptionalText(row.shareUrl),
  }
}

function normalizeAgentReview(raw: unknown): AgentReview {
  const row = normalizeRecord(raw)
  return {
    id: toNumber(row.id),
    agentId: toNumber(row.agentId),
    userId: toNumber(row.userId),
    userName: toOptionalText(row.userName ?? row.username),
    rating: toNumber(row.rating),
    content: toOptionalText(row.content),
    replyContent: toOptionalText(row.replyContent),
    replyTime: toOptionalText(row.replyTime),
    createdAt: toText(row.createdAt ?? row.createTime),
  }
}

function normalizeNullableReview(raw: unknown): AgentReview | null {
  const row = normalizeRecord(raw)
  return Object.keys(row).length > 0 ? normalizeAgentReview(row) : null
}

function normalizeRatingStats(raw: unknown): RatingStats {
  const row = normalizeRecord(raw)
  const distributionSource = isRecord(row.distribution) ? row.distribution : {}
  return {
    agentId: toNumber(row.agentId),
    averageRating: toNumber(row.averageRating),
    reviewCount: toNumber(row.reviewCount ?? row.count),
    distribution: Object.fromEntries(
      Object.entries(distributionSource).map(([key, value]) => [key, toNumber(value)])
    ),
  }
}

function normalizeWorkflowStepResult(raw: unknown, index: number): WorkflowStepResult {
  const row = normalizeRecord(raw)
  return {
    stepOrder: toNumber(row.stepOrder ?? row.orderNo ?? row.sequenceNo, index + 1),
    stepName: toText(row.stepName ?? row.name, `步骤 ${index + 1}`),
    agentId: toNumber(row.agentId),
    outputKey: toOptionalText(row.outputKey),
    output: toOptionalText(row.output),
    toolCallCount: toOptionalNumber(row.toolCallCount),
    success: row.success === undefined ? undefined : Boolean(row.success),
    skipped: row.skipped === undefined ? undefined : Boolean(row.skipped),
    reason: toOptionalText(row.reason),
    error: toOptionalText(row.error ?? row.errorMessage),
  }
}

function normalizeContextData(raw: unknown): Record<string, string> | undefined {
  const row = normalizeRecord(raw)
  if (Object.keys(row).length === 0) return undefined
  return Object.fromEntries(Object.entries(row).map(([key, value]) => [key, String(value ?? '')]))
}

function normalizeWorkflowExecution(raw: unknown): WorkflowExecution {
  const row = normalizeRecord(raw)
  return {
    id: toNumber(row.id),
    workflowId: toNumber(row.workflowId),
    workflowName: toOptionalText(row.workflowName),
    userId: toNumber(row.userId),
    status: toNumber(row.status),
    statusLabel: toText(row.statusLabel ?? row.statusText ?? row.status_),
    currentStepOrder: toNumber(row.currentStepOrder),
    totalSteps: toNumber(row.totalSteps),
    contextData: normalizeContextData(row.contextData),
    startTime: toOptionalText(row.startTime),
    endTime: toOptionalText(row.endTime),
    durationSeconds: toOptionalNumber(row.durationSeconds),
    errorMessage: toOptionalText(row.errorMessage),
    dagLayers: toOptionalNumber(row.dagLayers),
    steps: normalizeArray<unknown>(row.steps ?? row.stepResults).map(normalizeWorkflowStepResult),
    completedSteps: toOptionalNumber(row.completedSteps),
    finalOutput: toOptionalText(row.finalOutput),
    status_: toOptionalText(row.status_),
  }
}

export const agentApi = {
  list: (params: AgentQuery) =>
    request.post<unknown>('/agent/list', params).then((data) =>
      normalizePage<unknown, Agent>(data, normalizeAgent, params.page ?? 0, params.rows ?? 20)
    ),
  get: (id: number) => request.post<unknown>('/agent/get', { id }).then(normalizeAgent),
  save: (params: Partial<AgentSave>) => request.post<number>('/agent/save', params),
  delete: (id: number) => request.post<void>('/agent/delete', undefined, { params: { id } }),
  updateStatus: (id: number, status: number) => request.post<void>('/agent/update-status', undefined, { params: { id, status } }),

  // Conversation
  conversationCreate: (agentId: number, title?: string) => request.post<number>('/agent/conversation/create', { agentId, topic: title }),
  conversationList: (agentId: number) =>
    request.post<unknown>('/agent/conversation/list', { agentId }).then((data) =>
      normalizeArray<unknown>(data).map(normalizeConversation)
    ),
  conversationDelete: (id: number) => request.post<void>('/agent/conversation/delete', undefined, { params: { id } }),

  // Message
  messageSend: (conversationId: number, content: string) =>
    request.post<number>('/agent/message/send', { conversationId, senderType: 1, content, tokens: 0 }),
  messageList: (conversationId: number, params?: { page?: number; rows?: number }) =>
    request.post<unknown>('/agent/message/list', { conversationId, page: params?.page, rows: params?.rows }).then((data) =>
      normalizeArray<unknown>(data).map(normalizeChatMessage)
    ),

  // Conversation management
  conversationRename: (id: number, title: string) => request.post<void>('/agent/conversation/rename', { id, title }),

  // Message rating (👍👎)
  messageRate: (messageId: number, rating: 'up' | 'down') => request.post<void>('/agent/message/rate', { messageId, rating }),

  // Export conversation to markdown
  exportConversation: (conversationId: number) => request.post<{ downloadUrl: string }>('/agent/conversation/export', { conversationId }),

  // SSE chat stream (use native EventSource or fetch)
  chatStreamUrl: '/api/v1/agent/chat-stream',
}

// ============ Share APIs ============

export interface AgentShare {
  id: number; shareCode: string; conversationId: number; agentId: number
  agentName?: string; ownerName?: string; title: string; summary?: string
  messageCount: number; viewCount: number; isPublic: number
  expiresAt?: string; createTime?: string; shareUrl?: string
}

export interface ShareCreate {
  conversationId: number; title?: string; summary?: string
  isPublic?: number; expiresDays?: number
}

export const shareApi = {
  create: (params: ShareCreate) => request.post<unknown>('/agent/share/create', params).then(normalizeShare),
  get: (shareCode: string) => request.post<unknown>('/agent/share/get', { shareCode }).then(normalizeShare),
  list: () => request.post<unknown>('/agent/share/list', {}).then((data) => normalizeArray<unknown>(data).map(normalizeShare)),
  delete: (shareId: number) => request.post<void>('/agent/share/delete', { shareId }),
  getData: (shareCode: string) =>
    request.post<unknown>('/agent/share/data', { shareCode }).then((data) => {
      const row = normalizeRecord(data)
      return {
        share: normalizeShare(row.share ?? row.conversationShare ?? row),
        messages: normalizeArray<unknown>(row.messages ?? row.messageList ?? row.records ?? row.list ?? row.items)
          .map(normalizeChatMessage),
      }
    }),
}

// ============ Review APIs ============

export interface AgentReview {
  id: number; agentId: number; userId: number; userName?: string
  rating: number; content?: string; replyContent?: string
  replyTime?: string; createdAt: string
}

export interface RatingStats {
  agentId: number; averageRating: number; reviewCount: number
  distribution: Record<string, number>
}

export const reviewApi = {
  stats: (agentId: number) => request.post<unknown>('/agent/review/stats', { agentId }).then(normalizeRatingStats),
  list: (agentId: number, page = 0, rows = 10) =>
    request.post<unknown>('/agent/review/list', { agentId, page, rows }).then((data) =>
      normalizePage<unknown, AgentReview>(data, normalizeAgentReview, page, rows)
    ),
  submit: (agentId: number, rating: number, content?: string) =>
    request.post<number>('/agent/review/submit', { agentId, rating, content }),
  myReview: (agentId: number) =>
    request.post<unknown>('/agent/review/my', { agentId }).then(normalizeNullableReview),
}

// ============ Workflow APIs ============

export interface WorkflowStep {
  id?: number
  stepOrder: number
  agentId: number
  agentName?: string
  stepName?: string
  inputTemplate?: string
  outputKey?: string
  skipCondition?: string
  dependsOn?: string[]   // JSON parsed
  executionMode?: number // 0=sequential, 1=parallel
  retryCount?: number
  timeoutSeconds?: number
}

export interface AgentWorkflow {
  id: number; userId: number; name: string; description?: string
  version: number; status: number; createTime: string; updateTime?: string
  steps: WorkflowStep[]
}

export interface WorkflowSave {
  id?: number; name: string; description?: string; status?: number
  steps: Omit<WorkflowStep, 'id' | 'agentName'>[]
}

export interface WorkflowExecution {
  id: number; workflowId: number; workflowName?: string; userId: number
  status: number; statusLabel: string
  currentStepOrder: number; totalSteps: number
  contextData?: Record<string, string>
  startTime?: string; endTime?: string; durationSeconds?: number
  errorMessage?: string
  dagLayers?: number
  steps?: WorkflowStepResult[]
  completedSteps?: number
  finalOutput?: string
  status_?: string // "completed" | "failed"
}

export interface WorkflowStepResult {
  stepOrder: number; stepName: string; agentId: number; outputKey?: string
  output?: string; toolCallCount?: number; success?: boolean
  skipped?: boolean; reason?: string; error?: string
}

export const workflowApi = {
  list: (page = 0, rows = 10) =>
    request.post<unknown>('/agent/workflow/list', { page, rows }).then((data) =>
      normalizePage<unknown, AgentWorkflow>(data, normalizeWorkflow, page, rows)
    ),
  get: (id: number) =>
    request.post<unknown>('/agent/workflow/get', { id }).then(normalizeWorkflow),
  save: (params: WorkflowSave) =>
    request.post<number>('/agent/workflow/save', params),
  delete: (id: number) =>
    request.post<void>('/agent/workflow/delete', { id }),
  execute: (workflowId: number, userInput: string, conversationId?: number) =>
    request.post<WorkflowExecution>('/agent/workflow/execute', {
      workflowId, userInput, conversationId,
    }),
  // Execution history
  executionList: (page = 0, rows = 10) =>
    request.post<unknown>('/agent/workflow/execution/list', { page, rows }).then((data) =>
      normalizePage<unknown, WorkflowExecution>(data, normalizeWorkflowExecution, page, rows)
    ),
  workflowExecutionList: (workflowId: number, page = 0, rows = 10) =>
    request.post<unknown>('/agent/workflow/execution/workflow-list', { workflowId, page, rows }).then((data) =>
      normalizePage<unknown, WorkflowExecution>(data, normalizeWorkflowExecution, page, rows)
    ),
  executionGet: (id: number) =>
    request.post<unknown>('/agent/workflow/execution/get', { id }).then(normalizeWorkflowExecution),
}
