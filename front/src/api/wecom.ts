import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface WcRobot {
  id: number; robotName: string; webhookUrl: string
  robotType: string; description: string; status: number; createTime: string
}
export interface RobotQuery { page?: number; rows?: number; robotName?: string; status?: number }
export interface RobotSave { id?: number; robotName: string; webhookUrl: string; robotType?: string; description?: string; status?: number }

export interface WcRule {
  id: number
  robotId: number
  ruleName: string
  triggerType: string   // manual / schedule / event
  triggerConfig: string
  messageTemplate: string
  status: number
  createTime: string
}
export interface RuleQuery { page?: number; rows?: number; robotId?: number; triggerType?: string; status?: number }
export interface RuleSave { id?: number; robotId: number; ruleName: string; triggerType: string; triggerConfig?: string; messageTemplate: string; status?: number }

export interface WcPushLog {
  id: number
  robotId: number
  robotName: string
  ruleId: number | null
  content: string
  status: number        // 0=失败 1=成功
  errMsg: string | null
  createTime: string
}
export interface PushLogQuery { page?: number; rows?: number; robotId?: number; status?: number; startTime?: string; endTime?: string }

export interface WcMessageTemplate {
  id: number
  robotId: number
  templateName: string
  templateContent: string   // 含 {变量名} 占位符
  variables: string[]       // 解析出的变量名列表
  exampleValues?: Record<string, string>
  status: number
  createTime: string
}

export const wecomApi = {
  // 机器人
  list: (params: RobotQuery) => request.post<PageResult<WcRobot>>('/wecom/robot/list', params),
  get: (id: number) => request.post<WcRobot>('/wecom/robot/get', { id }),
  save: (params: Partial<RobotSave>) => request.post<void>('/wecom/robot/save', params),
  delete: (id: number) => request.post<void>('/wecom/robot/delete', { id }),
  updateStatus: (id: number, status: number) => request.post<void>('/wecom/robot/update-status', { id, status }),
  test: (id: number) => request.post<void>('/wecom/robot/test', { id }),
  push: (params: { robotId: number; content: string }) => request.post<void>('/wecom/robot/push', params),

  // 推送规则
  ruleList: (params: RuleQuery) => request.post<PageResult<WcRule>>('/wecom/rule/list', params),
  ruleGet: (id: number) => request.post<WcRule>('/wecom/rule/get', { id }),
  ruleSave: (params: Partial<RuleSave>) => request.post<void>('/wecom/rule/save', params),
  ruleDelete: (id: number) => request.post<void>('/wecom/rule/delete', { id }),
  ruleUpdateStatus: (id: number, status: number) => request.post<void>('/wecom/rule/update-status', { id, status }),

  // 推送日志
  logList: (params: PushLogQuery) => request.post<PageResult<WcPushLog>>('/wecom/log/list', params),

  // P1: 手动触发推送
  manualPush: (ruleId: number, data?: Record<string, unknown>) =>
    request.post<{ success: boolean; messageId?: string; costMs?: number }>('/wecom/rule/manual-push', { ruleId, ...data }),

  // P1: 推送日志重新发送
  retryPush: (logId: number) =>
    request.post<{ success: boolean; errMsg?: string }>('/wecom/log/retry', { logId }),

  // P1: 推送成功率统计
  pushStats: (params?: { robotId?: number; startTime?: string; endTime?: string }) =>
    request.post<{ total: number; success: number; failed: number; successRate: number; avgCostMs: number }>('/wecom/log/stats', params ?? {}),

  // P1: 消息模板 CRUD
  templateList: (params?: { robotId?: number; page?: number; rows?: number }) =>
    request.post<PageResult<WcMessageTemplate>>('/wecom/template/list', params ?? {}),
  templateGet: (id: number) =>
    request.post<WcMessageTemplate>('/wecom/template/get', { id }),
  templateSave: (params: Partial<WcMessageTemplate>) =>
    request.post<void>('/wecom/template/save', params),
  templateDelete: (id: number) =>
    request.post<void>('/wecom/template/delete', { id }),
}
