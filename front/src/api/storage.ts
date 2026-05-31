import request from '@/utils/request'
import { normalizeArray } from '@/utils/response-normalize'

export interface StorageFile {
  key: string
  size?: number | null
  lastModified?: string | null
  url?: string | null
  directory?: boolean
}
export type SysFile = StorageFile

export interface StorageQuery {
  prefixSuffix?: string
  originalName?: string
  fileName?: string
  fileType?: string
  module?: string
}

export interface UploadTask {
  id: number
  uploadId: string
  originalFilename: string
  fileSize: number
  totalChunks: number
  uploadedChunks: number
  status: string
  createdAt: string
}

export interface UploadInitParams {
  originalFilename?: string
  filename?: string
  fileSize: number
  fileMd5: string
  storageKey?: string
  module?: string
}

export const storageApi = {
  configured: () => request.post<boolean>('/storage/configured', {}),
  list: (params: StorageQuery = {}) =>
    request.post<unknown>('/storage/list', {
      prefixSuffix: params.prefixSuffix ?? params.originalName ?? params.fileName ?? '',
    }).then(normalizeArray<StorageFile>),
  upload: (file: File, prefix?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (prefix) form.append('prefix', prefix)
    return request.post<{ key: string; url: string }>('/storage/upload', form)
  },
  delete: (key: string) => request.post<void>('/storage/delete', { key }),
  url: (key: string) => request.post<{ url: string }>('/storage/url', { key }),

  // 分片上传
  uploadInit: (params: UploadInitParams) => {
    const originalFilename = params.originalFilename ?? params.filename ?? ''
    return request.post<UploadTask>('/storage/upload/init', {
      originalFilename,
      fileSize: params.fileSize,
      fileMd5: params.fileMd5,
      storageKey: params.storageKey ?? originalFilename,
      module: params.module,
    })
  },
  uploadChunk: (uploadId: string, chunkIndex: number, chunkData: Blob, chunkMd5?: string) => {
    const form = new FormData()
    form.append('uploadId', uploadId)
    form.append('chunkIndex', String(chunkIndex))
    form.append('chunkMd5', chunkMd5 ?? '')
    form.append('chunk', chunkData)
    return request.post<void>('/storage/upload/chunk', form)
  },
  uploadComplete: (uploadId: string) => request.post<StorageFile>('/storage/upload/complete', { uploadId }),
  uploadCancel: (uploadId: string) => request.post<void>('/storage/upload/cancel', undefined, { params: { uploadId } }),
}
