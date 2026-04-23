import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import RobotsPage from '../RobotsPage'
import { wecomApi } from '@/api/wecom'

vi.mock('@/api/wecom', () => ({
  wecomApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    test: vi.fn(),
    push: vi.fn(),
    logList: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('RobotsPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(wecomApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          robotName: '日报机器人',
          webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads robots and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <RobotsPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(wecomApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('日报机器人')).toBeInTheDocument()
      expect(screen.getByText('新增机器人')).toBeInTheDocument()
    })
  })
})
