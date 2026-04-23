import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import WecomPage from '../WecomPage'
import { wecomApi } from '@/api/wecom'

vi.mock('@/api/wecom', () => ({
  wecomApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    updateStatus: vi.fn(),
    push: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('WecomPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(wecomApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          robotName: '告警机器人',
          webhookUrl: 'https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=abc',
          status: 1,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('renders title and loads robot list on default tab', async () => {
    renderWithProviders(
      <MemoryRouter>
        <WecomPage />
      </MemoryRouter>,
    )

    expect(screen.getByRole('heading', { name: '企微推送' })).toBeInTheDocument()

    await waitFor(() => {
      expect(wecomApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('告警机器人')).toBeInTheDocument()
    })
  })
})
