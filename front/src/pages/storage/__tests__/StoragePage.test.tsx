import { describe, it, expect, vi, beforeEach } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { fireEvent, renderWithProviders, screen, waitFor } from '@/test/utils'
import StoragePage from '../StoragePage'
import { storageApi } from '@/api/storage'

vi.mock('@/api/storage', () => ({
  storageApi: {
    configured: vi.fn(),
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
    vi.mocked(storageApi.configured).mockResolvedValue(true as never)
    vi.mocked(storageApi.list).mockResolvedValue([
      {
        key: '5001/banner.png',
        size: 2048,
        lastModified: '2026-04-10 10:00:00',
        url: 'https://example.com/banner.png',
        directory: false,
      },
    ] as never)
  })

  it('loads file list and renders file row', async () => {
    renderWithProviders(
      <MemoryRouter>
        <StoragePage />
      </MemoryRouter>,
    )

    await waitFor(() => {
      expect(storageApi.configured).toHaveBeenCalledWith()
      expect(storageApi.list).toHaveBeenCalledWith({})
    })

    const root = screen.getByTestId('storage-workbench')
    expect(root).toHaveAttribute('data-contract-scope', 'asset-storage-bos-browser')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/storage/configured')
    expect(root.getAttribute('data-ready-endpoints')).toContain('/storage/upload')
    expect(root).toHaveAttribute('data-no-local-file-fallback', 'true')
    expect(root).toHaveAttribute('data-no-client-prefix-bypass', 'true')
    expect(screen.getByTestId('storage-config-contract')).toHaveAttribute('data-prefix-mode', 'server-user-prefix')
    expect(screen.getByTestId('storage-search-contract')).toHaveAttribute('data-contract-source', '/storage/list')
    expect(screen.getByTestId('storage-upload-contract')).toHaveAttribute('data-contract-source', '/storage/upload')

    await waitFor(() => {
      expect(screen.getByText(/BOS 配置状态：已配置/)).toBeInTheDocument()
      expect(screen.getByText('5001/banner.png')).toBeInTheDocument()
      expect(screen.getByText('banner.png')).toBeInTheDocument()
    })
  })

  it('tolerates wrapped storage list mocks', async () => {
    vi.mocked(storageApi.list).mockResolvedValue({
      data: {
        files: [
          {
            key: '5001/wrapped.mp4',
            size: 4096,
            lastModified: '2026-04-11 10:00:00',
            url: 'https://example.com/wrapped.mp4',
            directory: false,
          },
        ],
      },
    } as never)

    renderWithProviders(
      <MemoryRouter>
        <StoragePage />
      </MemoryRouter>,
    )

    expect(await screen.findByText('5001/wrapped.mp4')).toBeInTheDocument()
    expect(screen.getByText('wrapped.mp4')).toBeInTheDocument()
  })

  it('shows upload endpoint error without adding a local file row', async () => {
    vi.mocked(storageApi.upload).mockRejectedValue(new Error('upload down') as never)

    renderWithProviders(
      <MemoryRouter>
        <StoragePage />
      </MemoryRouter>,
    )

    await screen.findByText('5001/banner.png')

    const fileInput = document.querySelector('input[type="file"]') as HTMLInputElement
    fireEvent.change(fileInput, {
      target: { files: [new File(['new'], 'new-video.mp4', { type: 'video/mp4' })] },
    })

    expect(await screen.findByText(/\/storage\/upload 上传失败：upload down/)).toBeInTheDocument()
    expect(screen.getByTestId('storage-upload-error')).toHaveAttribute('data-no-local-file-fallback', 'true')
    expect(screen.queryByText('new-video.mp4')).not.toBeInTheDocument()
  })

  it('keeps delete confirm context and file row when delete fails', async () => {
    vi.mocked(storageApi.delete).mockRejectedValue(new Error('delete down') as never)

    renderWithProviders(
      <MemoryRouter>
        <StoragePage />
      </MemoryRouter>,
    )

    await screen.findByText('5001/banner.png')
    fireEvent.click(screen.getByRole('button', { name: '删除' }))
    fireEvent.click(screen.getByRole('button', { name: '确认' }))

    expect(await screen.findAllByText(/\/storage\/delete 删除失败：delete down/)).toHaveLength(2)
    expect(screen.getByTestId('storage-delete-error')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getByTestId('storage-delete-contract')).toHaveAttribute('data-row-retained', 'true')
    expect(screen.getAllByText(/5001\/banner.png/).length).toBeGreaterThan(0)
  })

  it('keeps list errors explicit without local file fallback', async () => {
    vi.mocked(storageApi.list).mockRejectedValue(new Error('list down') as never)

    renderWithProviders(
      <MemoryRouter>
        <StoragePage />
      </MemoryRouter>,
    )

    expect(await screen.findByTestId('storage-list-error')).toHaveAttribute('data-no-local-file-fallback', 'true')
    expect(screen.queryByText('5001/banner.png')).not.toBeInTheDocument()
  })
})
