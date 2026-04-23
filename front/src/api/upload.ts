import request from '@/utils/request'

export interface UploadInitRequest {
  originalFilename?: string
  fileName?: string
  fileSize: number
  fileMd5: string
  chunkSize?: number
  totalChunks?: number
  storageKey?: string
  module?: string
  contentType?: string
}

export interface UploadInitResult {
  uploadId: string
  fileKey?: string
  uploadedChunks?: number[]
  totalChunks?: number
  isSecondUpload?: boolean
  secondUploadUrl?: string
}

export function initUpload(params: UploadInitRequest): Promise<UploadInitResult> {
  return request.post<UploadInitResult>('/storage/chunk/init', params)
}

export function uploadChunk(uploadId: string, chunkIndex: number, chunk: Blob, chunkMd5?: string): Promise<void> {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('chunk', chunk)
  if (chunkMd5) formData.append('chunkMd5', chunkMd5)
  return request.post<void>('/storage/chunk/upload', formData)
}

export function getUploadedChunks(uploadId: string): Promise<number[]> {
  return request.post<number[]>('/storage/chunk/uploaded', { uploadId })
}

export function completeUpload(uploadId: string, fileKey: string): Promise<string> {
  return request.post<string>('/storage/chunk/complete', { uploadId, fileKey })
}

export function cancelUpload(uploadId: string): Promise<void> {
  return request.post<void>('/storage/chunk/cancel', { uploadId })
}

export function uploadFile(file: File): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/storage/upload', formData)
}

export function uploadFileWithProgress(
  file: File,
  onProgress?: (percent: number) => void
): Promise<string> {
  const formData = new FormData()
  formData.append('file', file)
  return request.post<string>('/storage/upload', formData, {
    onUploadProgress: (e: { loaded: number; total?: number }) => {
      if (onProgress && e.total) {
        onProgress(Math.round((e.loaded * 100) / e.total))
      }
    },
  } as Record<string, unknown>)
}
