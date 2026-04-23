import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import CopyLibraryPage from '../CopyLibraryPage'
import { copyApi } from '@/api/copy'

vi.mock('@/api/copy', () => ({
  copyApi: {
    list: vi.fn(),
    semanticSearch: vi.fn(),
    usageList: vi.fn(),
    delete: vi.fn(),
    batchDelete: vi.fn(),
    batchSubmitApproval: vi.fn(),
    batchTag: vi.fn(),
    aiGenerate: vi.fn(),
    save: vi.fn(),
    exportCsv: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('CopyLibraryPage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(copyApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          title: '护肤直播开场',
          content: '大家好，欢迎来到直播间，今天给大家带来修护精华',
          category: '护肤',
          tags: '开场话术,商品介绍',
          useCount: 12,
          rating: 8.8,
          status: 2,
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
    vi.mocked(copyApi.usageList).mockResolvedValue([] as never)
  })

  it('loads copy library list and renders row content', async () => {
    renderWithProviders(
      <MemoryRouter>
        <CopyLibraryPage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(copyApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText(/大家好，欢迎来到直播间/)).toBeInTheDocument()
      expect(screen.getAllByText('开场话术').length).toBeGreaterThan(0)
      expect(screen.getByText('已通过')).toBeInTheDocument()
    })
  })
})
