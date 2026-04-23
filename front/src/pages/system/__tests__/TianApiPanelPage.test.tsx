import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import TianApiPanelPage from '../TianApiPanelPage'
import { tianapi } from '@/api/tianapi'

vi.mock('@/api/tianapi', () => ({
  tianapi: {
    status: vi.fn(),
    hotDouyin: vi.fn(),
    hotToutiao: vi.fn(),
    hotWeibo: vi.fn(),
    hotNetwork: vi.fn(),
  },
}))

describe('TianApiPanelPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(tianapi.status).mockResolvedValue({ status: 'ok', remainingQuota: 888, todayUsed: 112 } as never)
    vi.mocked(tianapi.hotDouyin).mockResolvedValue([{ word: '护肤热榜', hotIndex: 100000 }] as never)
    vi.mocked(tianapi.hotToutiao).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotWeibo).mockResolvedValue([] as never)
    vi.mocked(tianapi.hotNetwork).mockResolvedValue([] as never)
  })

  it('loads tianapi status and renders hot list panel', async () => {
    renderWithProviders(
      <MemoryRouter>
        <TianApiPanelPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('天API 数据面板')).toBeInTheDocument()

    await waitFor(() => {
      expect(tianapi.status).toHaveBeenCalled()
      expect(tianapi.hotDouyin).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('抖音热榜')).toBeInTheDocument()
      expect(screen.getByText('护肤热榜')).toBeInTheDocument()
      expect(screen.getByText('888')).toBeInTheDocument()
    })
  })
})
