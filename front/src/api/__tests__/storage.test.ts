import { describe, it, expect, vi, beforeEach } from 'vitest'
import * as request from '@/utils/request'
import { storageApi } from '../storage'

vi.mock('@/utils/request', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
  },
}))

describe('storage API', () => {
  const mockPost = vi.mocked(request.default.post)

  beforeEach(() => {
    vi.clearAllMocks()
  })

  it('configured posts empty payload and returns backend boolean', async () => {
    mockPost.mockResolvedValue(true)
    await expect(storageApi.configured()).resolves.toBe(true)
    expect(mockPost).toHaveBeenCalledWith('/storage/configured', {})
  })

  it('list posts file search payload', async () => {
    mockPost.mockResolvedValue({ data: { files: [{ key: '5001/banner.png' }] } })
    const res = await storageApi.list({ prefixSuffix: 'banner' })
    expect(mockPost).toHaveBeenCalledWith('/storage/list', {
      prefixSuffix: 'banner',
    })
    expect(res).toEqual([{ key: '5001/banner.png' }])
  })

  it('list sends empty prefixSuffix when no filter is provided', async () => {
    mockPost.mockResolvedValue([])

    await storageApi.list()

    expect(mockPost).toHaveBeenCalledWith('/storage/list', {
      prefixSuffix: '',
    })
  })

  it('uploadInit posts multipart init payload', async () => {
    mockPost.mockResolvedValue({ uploadId: 'u1' })
    await storageApi.uploadInit({ filename: 'demo.png', fileSize: 1024, fileMd5: 'abc123' })
    expect(mockPost).toHaveBeenCalledWith('/storage/upload/init', {
      originalFilename: 'demo.png',
      fileSize: 1024,
      fileMd5: 'abc123',
      storageKey: 'demo.png',
      module: undefined,
    })
  })

  it('uploadChunk posts backend chunkMd5 multipart field', async () => {
    mockPost.mockResolvedValue(undefined)
    await storageApi.uploadChunk('upload-1', 2, new Blob(['part']), 'chunk-md5')
    expect(mockPost).toHaveBeenCalledWith('/storage/upload/chunk', expect.any(FormData))
    const form = mockPost.mock.calls[0][1] as FormData
    expect(form.get('uploadId')).toBe('upload-1')
    expect(form.get('chunkIndex')).toBe('2')
    expect(form.get('chunkMd5')).toBe('chunk-md5')
  })

  it('uploadComplete posts upload id payload', async () => {
    mockPost.mockResolvedValue({ id: 1 })
    await storageApi.uploadComplete('upload-1')
    expect(mockPost).toHaveBeenCalledWith('/storage/upload/complete', { uploadId: 'upload-1' })
  })

  it('uploadCancel posts upload id as request param', async () => {
    mockPost.mockResolvedValue(undefined)
    await storageApi.uploadCancel('upload-1')
    expect(mockPost).toHaveBeenCalledWith('/storage/upload/cancel', undefined, { params: { uploadId: 'upload-1' } })
  })
})
