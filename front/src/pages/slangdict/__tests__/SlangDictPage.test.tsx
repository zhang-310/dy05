import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import SlangDictPage from '../SlangDictPage'
import { slangApi } from '@/api/slangdict'

vi.mock('@/api/slangdict', () => ({
  slangApi: {
    list: vi.fn(),
    save: vi.fn(),
    delete: vi.fn(),
    enable: vi.fn(),
    disable: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('SlangDictPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(slangApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          term: '破圈',
          definition: '突破原有受众圈层',
          example: '这条视频有机会破圈',
          industry: '内容',
          enabled: true,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads slang entries and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <SlangDictPage />
      </MemoryRouter>,
    )

    expect(screen.getByText('行业俚语词典')).toBeInTheDocument()

    await waitFor(() => {
      expect(slangApi.list).toHaveBeenCalledWith({ page: 0, rows: 20 })
    })

    await waitFor(() => {
      expect(screen.getByText('破圈')).toBeInTheDocument()
      expect(screen.getByText('突破原有受众圈层')).toBeInTheDocument()
    })
  })
})
