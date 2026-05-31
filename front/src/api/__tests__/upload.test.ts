import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { cancelUpload, completeUpload, getUploadedChunks, initUpload, uploadChunk } from '../upload'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('upload API', () => {
  const mockPost = vi.mocked(request.default.post)
  const mockGet = vi.mocked(request.default.get)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('initUpload posts real UploadInitVO payload', async () => {
    mockPost.mockResolvedValue({ uploadId: 'upload-1' })

    await initUpload({
      fileName: 'demo.mp4',
      fileSize: 4096,
      fileMd5: 'file-md5',
      storageKey: 'uploads/demo.mp4',
      module: 'shortvideo',
    })

    expect(mockPost).toHaveBeenCalledWith('/storage/upload/init', {
      originalFilename: 'demo.mp4',
      fileSize: 4096,
      fileMd5: 'file-md5',
      storageKey: 'uploads/demo.mp4',
      module: 'shortvideo',
    })
  })

  it('uploadChunk posts real UploadController multipart payload', async () => {
    mockPost.mockResolvedValue(undefined)

    await uploadChunk('upload-1', 3, new Blob(['chunk']), 'chunk-md5')

    expect(mockPost).toHaveBeenCalledWith('/storage/upload/chunk', expect.any(FormData))
    const form = mockPost.mock.calls[0][1] as FormData
    expect(form.get('uploadId')).toBe('upload-1')
    expect(form.get('chunkIndex')).toBe('3')
    expect(form.get('chunkMd5')).toBe('chunk-md5')
  })

  it('queries uploaded chunks through backend GET endpoint', async () => {
    mockGet.mockResolvedValue([0, 2])

    await expect(getUploadedChunks('upload-1')).resolves.toEqual([0, 2])

    expect(mockGet).toHaveBeenCalledWith('/storage/upload/chunks', { params: { uploadId: 'upload-1' } })
  })

  it('complete and cancel use real upload endpoints', async () => {
    mockPost.mockResolvedValue(undefined)

    await completeUpload('upload-1', 'ignored-key')
    await cancelUpload('upload-1')

    expect(mockPost).toHaveBeenNthCalledWith(1, '/storage/upload/complete', { uploadId: 'upload-1' })
    expect(mockPost).toHaveBeenNthCalledWith(2, '/storage/upload/cancel', undefined, { params: { uploadId: 'upload-1' } })
  })
})
