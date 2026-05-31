import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { logApi } from '../log'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('log API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts operation log query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await logApi.list({ page: 0, rows: 20, username: 'admin' })
    expect(mockPost).toHaveBeenCalledWith('/log/operation/page', {
      page: 0,
      rows: 20,
      username: 'admin',
    })
  })

  it('systemList posts system log query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await logApi.systemList({ page: 1, rows: 50, module: 'auth', eventType: 'startup', status: 1 })
    expect(mockPost).toHaveBeenCalledWith('/log/system/page', {
      page: 1,
      rows: 50,
      module: 'auth',
      eventType: 'startup',
      status: 1,
    })
  })

  it('auditLogPage posts audit search payload to real backend route', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await logApi.auditLogPage({ page: 0, rows: 20, username: 'admin', entity: 'product' })
    expect(mockPost).toHaveBeenCalledWith('/log/audit/search', {
      page: 0,
      rows: 20,
      username: 'admin',
      entity: 'product',
    })
  })

  it('normalizes wrapped operation, system and audit log pages', async () => {
    mockPost
      .mockResolvedValueOnce({ data: { records: [{ id: 1, username: 'admin', requestUrl: '/api/test', responseTime: '88', status: '1' }], totalElements: 1 } })
      .mockResolvedValueOnce({ items: [{ id: 2, module: 'system', type: 'warn', message: '缓存告警', status: '0' }], totalCount: 1 })
      .mockResolvedValueOnce({ rows: [{ id: 3, username: 'auditor', targetType: 'config', targetId: '9', beforeValue: '{}', afterValue: '{"ok":true}' }], totalRecords: 1 })

    const ops = await logApi.list({ page: 0, rows: 20 })
    const system = await logApi.systemList({ page: 0, rows: 20 })
    const audit = await logApi.auditLogPage({ page: 0, rows: 20 })

    expect(ops).toMatchObject({ total: 1, list: [expect.objectContaining({ requestUri: '/api/test', durationMs: 88 })] })
    expect(system.list[0]).toMatchObject({ eventType: 'warn', summary: '缓存告警', status: 0 })
    expect(audit.list[0]).toMatchObject({ entity: 'config', entityId: 9, newValue: '{"ok":true}' })
  })
})
