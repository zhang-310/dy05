import request from '@/utils/request'
import type { PageResult } from '@/types/common'

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

export const agentApi = {
  list: (params: AgentQuery) => request.post<PageResult<Agent>>('/agent/list', params),
  get: (id: number) => request.post<Agent>('/agent/get', { id }),
  save: (params: Partial<AgentSave>) => request.post<number>('/agent/save', params),
  delete: (id: number) => request.post<void>('/agent/delete', { id }),
  updateStatus: (id: number, status: number) => request.post<void>('/agent/update-status', { id, status }),

  // Conversation
  conversationCreate: (agentId: number, title?: string) => request.post<number>('/agent/conversation/create', { agentId, title }),
  conversationList: (agentId: number) => request.post<Conversation[]>('/agent/conversation/list', { agentId }),
  conversationDelete: (id: number) => request.post<void>('/agent/conversation/delete', { id }),

  // Message
  messageSend: (conversationId: number, content: string) => request.post<number>('/agent/message/send', { conversationId, content }),
  messageList: (conversationId: number) => request.post<ChatMessage[]>('/agent/message/list', { conversationId }),

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
  create: (params: ShareCreate) => request.post<AgentShare>('/agent/share/create', params),
  get: (shareCode: string) => request.post<AgentShare>('/agent/share/get', { shareCode }),
  list: () => request.post<AgentShare[]>('/agent/share/list', {}),
  delete: (shareId: number) => request.post<void>('/agent/share/delete', { shareId }),
  getData: (shareCode: string) => request.post<{ share: AgentShare; messages: ChatMessage[] }>('/agent/share/data', { shareCode }),
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
  stats: (agentId: number) => request.post<RatingStats>('/agent/review/stats', { agentId }),
  list: (agentId: number, page = 0, rows = 10) =>
    request.post<PageResult<AgentReview>>('/agent/review/list', { agentId, page, rows }),
  submit: (agentId: number, rating: number, content?: string) =>
    request.post<number>('/agent/review/submit', { agentId, rating, content }),
  myReview: (agentId: number) =>
    request.post<AgentReview | null>('/agent/review/my', { agentId }),
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
    request.post<PageResult<AgentWorkflow>>('/agent/workflow/list', { page, rows }),
  get: (id: number) =>
    request.post<AgentWorkflow>('/agent/workflow/get', { id }),
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
    request.post<PageResult<WorkflowExecution>>('/agent/workflow/execution/list', { page, rows }),
  workflowExecutionList: (workflowId: number, page = 0, rows = 10) =>
    request.post<PageResult<WorkflowExecution>>('/agent/workflow/execution/workflow-list', { workflowId, page, rows }),
  executionGet: (id: number) =>
    request.post<WorkflowExecution>('/agent/workflow/execution/get', { id }),
}
