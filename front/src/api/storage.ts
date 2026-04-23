import request from '@/utils/request'
import type { PageResult } from '@/types/common'

export interface SysFile {
  id: number
  ownerId: number
  originalName: string
  storageName: string
  storagePath: string
  fileUrl: string
  fileType: string
  fileExt: string
  fileSize: number
  module: string
  provider: string
  createTime: string
}
export interface StorageQuery { page?: number; rows?: number; originalName?: string; fileName?: string; fileType?: string; module?: string }

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

export const storageApi = {
  configured: () => request.post<{ configured: boolean; provider: string }>('/storage/configured', {}),
  list: (params: StorageQuery) => request.post<PageResult<SysFile>>('/storage/list', params),
  upload: (file: File, module?: string) => {
    const form = new FormData()
    form.append('file', file)
    if (module) form.append('module', module)
    return request.post<SysFile>('/storage/upload', form)
  },
  delete: (id: number) => request.post<void>('/storage/delete', { id }),
  url: (id: number) => request.post<{ url: string }>('/storage/url', { id }),

  // 分片上传
  uploadInit: (params: { filename: string; fileSize: number; fileMd5: string; chunkSize?: number; module?: string }) =>
    request.post<UploadTask>('/storage/upload/init', params),
  uploadChunk: (uploadId: string, chunkIndex: number, chunkData: Blob) => {
    const form = new FormData()
    form.append('uploadId', uploadId)
    form.append('chunkIndex', String(chunkIndex))
    form.append('chunk', chunkData)
    return request.post<void>('/storage/upload/chunk', form)
  },
  uploadComplete: (uploadId: string) => request.post<SysFile>('/storage/upload/complete', { uploadId }),
  uploadCancel: (uploadId: string) => request.post<void>('/storage/upload/cancel', { uploadId }),
}
