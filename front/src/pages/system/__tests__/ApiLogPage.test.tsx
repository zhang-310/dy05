import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import ApiLogPage from '../ApiLogPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    apiLogList: vi.fn(),
    apiLogStats: vi.fn(),
  },
}))

describe('ApiLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.apiLogList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          apiPath: '/api/v1/auth/login',
          method: 'POST',
          statusCode: 200,
          responseTime: 120,
          ip: '127.0.0.1',
          errorMsg: null,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(systemApi.apiLogStats).mockResolvedValue({
      totalCalls: 100,
      successCalls: 98,
      errorCalls: 2,
      avgResponseTime: 180,
      p99ResponseTime: 650,
    } as never)
  })

  it('loads api logs and renders stats cards', async () => {
    renderWithProviders(
      <MemoryRouter>
        <ApiLogPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(systemApi.apiLogList).toHaveBeenCalledWith({ page: 0, rows: 20 })
      expect(systemApi.apiLogStats).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('总调用量')).toBeInTheDocument()
      expect(screen.getByText('/api/v1/auth/login')).toBeInTheDocument()
    })
  })
})
