/**
 * 分块上传相关类型定义
 */

/**
 * 上传状态枚举
 */
export enum UploadStatusEnum {
  PENDING = 'PENDING',
  UPLOADING = 'UPLOADING',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
  FAILED = 'FAILED',
}

/**
 * 上传任务信息
 */
export interface UploadTask {
  uploadId: string;
  filename: string;
  fileSize: number;
  totalChunks: number;
  uploadedChunks: number;
  uploadedBytes: number;
  progressPercent: number;
  status: UploadStatusEnum;
  error?: string;
}

/**
 * 分块信息
 */
export interface ChunkInfo {
  index: number;
  size: number;
  md5: string;
  file?: File;
  uploaded: boolean;
  uploading: boolean;
  error?: string;
}

/**
 * 文件信息
 */
export interface FileInfo {
  file: File;
  name: string;
  size: number;
  md5: string;
  chunks: ChunkInfo[];
}

/**
 * 上传统计
 */
export interface UploadStats {
  totalSize: number;
  uploadedSize: number;
  totalChunks: number;
  uploadedChunks: number;
  failedChunks: number;
  progressPercent: number;
  uploadSpeed: number; // bytes per second
  remainingTime: number; // seconds
}
