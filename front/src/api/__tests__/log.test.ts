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
    await logApi.systemList({ page: 1, rows: 50, module: 'auth' })
    expect(mockPost).toHaveBeenCalledWith('/log/system/page', {
      page: 1,
      rows: 50,
      module: 'auth',
    })
  })

  it('auditLogGet posts audit id payload', async () => {
    mockPost.mockResolvedValue({ id: 3 })
    await logApi.auditLogGet(3)
    expect(mockPost).toHaveBeenCalledWith('/log/audit/get', { id: 3 })
  })
})
