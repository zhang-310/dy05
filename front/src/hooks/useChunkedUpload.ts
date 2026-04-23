/**
 * 分块上传 Hook
 * 管理文件上传生命周期：初始化 → 查询恢复 → 并发上传 → 完成
 */

import { useState, useCallback, useRef } from 'react';
import {
  initUpload,
  uploadChunk,
  getUploadedChunks,
  completeUpload,
  cancelUpload,
  UploadInitRequest,
  UploadInitResult,
} from '@/api/upload';
import { UploadStatusEnum, UploadTask, UploadStats } from '@/types/upload';
import {
  computeFileMD5,
  computeChunkMD5,
  splitFileIntoChunks,
} from '@/utils/fileUtils';

const MAX_CONCURRENT_CHUNKS = 3; // 最多同时上传 3 个分块

export interface UseChunkedUploadOptions {
  onProgress?: (stats: UploadStats) => void;
  onComplete?: (result: unknown) => void;
  onError?: (error: Error) => void;
}

export const useChunkedUpload = (options: UseChunkedUploadOptions = {}) => {
  const [task, setTask] = useState<UploadTask | null>(null);
  const [stats, setStats] = useState<UploadStats>({
    totalSize: 0,
    uploadedSize: 0,
    totalChunks: 0,
    uploadedChunks: 0,
    failedChunks: 0,
    progressPercent: 0,
    uploadSpeed: 0,
    remainingTime: 0,
  });
  const [isUploading, setIsUploading] = useState(false);

  const fileRef = useRef<File | null>(null);
  const chunksRef = useRef<Blob[]>([]);
  const uploadingRef = useRef<Set<number>>(new Set());
  const startTimeRef = useRef<number>(0);
  const abortControllerRef = useRef<AbortController | null>(null);

  /**
   * 初始化上传
   */
  const startUpload = useCallback(
    async (file: File, storageKey: string, module?: string) => {
      try {
        setIsUploading(true);
        fileRef.current = file;
        startTimeRef.current = Date.now();

        // 1. 计算文件 MD5
        const fileMd5 = await computeFileMD5(file);

        // 2. 分割文件为分块
        const chunks = splitFileIntoChunks(file);
        chunksRef.current = chunks;

        // 3. 初始化上传任务
        const initReq: UploadInitRequest = {
          originalFilename: file.name,
          fileSize: file.size,
          fileMd5,
          storageKey,
          module,
        };

        const initResult = (await initUpload(initReq)) as UploadInitResult;

        // 秒传判断
        if (initResult?.isSecondUpload) {
          setTask({
            uploadId: initResult?.uploadId,
            filename: file.name,
            fileSize: file.size,
            totalChunks: 0,
            uploadedChunks: 0,
            uploadedBytes: file.size,
            progressPercent: 100,
            status: UploadStatusEnum.COMPLETED,
          });

          setStats((prev) => ({
            ...prev,
            totalSize: file.size,
            uploadedSize: file.size,
            progressPercent: 100,
          }));

          options.onComplete?.({
            uploadId: initResult?.uploadId,
            fileUrl: initResult?.secondUploadUrl,
            fileSize: file.size,
          });

          setIsUploading(false);
          return initResult?.uploadId;
        }

        // 4. 创建任务
        const newTask: UploadTask = {
          uploadId: initResult?.uploadId,
          filename: file.name,
          fileSize: file.size,
          totalChunks: initResult?.totalChunks ?? 0,
          uploadedChunks: 0,
          uploadedBytes: 0,
          progressPercent: 0,
          status: UploadStatusEnum.UPLOADING,
        };

        setTask(newTask);
        setStats({
          totalSize: file.size,
          uploadedSize: 0,
          totalChunks: initResult?.totalChunks ?? 0,
          uploadedChunks: 0,
          failedChunks: 0,
          progressPercent: 0,
          uploadSpeed: 0,
          remainingTime: 0,
        });

        // 5. 查询已上传分块（断点续传恢复）
        const uploadedChunks = initResult?.uploadedChunks || [];

        // 6. 并发上传分块
        await uploadChunksWithConcurrency(
          initResult?.uploadId,
          chunks,
          uploadedChunks
        );

        return initResult?.uploadId;
      } catch (error) {
        const err = error instanceof Error ? error : new Error(String(error));
        setTask((prev) =>
          prev
            ? { ...prev, status: UploadStatusEnum.FAILED, error: err.message }
            : null
        );
        options.onError?.(err);
        throw err;
      } finally {
        setIsUploading(false);
      }
    },
    [options]
  );

  /**
   * 并发上传分块（最多 MAX_CONCURRENT_CHUNKS 个同时上传）
   */
  const uploadChunksWithConcurrency = useCallback(
    async (uploadId: string, chunks: Blob[], skipIndices: number[]) => {
      const skipSet = new Set(skipIndices);
      const toUpload = Array.from({ length: chunks.length }, (_, i) => i).filter(
        (i) => !skipSet.has(i)
      );

      let uploadedCount = skipIndices.length;
      let failedCount = 0;

      for (let i = 0; i < toUpload.length; i += MAX_CONCURRENT_CHUNKS) {
        const batch = toUpload.slice(i, i + MAX_CONCURRENT_CHUNKS);
        const promises = batch.map((chunkIndex) =>
          uploadSingleChunk(uploadId, chunkIndex, chunks[chunkIndex])
            .then(() => {
              uploadedCount++;
              updateProgress(uploadId, uploadedCount, failedCount, chunks.length);
            })
            .catch(() => {
              failedCount++;
              updateProgress(uploadId, uploadedCount, failedCount, chunks.length);
              // 重试逻辑可在这里添加
            })
        );

        await Promise.all(promises);
      }

      // 所有分块上传完成，触发合并
      if (failedCount === 0) {
        await finishUpload(uploadId);
      } else {
        throw new Error(`${failedCount} 个分块上传失败`);
      }
    },
    []
  );

  /**
   * 上传单个分块
   */
  const uploadSingleChunk = useCallback(
    async (uploadId: string, chunkIndex: number, chunk: Blob) => {
      uploadingRef.current.add(chunkIndex);

      try {
        const chunkMd5 = await computeChunkMD5(chunk);
        await uploadChunk(uploadId, chunkIndex, chunk, chunkMd5);
      } finally {
        uploadingRef.current.delete(chunkIndex);
      }
    },
    []
  );

  /**
   * 更新上传进度
   */
  const updateProgress = useCallback(
    (_uploadId: string, uploadedCount: number, failedCount: number, totalChunks: number) => {
      if (!fileRef.current) return;

      const chunkSize = Math.ceil(fileRef.current.size / totalChunks);
      const uploadedBytes = uploadedCount * chunkSize;
      const totalSize = fileRef.current.size;
      const progressPercent = Math.round((uploadedBytes / totalSize) * 100);

      // 计算上传速度和剩余时间
      const elapsedTime = (Date.now() - startTimeRef.current) / 1000;
      const uploadSpeed = uploadedBytes / elapsedTime;
      const remainingBytes = totalSize - uploadedBytes;
      const remainingTime = uploadSpeed > 0 ? remainingBytes / uploadSpeed : 0;

      const newStats: UploadStats = {
        totalSize,
        uploadedSize: uploadedBytes,
        totalChunks,
        uploadedChunks: uploadedCount,
        failedChunks: failedCount,
        progressPercent,
        uploadSpeed,
        remainingTime,
      };

      setStats(newStats);
      setTask((prev) =>
        prev
          ? {
              ...prev,
              uploadedChunks: uploadedCount,
              uploadedBytes,
              progressPercent,
            }
          : null
      );

      options.onProgress?.(newStats);
    },
    [options]
  );

  /**
   * 完成上传
   */
  const finishUpload = useCallback(async (uploadId: string) => {
    try {
      const result = await completeUpload(uploadId, '');

      setTask((prev) =>
        prev
          ? {
              ...prev,
              status: UploadStatusEnum.COMPLETED,
              uploadedBytes: prev.fileSize,
              progressPercent: 100,
            }
          : null
      );

      options.onComplete?.(result);
    } catch (error) {
      const err = error instanceof Error ? error : new Error(String(error));
      setTask((prev) =>
        prev
          ? { ...prev, status: UploadStatusEnum.FAILED, error: err.message }
          : null
      );
      options.onError?.(err);
      throw err;
    }
  }, [options]);

  /**
   * 暂停上传（取消所有正在进行的）
   */
  const pauseUpload = useCallback(() => {
    setIsUploading(false);
    abortControllerRef.current?.abort();
  }, []);

  /**
   * 恢复上传（继续未完成的分块）
   */
  const resumeUpload = useCallback(async () => {
    if (!task) return;

    try {
      setIsUploading(true);
      const uploadedChunks = (await getUploadedChunks(task.uploadId)) as number[];
      await uploadChunksWithConcurrency(
        task.uploadId,
        chunksRef.current,
        uploadedChunks
      );
    } catch (error) {
      const err = error instanceof Error ? error : new Error(String(error));
      options.onError?.(err);
    } finally {
      setIsUploading(false);
    }
  }, [task, uploadChunksWithConcurrency, options]);

  /**
   * 取消上传
   */
  const abortUpload = useCallback(async () => {
    if (!task) return;

    try {
      await cancelUpload(task.uploadId);
      setTask((prev) =>
        prev ? { ...prev, status: UploadStatusEnum.CANCELLED } : null
      );
    } catch (error) {
      const err = error instanceof Error ? error : new Error(String(error));
      options.onError?.(err);
    }
  }, [task, options]);

  return {
    task,
    stats,
    isUploading,
    startUpload,
    pauseUpload,
    resumeUpload,
    abortUpload,
  };
};
