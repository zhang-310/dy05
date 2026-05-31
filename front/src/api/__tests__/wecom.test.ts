import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { wecomApi } from '../wecom'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('wecom API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts robot query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await wecomApi.list({ page: 0, rows: 20, robotName: '通知机器人' })
    expect(mockPost).toHaveBeenCalledWith('/wecom/robot/list', {
      page: 0,
      rows: 20,
      robotName: '通知机器人',
    })
  })

  it('passes request params for robot get and status updates', async () => {
    mockPost.mockResolvedValue({ id: 7 })
    await wecomApi.get(7)
    await wecomApi.updateStatus(7, 0)

    expect(mockPost).toHaveBeenNthCalledWith(1, '/wecom/robot/get', undefined, { params: { id: 7 } })
    expect(mockPost).toHaveBeenNthCalledWith(2, '/wecom/robot/update-status', undefined, { params: { id: 7, status: 0 } })
  })

  it('normalizes robot save without frontend ownerId', async () => {
    mockPost.mockResolvedValue(undefined)
    await wecomApi.save({
      robotName: ' 通知机器人 ',
      webhookUrl: ' https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc ',
      robotType: '',
      description: '',
      status: undefined,
    })

    expect(mockPost).toHaveBeenCalledWith('/wecom/robot/save', {
      id: undefined,
      robotName: '通知机器人',
      webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc',
      robotType: 'custom',
      description: undefined,
      status: 1,
    })
  })

  it('push maps content to backend messageContent contract', async () => {
    mockPost.mockResolvedValue(undefined)
    await wecomApi.push({ robotId: 3, ruleId: 9, content: '直播开始啦' })

    expect(mockPost).toHaveBeenCalledWith('/wecom/push', {
      robotId: 3,
      ruleId: 9,
      messageType: 'text',
      messageContent: '直播开始啦',
    })
  })

  it('ruleList uses the only backend list endpoint and normalizes list result', async () => {
    mockPost.mockResolvedValue([{ id: 1, robotId: 1, ruleName: '开播提醒' }])
    const result = await wecomApi.ruleList({ robotId: 1, rows: 100 })

    expect(mockPost).toHaveBeenCalledWith('/wecom/rule/list', {})
    expect(result).toEqual({
      total: 1,
      list: [expect.objectContaining({ id: 1, robotId: 1, ruleName: '开播提醒' })],
      pageNum: 0,
      pageSize: 100,
    })
  })

  it('normalizes wrapped robot, rule and log responses', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        records: [
          { robotId: 9, name: '包装机器人', webhook: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=wrapped', enabled: 1 },
        ],
        totalElements: 1,
      },
    })
    await expect(wecomApi.list({ page: 0, rows: 20 })).resolves.toMatchObject({
      total: 1,
      list: [expect.objectContaining({ id: 9, robotName: '包装机器人', status: 1 })],
    })

    mockPost.mockResolvedValueOnce({
      data: {
        rows: [
          { ruleId: 3, robotId: 9, name: '包装规则', type: 'manual', content: '推送内容', enabled: 1 },
          { ruleId: 4, robotId: 10, name: '其他规则', type: 'manual', content: '忽略', enabled: 1 },
        ],
        totalRecords: 2,
      },
    })
    await expect(wecomApi.ruleList({ robotId: 9, rows: 50 })).resolves.toMatchObject({
      total: 1,
      pageSize: 50,
      list: [expect.objectContaining({ id: 3, robotId: 9, ruleName: '包装规则', messageTemplate: '推送内容' })],
    })

    mockPost.mockResolvedValueOnce({
      data: {
        content: [
          { logId: 6, robotConfigId: 9, robot_name: '包装机器人', summary: '失败消息', success: 0, error: 'timeout', sentAt: '2026-05-22 10:00:00' },
        ],
        totalCount: 1,
      },
    })
    await expect(wecomApi.logList({ page: 0, rows: 20 })).resolves.toMatchObject({
      total: 1,
      list: [expect.objectContaining({ id: 6, robotId: 9, robotName: '包装机器人', messageContent: '失败消息', errorMessage: 'timeout' })],
    })
  })

  it('normalizes rule save triggerConfig for backend validation', async () => {
    mockPost.mockResolvedValue(undefined)
    await wecomApi.ruleSave({
      robotId: 3,
      ruleName: ' 开播提醒 ',
      triggerType: ' manual ',
      triggerConfig: '',
      messageTemplate: ' 直播开始啦 ',
    })

    expect(mockPost).toHaveBeenCalledWith('/wecom/rule/save', {
      id: undefined,
      robotId: 3,
      ruleName: '开播提醒',
      triggerType: 'manual',
      triggerConfig: '{}',
      messageTemplate: '直播开始啦',
      status: 1,
    })
  })

  it('rule delete and log list use backend contracts', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await wecomApi.ruleDelete(12)
    await wecomApi.logList({ page: 0, rows: 20, status: 0 })

    expect(mockPost).toHaveBeenNthCalledWith(1, '/wecom/rule/delete', undefined, { params: { id: 12 } })
    expect(mockPost).toHaveBeenNthCalledWith(2, '/wecom/log/list', { page: 0, rows: 20, status: 0 })
  })
})
