import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import {
  deleteExternalApiConfig,
  getExternalApiConfig,
  getExternalApisByCategory,
  saveExternalApiConfig,
  searchExternalApiConfigs,
  updateHealthStatus,
} from '../external-api'

vi.mock('@/utils/request', () => ({
  default: { post: vi.fn() },
}))

describe('external-api API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('searchExternalApiConfigs posts search payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 10 })

    await searchExternalApiConfigs({ page: 0, rows: 10, category: 'ai', keyword: 'deepseek' })

    expect(mockPost).toHaveBeenCalledWith('/system/external-api/list', {
      page: 0,
      rows: 10,
      category: 'ai',
      keyword: 'deepseek',
    })
  })

  it('getExternalApiConfig posts providerCode', async () => {
    mockPost.mockResolvedValue({ providerCode: 'deepseek' })

    await getExternalApiConfig({ providerCode: 'deepseek' })

    expect(mockPost).toHaveBeenCalledWith('/system/external-api/get', {
      providerCode: 'deepseek',
    })
  })

  it('saveExternalApiConfig posts save payload', async () => {
    mockPost.mockResolvedValue({ id: 1, providerCode: 'deepseek', providerName: 'DeepSeek', isEnabled: true, priority: 0 })

    await saveExternalApiConfig({
      providerCode: 'deepseek',
      providerName: 'DeepSeek',
      category: 'ai',
      baseUrl: 'https://api.example.com',
      apiKey: 'key',
      apiSecret: 'secret',
      isEnabled: true,
      priority: 1,
    })

    expect(mockPost).toHaveBeenCalledWith('/system/external-api/save', {
      providerCode: 'deepseek',
      providerName: 'DeepSeek',
      category: 'ai',
      baseUrl: 'https://api.example.com',
      apiKey: 'key',
      apiSecret: 'secret',
      isEnabled: true,
      priority: 1,
    })
  })

  it('deleteExternalApiConfig posts id payload', async () => {
    mockPost.mockResolvedValue(undefined)

    await deleteExternalApiConfig({ id: 9 })

    expect(mockPost).toHaveBeenCalledWith('/system/external-api/delete', { id: 9 })
  })

  it('getExternalApisByCategory posts category payload', async () => {
    mockPost.mockResolvedValue([])

    await getExternalApisByCategory({ category: 'media' })

    expect(mockPost).toHaveBeenCalledWith('/system/external-api/by-category', {
      category: 'media',
    })
  })

  it('updateHealthStatus posts health payload', async () => {
    mockPost.mockResolvedValue(undefined)

    await updateHealthStatus({
      providerCode: 'deepseek',
      status: 'healthy',
      latencyMs: 180,
      successRate: 99.5,
    })

    expect(mockPost).toHaveBeenCalledWith('/system/external-api/health-status', {
      providerCode: 'deepseek',
      status: 'healthy',
      latencyMs: 180,
      successRate: 99.5,
    })
  })
})
