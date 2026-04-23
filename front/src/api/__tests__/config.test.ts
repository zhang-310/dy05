import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { configApi } from '../config'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('config API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('list posts config query payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await configApi.list({ page: 0, rows: 20, configKey: 'APP_TOKEN_SECRET' })
    expect(mockPost).toHaveBeenCalledWith('/config/list', {
      page: 0,
      rows: 20,
      configKey: 'APP_TOKEN_SECRET',
    })
  })

  it('get posts config id payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await configApi.get(1)
    expect(mockPost).toHaveBeenCalledWith('/config/get', { id: 1 })
  })

  it('save posts config save payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await configApi.save({ configKey: 'feature.enabled', configValue: 'true' })
    expect(mockPost).toHaveBeenCalledWith('/config/save', {
      configKey: 'feature.enabled',
      configValue: 'true',
    })
  })
})
