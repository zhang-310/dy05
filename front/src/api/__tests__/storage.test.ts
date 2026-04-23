import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { storageApi } from '../storage'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
  },
}))

describe('storage API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('configured posts empty payload', async () => {
    mockPost.mockResolvedValue({ configured: true, provider: 'bos' })
    await storageApi.configured()
    expect(mockPost).toHaveBeenCalledWith('/storage/configured', {})
  })

  it('list posts file search payload', async () => {
    mockPost.mockResolvedValue({ total: 0, list: [] })
    await storageApi.list({ page: 0, rows: 20, fileName: 'banner' })
    expect(mockPost).toHaveBeenCalledWith('/storage/list', {
      page: 0,
      rows: 20,
      fileName: 'banner',
    })
  })

  it('uploadInit posts multipart init payload', async () => {
    mockPost.mockResolvedValue({ uploadId: 'u1' })
    await storageApi.uploadInit({ filename: 'demo.png', fileSize: 1024, fileMd5: 'abc123' })
    expect(mockPost).toHaveBeenCalledWith('/storage/upload/init', {
      filename: 'demo.png',
      fileSize: 1024,
      fileMd5: 'abc123',
    })
  })

  it('uploadComplete posts upload id payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await storageApi.uploadComplete('upload-1')
    expect(mockPost).toHaveBeenCalledWith('/storage/upload/complete', { uploadId: 'upload-1' })
  })
})
