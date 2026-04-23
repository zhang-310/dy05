import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import SyncLogPage from '../SyncLogPage'
import { systemApi } from '@/api/system'

vi.mock('@/api/system', () => ({
  systemApi: {
    syncLogList: vi.fn(),
  },
}))

describe('SyncLogPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(systemApi.syncLogList).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          syncType: 'douyin-video',
          status: 'SUCCESS',
          totalCount: 100,
          successCount: 98,
          failCount: 2,
          errorMessage: null,
          startTime: '2026-04-10 09:00:00',
          endTime: '2026-04-10 09:05:00',
          createTime: '2026-04-10 09:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads sync logs and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SyncLogPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('同步日志')).toBeInTheDocument()

    await waitFor(() => {
      expect(systemApi.syncLogList).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('douyin-video')).toBeInTheDocument()
      expect(screen.getAllByText('成功').length).toBeGreaterThan(0)
    })
  })
})
