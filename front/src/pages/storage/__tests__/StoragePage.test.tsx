import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { renderWithProviders, screen, waitFor } from '@/test/utils'
import StoragePage from '../StoragePage'
import { storageApi } from '@/api/storage'

vi.mock('@/api/storage', () => ({
  storageApi: {
    list: vi.fn(),
    upload: vi.fn(),
    delete: vi.fn(),
  },
}))

vi.mock('@/contexts/ToastContext', () => ({
  useToast: () => vi.fn(),
}))

describe('StoragePage', () => {
  beforeEach(() => {
    vi.clearAllMocks()
    vi.mocked(storageApi.list).mockResolvedValue({
      total: 1,
      list: [
        {
          id: 1,
          fileName: 'banner.png',
          fileType: 'image',
          fileSize: 2048,
          fileUrl: 'https://example.com/banner.png',
          createTime: '2026-04-10 10:00:00',
        },
      ],
      pageNum: 0,
      pageSize: 20,
    } as never)
  })

  it('loads file list and renders file row', async () => {
    renderWithProviders(
      <MemoryRouter>
        <StoragePage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(storageApi.list).toHaveBeenCalled()
    })

    await waitFor(() => {
      expect(screen.getByText('banner.png')).toBeInTheDocument()
      expect(screen.getByText('image')).toBeInTheDocument()
    })
  })
})
