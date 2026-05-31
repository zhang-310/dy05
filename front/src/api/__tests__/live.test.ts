/**
 * live API 模块测试（mock request）
 */
import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { liveApi, getSession } from '../live'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('live API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('sessionSearch calls post with correct path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })
    await liveApi.sessionSearch({ page: 0, rows: 10 })
    expect(mockPost).toHaveBeenCalledWith('/live/session/search', { page: 0, rows: 10 })
  })

  it('getSession calls post with id', async () => {
    mockPost.mockResolvedValue({ id: 1, liveTitle: '测试', userId: 1, accountId: 1, personaId: 1, sessionCover: '', scriptStyle: '', liveDescription: '', scheduledTime: '', scheduledEndTime: '', startTime: '', endTime: '', liveUrl: '', viewers: 0, likes: 0, status: 1, sessionType: '', liveFormat: '', createTime: '', updateTime: '' })
    const result = await getSession(1)
    expect(mockPost).toHaveBeenCalledWith('/live/session/get', { id: 1 })
    expect(result.id).toBe(1)
  })

  it('sessionClone posts clone request', async () => {
    mockPost.mockResolvedValue(99)
    await liveApi.sessionClone(99)
    expect(mockPost).toHaveBeenCalledWith('/live/session/clone', {
      id: 99,
      sessionId: 99,
      sourceSessionId: 99,
    })
  })

  it('scriptSave posts script payload', async () => {
    mockPost.mockResolvedValue(11)
    await liveApi.scriptSave({
      sessionId: 1,
      scriptTitle: '开场话术',
      scriptContent: '欢迎来到直播间',
      scriptType: 'opening',
    })
    expect(mockPost).toHaveBeenCalledWith('/live/script/save', {
      sessionId: 1,
      scriptContent: '欢迎来到直播间',
      scriptType: 'opening',
    })
  })

  it('normalizes live script search wrappers and aliases', async () => {
    mockPost.mockResolvedValue({
      records: [
        {
          id: '21',
          sessionId: '18',
          requirement: '修护精华槽位',
          content: '今晚主推修护精华',
          scriptType: '产品介绍',
          sequenceNo: '2',
          durationLimitSec: '90',
          executed: '1',
          aiGenerated: '1',
          violationChecked: '0',
          createTime: '2026-05-22T20:00:00',
        },
      ],
      totalElements: '1',
      page: '0',
      size: '20',
    })

    const result = await liveApi.scriptSearch({ page: 0, rows: 20, sessionId: 18, scriptType: '产品介绍', keyword: '修护', executed: 1 })

    expect(mockPost).toHaveBeenCalledWith('/live/script/search', {
      page: 0,
      rows: 20,
      sessionId: 18,
      scriptType: '产品介绍',
      keyword: '修护',
      executed: 1,
    })
    expect(result).toMatchObject({
      total: 1,
      pageNum: 0,
      pageSize: 20,
    })
    expect(result.list[0]).toMatchObject({
      id: 21,
      sessionId: 18,
      scriptTitle: '修护精华槽位',
      scriptContent: '今晚主推修护精华',
      sequenceNo: 2,
      sortOrder: 2,
      durationLimitSec: 90,
      duration: 90,
      executed: 1,
      aiGenerated: true,
      violationChecked: false,
    })
  })

  it('maps live script save payload to real backend fields', async () => {
    mockPost.mockResolvedValue(22)

    await liveApi.scriptSave({
      id: 21,
      sessionId: 18,
      scriptTitle: '不会提交到后端',
      scriptContent: '更新后的话术',
      scriptType: '产品介绍',
      requirement: '修护精华槽位',
      sortOrder: 3,
      duration: 120,
    })

    expect(mockPost).toHaveBeenCalledWith('/live/script/save', {
      id: 21,
      sessionId: 18,
      scriptContent: '更新后的话术',
      scriptType: '产品介绍',
      requirement: '修护精华槽位',
      sequenceNo: 3,
      durationLimitSec: 120,
    })
  })

  it('updates script executed status with required backend payload', async () => {
    mockPost.mockResolvedValue(undefined)

    await liveApi.scriptUpdateExecuted({ id: 21, executed: 0 })

    expect(mockPost).toHaveBeenCalledWith('/live/script/executed', { id: 21, executed: 0 })
  })

  it('productBatchSort posts session and product ids', async () => {
    mockPost.mockResolvedValue(undefined)
    await liveApi.productBatchSort(7, [101, 102, 103])
    expect(mockPost).toHaveBeenCalledWith('/live/product/batch-sort', {
      sessionId: 7,
      productIds: [101, 102, 103],
    })
  })

  it('normalizes live session search wrappers for product selectors', async () => {
    mockPost.mockResolvedValue({
      result: {
        sessions: [
          {
            session_id: '18',
            live_title: '晚场修护直播',
            status: '1',
            total_viewers: '1288',
            likes: '88',
            live_format: '双人',
            scheduled_time: '2026-05-21 20:00:00',
            total_gmv: '2300',
          },
        ],
        totalRecords: '1',
        page: '0',
        size: '100',
      },
    })

    const result = await liveApi.sessionSearch({ rows: 100 })

    expect(result.total).toBe(1)
    expect(result.list[0]).toMatchObject({
      id: 18,
      liveTitle: '晚场修护直播',
      status: 1,
      viewers: 1288,
      likes: 88,
      liveFormat: '双人',
      scheduledTime: '2026-05-21 20:00:00',
      totalGmv: '2300',
    })
  })

  it('normalizes live product search wrappers and aliases', async () => {
    mockPost.mockResolvedValue({
      rows: [
        {
          id: '31',
          sessionId: '18',
          productId: '9001',
          productName: '修护精华',
          sales: '12',
          gmv: '4999.5',
          sortOrder: '2',
          productType: 'hot',
          productScriptId: '77',
          createTime: '2026-05-22T20:00:00',
        },
      ],
      totalRecords: '1',
    })

    const result = await liveApi.productSearch({ page: 0, rows: 20, sessionId: 18 })

    expect(mockPost).toHaveBeenCalledWith('/live/product/search', { page: 0, rows: 20, sessionId: 18 })
    expect(result.total).toBe(1)
    expect(result.list[0]).toMatchObject({
      id: 31,
      sessionId: 18,
      productId: 9001,
      productName: '修护精华',
      saleQuantity: 12,
      revenue: 4999.5,
      position: 2,
      productType: 'hot',
      productScriptId: 77,
    })
  })

  it('aiGenerateFull posts generation payload', async () => {
    mockPost.mockResolvedValue([])
    await liveApi.aiGenerateFull({ sessionId: 5, style: 'professional' })
    expect(mockPost).toHaveBeenCalledWith('/live/ai/generate-full', {
      sessionId: 5,
      style: 'professional',
    })
  })

  it('normalizes wrapped live arrays across workbench endpoints', async () => {
    mockPost.mockResolvedValueOnce({
      data: {
        records: [
          { id: '41', sessionId: '5', content: '包装话术', sequenceNo: '1', durationLimitSec: '60' },
        ],
      },
    })
    await expect(liveApi.scriptBySession(5)).resolves.toEqual([
      expect.objectContaining({ id: 41, sessionId: 5, scriptContent: '包装话术', sequenceNo: 1, durationLimitSec: 60 }),
    ])
    expect(mockPost).toHaveBeenLastCalledWith('/live/script/by-session', { sessionId: 5 })

    mockPost.mockResolvedValueOnce({
      data: {
        items: [
          { id: '51', sessionId: '5', productId: '9001', productName: '修护精华', gmv: '2999.5' },
        ],
      },
    })
    await expect(liveApi.productBySession(5)).resolves.toEqual([
      expect.objectContaining({ id: 51, sessionId: 5, productId: 9001, productName: '修护精华', revenue: 2999.5 }),
    ])

    mockPost.mockResolvedValueOnce({ rows: [{ scriptId: 41, score: 88 }] })
    await expect(liveApi.effectivenessTopScripts({ sessionId: 5, limit: 10 })).resolves.toEqual([
      expect.objectContaining({ scriptId: 41, score: 88 }),
    ])

    mockPost.mockResolvedValueOnce({ content: [{ section: 'opening', text: '欢迎来到直播间' }] })
    await expect(liveApi.aiGenerateFull({ sessionId: 5 })).resolves.toEqual([
      expect.objectContaining({ section: 'opening', text: '欢迎来到直播间' }),
    ])

    mockPost.mockResolvedValueOnce({ data: { list: ['库存紧张', '这款适合敏感肌', ''] } })
    await expect(liveApi.danmakuSuggest({ sessionId: 5 })).resolves.toEqual(['库存紧张', '这款适合敏感肌'])

    mockPost.mockResolvedValueOnce({ records: [{ id: '6', title: '待审批直播', status: '0' }] })
    await expect(liveApi.approvalPending()).resolves.toEqual([
      expect.objectContaining({ id: 6, liveTitle: '待审批直播', status: 0 }),
    ])
  })

  it('exposes SSE endpoint constants for live generation', () => {
    expect(liveApi.aiGenerateFullSse).toBe('/live/ai/generate-full-sse')
    expect(liveApi.aiGenerateFullPipelinedSse).toBe('/live/ai/generate-full-pipelined-sse')
    expect(liveApi.aiGenerateSkeletonSse).toBe('/live/ai/generate-skeleton-sse')
  })

  it('normalizes live script version list from real backend path', async () => {
    mockPost.mockResolvedValue([
      {
        id: 10,
        scriptId: 5,
        versionNo: 2,
        versionLabel: '优化版',
        scriptContent: '新版话术',
        versionStatus: 'active',
        createTime: '2026-05-21T10:00:00',
      },
    ])

    const result = await liveApi.versionList(5)

    expect(mockPost).toHaveBeenCalledWith('/live/script/version/getByScriptId', 5)
    expect(result[0]).toMatchObject({
      id: 10,
      scriptId: 5,
      content: '新版话术',
      scriptContent: '新版话术',
      isActive: true,
      status: 1,
    })
  })

  it('normalizes wrapped live script version list from real backend path', async () => {
    mockPost.mockResolvedValue({
      records: [
        {
          id: 11,
          scriptId: 5,
          versionNumber: 3,
          content: '包装话术',
          isCurrent: 1,
          createTime: '2026-05-22T10:00:00',
        },
      ],
      totalElements: 1,
    })

    const result = await liveApi.getVersionsByScriptId(5)

    expect(mockPost).toHaveBeenCalledWith('/live/script/version/getByScriptId', 5)
    expect(result[0]).toMatchObject({
      id: 11,
      scriptId: 5,
      versionNo: 3,
      content: '包装话术',
      scriptContent: '包装话术',
      isActive: true,
    })
  })


  it('saves live script version with real backend payload', async () => {
    mockPost.mockResolvedValue(12)

    await liveApi.versionSave({ scriptId: 5, content: '保存的话术', versionNo: '3' })

    expect(mockPost).toHaveBeenCalledWith('/live/script/version/save', {
      scriptId: 5,
      versionNo: 3,
      versionLabel: undefined,
      scriptContent: '保存的话术',
      scriptType: undefined,
      remark: undefined,
      versionStatus: 'draft',
      effectivenessScore: undefined,
      basedOnVersionId: undefined,
      recommendReason: undefined,
      isRecommended: undefined,
    })
  })

  it('activates and deletes live script versions through real path', async () => {
    mockPost.mockResolvedValue(undefined)

    await liveApi.versionActivate(8)
    await liveApi.versionDelete(9)

    expect(mockPost).toHaveBeenCalledWith('/live/script/version/updateStatus', {
      versionId: 8,
      versionStatus: 'active',
    })
    expect(mockPost).toHaveBeenCalledWith('/live/script/version/delete', 9)
  })

  it('maps version diff aliases for legacy consumers', async () => {
    mockPost.mockResolvedValue({
      oldVersionId: 1,
      newVersionId: 2,
      oldVersionNo: 1,
      newVersionNo: 2,
      oldContent: '旧内容',
      newContent: '新内容',
      similarity: 80,
    })

    const result = await liveApi.versionDiff({ versionId1: 1, versionId2: 2 })

    expect(mockPost).toHaveBeenCalledWith('/live/script/version/diff', {
      oldVersionId: 1,
      newVersionId: 2,
    })
    expect(result.leftContent).toBe('旧内容')
    expect(result.rightContent).toBe('新内容')
    expect(result.versionA).toMatchObject({ id: 1, versionNo: '1' })
  })

  it('uses real rhythm optimize and save paths', async () => {
    mockPost.mockResolvedValue({ status: 'ok' })

    await liveApi.rhythmOptimize(6)
    await liveApi.rhythmSave({ sessionId: 6, slots: [{ scriptId: 1, sequenceNo: 1, durationLimitSec: 60 }] })

    expect(mockPost).toHaveBeenCalledWith('/live/rhythm/optimize', { sessionId: 6 })
    expect(mockPost).toHaveBeenCalledWith('/live/rhythm/save-rhythm', {
      sessionId: 6,
      slots: [{ scriptId: 1, sequenceNo: 1, durationLimitSec: 60 }],
    })
  })
})
