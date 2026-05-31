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
  const originalFilename = params.originalFilename ?? params.fileName ?? ''
  return request.post<UploadInitResult>('/storage/upload/init', {
    originalFilename,
    fileSize: params.fileSize,
    fileMd5: params.fileMd5,
    storageKey: params.storageKey ?? originalFilename,
    module: params.module,
  })
}

export function uploadChunk(uploadId: string, chunkIndex: number, chunk: Blob, chunkMd5?: string): Promise<void> {
  const formData = new FormData()
  formData.append('uploadId', uploadId)
  formData.append('chunkIndex', String(chunkIndex))
  formData.append('chunkMd5', chunkMd5 ?? '')
  formData.append('chunk', chunk)
  return request.post<void>('/storage/upload/chunk', formData)
}

export function getUploadedChunks(uploadId: string): Promise<number[]> {
  return request.get<number[]>('/storage/upload/chunks', { params: { uploadId } })
}

export function completeUpload(uploadId: string, _fileKey?: string): Promise<string> {
  return request.post<string>('/storage/upload/complete', { uploadId })
}

export function cancelUpload(uploadId: string): Promise<void> {
  return request.post<void>('/storage/upload/cancel', undefined, { params: { uploadId } })
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
