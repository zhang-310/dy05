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
    mockPost.mockResolvedValue({ data: { records: [{ id: 1, configKey: 'APP_TOKEN_SECRET' }], totalElements: 3 } })
    const res = await configApi.list({ page: 0, rows: 20, configKey: 'APP_TOKEN_SECRET' })
    expect(mockPost).toHaveBeenCalledWith('/config/list', {
      page: 0,
      rows: 20,
      configKey: 'APP_TOKEN_SECRET',
    })
    expect(res.total).toBe(3)
    expect(res.list[0]).toEqual(expect.objectContaining({ configKey: 'APP_TOKEN_SECRET' }))
  })

  it('get posts config key payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await configApi.get('feature.enabled')
    expect(mockPost).toHaveBeenCalledWith('/config/get', { key: 'feature.enabled' })
  })

  it('save posts config save payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await configApi.save({ configKey: 'feature.enabled', configValue: 'true', configName: '功能开关', configType: 'feature', valueType: 'boolean', isSensitive: 0 })
    expect(mockPost).toHaveBeenCalledWith('/config/save', {
      configKey: 'feature.enabled',
      configValue: 'true',
      configName: '功能开关',
      configType: 'feature',
      valueType: 'boolean',
      isSensitive: 0,
    })
  })
})
