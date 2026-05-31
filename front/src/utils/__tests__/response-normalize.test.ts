import { describe, expect, it } from 'vitest'
import { isRecord, normalizeArray, normalizePage, normalizeRecord, normalizeStringArray, readNumberValue } from '../response-normalize'

describe('response-normalize', () => {
  it('unwraps live session arrays from result.sessions wrappers', () => {
    expect(normalizeArray<{ id: number }>({ result: { sessions: [{ id: 19 }] } })).toEqual([{ id: 19 }])
  })

  it('builds stable pages from wrapped session arrays', () => {
    expect(normalizePage<{ id: string }, { id: number }>(
      { result: { sessions: [{ id: '19' }], totalRecords: '1' } },
      row => ({ id: Number(row.id) }),
      0,
      100,
    )).toEqual({
      total: 1,
      list: [{ id: 19 }],
      pageNum: 0,
      pageSize: 100,
    })
  })

  it('does not treat arrays as records', () => {
    expect(isRecord([])).toBe(false)
    expect(isRecord({ data: [] })).toBe(true)
  })

  it('unwraps serialized nested list envelopes', () => {
    expect(normalizeArray<{ id: number }>('{"result":{"items":[{"id":7}]}}')).toEqual([{ id: 7 }])
  })

  it('keeps nested empty page metadata instead of falling back to requested values', () => {
    expect(normalizePage<{ id: string }, { id: number }>(
      { data: { records: [], totalElements: '0', page: '2', size: '50' } },
      row => ({ id: Number(row.id) }),
      0,
      20,
    )).toEqual({
      total: 0,
      list: [],
      pageNum: 2,
      pageSize: 50,
    })
  })

  it('returns an empty array for non-list envelopes', () => {
    expect(normalizeArray<{ id: number }>({ status: 200, message: 'ok' })).toEqual([])
  })

  it('unwraps SDK list envelopes shared by storage and organization APIs', () => {
    expect(normalizeArray<{ key: string }>({ payload: { body: { files: [{ key: '5001/banner.png' }] } } })).toEqual([
      { key: '5001/banner.png' },
    ])
    expect(normalizeArray<{ id: number }>({ result: { members: [{ id: 2 }], totalRecords: 1 } })).toEqual([{ id: 2 }])
    expect(normalizeArray<{ id: number }>({ detail: { reviews: [{ id: 3 }] } })).toEqual([{ id: 3 }])
  })

  it('unwraps benchmark SDK business list envelopes', () => {
    expect(normalizeArray<{ id: number }>({ result: { accounts: [{ id: 21 }], totalCount: 3 } })).toEqual([{ id: 21 }])
    expect(normalizeArray<{ id: number }>({ detail: { videos: [{ id: 31 }], totalRecords: 5 } })).toEqual([{ id: 31 }])
    expect(normalizeArray<{ id: number }>({ payload: { scripts: [{ id: 41 }] } })).toEqual([{ id: 41 }])
  })

  it('unwraps shortvideo SDK business list envelopes', () => {
    expect(normalizeArray<{ id: number }>({ data: { tasks: [{ id: 51 }] } })).toEqual([{ id: 51 }])
    expect(normalizeArray<{ id: number }>({ result: { shots: [{ id: 61 }] } })).toEqual([{ id: 61 }])
    expect(normalizeArray<{ title: string }>({ payload: { titleSuggestions: [{ title: '标题A' }] } })).toEqual([{ title: '标题A' }])
    expect(normalizeArray<{ platform: string }>({ body: { platformResults: [{ platform: 'douyin' }] } })).toEqual([{ platform: 'douyin' }])
    expect(normalizeArray<{ keyword: string }>({ payload: { hotTopics: [{ keyword: '包装热点' }] } })).toEqual([{ keyword: '包装热点' }])
    expect(normalizeArray<{ id: number }>({ payload: { dashboardProjects: [{ id: 52 }] } })).toEqual([{ id: 52 }])
    expect(normalizeArray<{ date: string }>({ result: { trend: [{ date: '2026-05-22' }] } })).toEqual([{ date: '2026-05-22' }])
    expect(normalizeArray<{ id: number }>({ payload: { accountCollectTasks: [{ id: 53 }] } })).toEqual([{ id: 53 }])
    expect(normalizeArray<{ id: number }>({ body: { collectTasks: [{ id: 54 }] } })).toEqual([{ id: 54 }])
  })

  it('unwraps AI evolution topic envelopes', () => {
    expect(normalizeArray<{ id: number }>({ data: { result: { topics: [{ id: 71 }] } } })).toEqual([{ id: 71 }])
  })

  it('unwraps AI evolution review task envelopes', () => {
    expect(normalizeArray<{ id: number }>({ payload: { reviewTasks: [{ id: 81 }] } })).toEqual([{ id: 81 }])
  })

  it('unwraps auth administration envelopes', () => {
    expect(normalizeArray<{ id: number }>({ result: { users: [{ id: 91 }] } })).toEqual([{ id: 91 }])
    expect(normalizeArray<{ id: number }>({ payload: { roles: [{ id: 92 }] } })).toEqual([{ id: 92 }])
    expect(normalizeArray<{ id: number }>({ body: { resources: [{ id: 93 }] } })).toEqual([{ id: 93 }])
    expect(normalizeArray<{ id: number }>({ data: { loginLogs: [{ id: 94 }] } })).toEqual([{ id: 94 }])
  })

  it('unwraps system config and storage file envelopes', () => {
    expect(normalizeArray<{ id: number }>({ payload: { configs: [{ id: 101 }] } })).toEqual([{ id: 101 }])
    expect(normalizeArray<{ id: number }>({ result: { sysConfigs: [{ id: 102 }] } })).toEqual([{ id: 102 }])
    expect(normalizeArray<{ key: string }>({ body: { storageFiles: [{ key: '5001/a.png' }] } })).toEqual([{ key: '5001/a.png' }])
    expect(normalizeArray<{ key: string }>({ detail: { files: [{ key: '5001/b.png' }] } })).toEqual([{ key: '5001/b.png' }])
  })

  it('unwraps performance monitoring envelopes', () => {
    expect(normalizeArray<{ time: string }>({ payload: { points: [{ time: '10:00' }] } })).toEqual([{ time: '10:00' }])
    expect(normalizeArray<{ time: string }>({ result: { series: [{ time: '11:00' }] } })).toEqual([{ time: '11:00' }])
    expect(normalizeArray<{ path: string }>({ body: { slowQueries: [{ path: '/api/slow' }] } })).toEqual([{ path: '/api/slow' }])
    expect(normalizeArray<{ path: string }>({ detail: { queries: [{ path: '/api/query' }] } })).toEqual([{ path: '/api/query' }])
    expect(normalizeRecord({ data: { cacheStats: { hitRate: 0.22 } } }, ['cacheStats'])).toEqual({ hitRate: 0.22 })
  })

  it('unwraps system monitoring page business envelopes', () => {
    expect(normalizeArray<{ id: number }>({ payload: { apiLogs: [{ id: 111 }] } })).toEqual([{ id: 111 }])
    expect(normalizeArray<{ id: number }>({ result: { syncLogs: [{ id: 112 }] } })).toEqual([{ id: 112 }])
    expect(normalizeArray<{ id: number }>({ body: { alertRecords: [{ id: 113 }] } })).toEqual([{ id: 113 }])
    expect(normalizeArray<{ id: number }>({ detail: { alerts: [{ id: 114 }] } })).toEqual([{ id: 114 }])
    expect(normalizeArray<{ id: number }>({ data: { externalApis: [{ id: 115 }] } })).toEqual([{ id: 115 }])
    expect(normalizeArray<{ id: number }>({ data: { providers: [{ id: 116 }] } })).toEqual([{ id: 116 }])
  })

  it('unwraps dashboard live session and approval envelopes', () => {
    expect(normalizeArray<{ id: number }>({ payload: { liveSessions: [{ id: 121 }] } })).toEqual([{ id: 121 }])
    expect(normalizeArray<{ id: number }>({ result: { approvals: [{ id: 122 }] } })).toEqual([{ id: 122 }])
    expect(normalizeArray<{ id: number }>({ body: { pendingApprovals: [{ id: 123 }] } })).toEqual([{ id: 123 }])
    expect(normalizeArray<{ id: number }>({ detail: { pendingReviews: [{ id: 124 }] } })).toEqual([{ id: 124 }])
  })

  it('unwraps slang dictionary entry envelopes', () => {
    expect(normalizeArray<{ id: number }>({ payload: { slangEntries: [{ id: 131 }] } })).toEqual([{ id: 131 }])
    expect(normalizeArray<{ id: number }>({ result: { entries: [{ id: 132 }] } })).toEqual([{ id: 132 }])
  })

  it('unwraps string suggestions from nested primitive and object arrays', () => {
    expect(normalizeStringArray({ data: { list: ['库存紧张', { label: '适合敏感肌' }, { text: '注意节奏' }, ''] } })).toEqual([
      '库存紧张',
      '适合敏感肌',
      '注意节奏',
    ])
  })

  it('unwraps business records from named payload fields', () => {
    expect(normalizeRecord({ result: { organization: { orgName: '包装机构' } } }, ['organization', 'org'])).toEqual({
      orgName: '包装机构',
    })
    expect(normalizeRecord({ payload: { review: { reviewId: 5 } } }, ['review'])).toEqual({ reviewId: 5 })
    expect(normalizeRecord({ payload: { dashboardStats: { totalVideoCount: 3 } } }, ['dashboardStats', 'stats', 'summary'])).toEqual({
      totalVideoCount: 3,
    })
    expect(normalizeRecord({ body: { costBreakdown: { total: 1.2 } } }, ['costBreakdown', 'breakdown', 'costs'])).toEqual({
      total: 1.2,
    })
  })

  it('reads finite numeric primitives without converting blanks to zero', () => {
    expect(readNumberValue('22.28')).toBe(22.28)
    expect(readNumberValue(0)).toBe(0)
    expect(readNumberValue('')).toBeUndefined()
    expect(readNumberValue('abc')).toBeUndefined()
  })
})
