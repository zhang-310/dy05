import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { aiApi, getModelsByTaskCode, listAiModels } from '../ai'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    delete: vi.fn(),
  },
}))

describe('ai API', () => {
  const mockPost = vi.mocked(request.default.post)
  const mockDelete = vi.mocked(request.default.delete)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('kbList posts knowledge base query', async () => {
    mockPost.mockResolvedValue([])
    await aiApi.kbList({ page: 0, rows: 20, name: '护肤' })
    expect(mockPost).toHaveBeenCalledWith('/ai/knowledge-base/list', { page: 0, rows: 20, name: '护肤' })
  })

  it('kbDelete uses delete with id path', async () => {
    mockDelete.mockResolvedValue(undefined)
    await aiApi.kbDelete(12)
    expect(mockDelete).toHaveBeenCalledWith('/ai/knowledge-base/12')
  })

  it('getModelsByTaskCode posts task code payload', async () => {
    mockPost.mockResolvedValue([])
    await getModelsByTaskCode('live_script')
    expect(mockPost).toHaveBeenCalledWith('/ai/model/list-by-task', { taskCode: 'live_script' })
  })

  it('listAiModels requests fixed page size', async () => {
    mockPost.mockResolvedValue([])
    await listAiModels(2)
    expect(mockPost).toHaveBeenCalledWith('/ai/model/list', { page: 2, rows: 100 })
  })

  it('evolveTaskTrigger posts evolution task request', async () => {
    mockPost.mockResolvedValue({ taskId: 'abc12def' })
    await aiApi.evolveTaskTrigger({ taskType: 'quality', targetId: 88, priority: 5 })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution/task/trigger', {
      taskType: 'quality',
      targetId: 88,
      priority: 5,
    })
  })

  it('evolutionReviewList posts evolution-review path', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [], pageNum: 0, pageSize: 20 })
    await aiApi.evolutionReviewList({ page: 0, rows: 20, status: 'PENDING' })
    expect(mockPost).toHaveBeenCalledWith('/ai/evolution-review/list', { page: 0, rows: 20, status: 'PENDING' })
  })

  it('evolveTaskReport posts admin task report request', async () => {
    mockPost.mockResolvedValue({ id: 9 })
    await aiApi.evolveTaskReport(9)
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/evolve/report/by-task', { taskId: 9 })
  })

  it('infraHealth posts empty object payload', async () => {
    mockPost.mockResolvedValue({ status: 'ok' })
    await aiApi.infraHealth()
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/infra/health', {})
  })

  it('quotaUpdate posts admin quota update payload', async () => {
    mockPost.mockResolvedValue(undefined)
    await aiApi.quotaUpdate({ userId: 7, dailyMax: 500 })
    expect(mockPost).toHaveBeenCalledWith('/ai/admin/quota/update', { userId: 7, dailyMax: 500 })
  })

  it('modelBenchmarkBestModel posts task code request', async () => {
    mockPost.mockResolvedValue({ modelId: 3 })
    await aiApi.modelBenchmarkBestModel('copy_generate')
    expect(mockPost).toHaveBeenCalledWith('/ai/model-benchmark/best-model', {
      taskCode: 'copy_generate',
      priority: 'latency',
    })
  })
})
