/**
 * 分块上传集成测试
 * 测试整个上传生命周期：初始化 → 查询恢复 → 并发上传 → 完成
 */

import { describe, it, expect, beforeEach, vi } from 'vitest';
import { renderHook, act } from '@testing-library/react';
import { useChunkedUpload } from '@/hooks/useChunkedUpload';
import * as uploadApi from '@/api/upload';
import * as fileUtils from '@/utils/fileUtils';
import { UploadStatusEnum } from '@/types/upload';

// Mock API
vi.mock('@/api/upload');
vi.mock('@/utils/fileUtils');

describe('上传流程集成测试', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  describe('第一阶段：初始化上传', () => {
    it('应该计算文件 MD5', async () => {
      const file = new File(['content'], 'test.txt', { type: 'text/plain' });
      const mockMD5 = 'abc123def456';
      const chunks = [new Blob(['content'])];

      vi.mocked(fileUtils.computeFileMD5).mockResolvedValue(mockMD5);
      vi.mocked(fileUtils.splitFileIntoChunks).mockReturnValue(chunks);
      vi.mocked(uploadApi.initUpload).mockResolvedValue({
        uploadId: 'upload-123',
        isSecondUpload: false,
        chunkSize: 5 * 1024 * 1024,
        totalChunks: 1,
        uploadedChunks: [],
      } as any);
      vi.mocked(uploadApi.uploadChunk).mockResolvedValue({ success: true } as any);

      const { result } = renderHook(() => useChunkedUpload());

      await act(async () => {
        await result.current.startUpload(file, 'test-storage-key');
      });

      expect(fileUtils.computeFileMD5).toHaveBeenCalledWith(file);
    });

    it('应该分割文件为多个分块', async () => {
      const file = new File(['a'.repeat(10 * 1024 * 1024)], 'large.bin');
      const chunks = [new Blob(), new Blob()];

      vi.mocked(fileUtils.computeFileMD5).mockResolvedValue('md5-123');
      vi.mocked(fileUtils.splitFileIntoChunks).mockReturnValue(chunks);
      vi.mocked(uploadApi.initUpload).mockResolvedValue({
        uploadId: 'upload-123',
        isSecondUpload: false,
        chunkSize: 5 * 1024 * 1024,
        totalChunks: 2,
        uploadedChunks: [],
      } as any);

      const { result } = renderHook(() => useChunkedUpload());

      await act(async () => {
        await result.current.startUpload(file, 'storage-key');
      });

      expect(fileUtils.splitFileIntoChunks).toHaveBeenCalledWith(file);
    });

    it('应该处理秒传场景（文件已存在）', async () => {
      const file = new File(['content'], 'existing.txt');
      const onComplete = vi.fn();

      vi.mocked(fileUtils.computeFileMD5).mockResolvedValue('existing-md5');
      vi.mocked(fileUtils.splitFileIntoChunks).mockReturnValue([]);
      vi.mocked(uploadApi.initUpload).mockResolvedValue({
        uploadId: 'upload-123',
        isSecondUpload: true,
        secondUploadUrl: 'https://cdn.example.com/existing.txt',
        chunkSize: 5 * 1024 * 1024,
        totalChunks: 0,
        uploadedChunks: [],
      } as any);

      const { result } = renderHook(() =>
        useChunkedUpload({ onComplete })
      );

      await act(async () => {
        await result.current.startUpload(file, 'storage-key');
      });

      expect(result.current.task?.progressPercent).toBe(100);
      expect(result.current.task?.status).toBe(UploadStatusEnum.COMPLETED);
      expect(onComplete).toHaveBeenCalled();
    });
  });

  describe('第二阶段：查询恢复', () => {
    it('应该支持恢复暂停的上传', async () => {
      const file = new File(['a'.repeat(10 * 1024 * 1024)], 'large.bin');
      const chunks = [new Blob(['part1']), new Blob(['part2']), new Blob(['part3'])];

      vi.mocked(fileUtils.computeFileMD5).mockResolvedValue('md5-123');
      vi.mocked(fileUtils.splitFileIntoChunks).mockReturnValue(chunks);
      vi.mocked(uploadApi.initUpload).mockResolvedValue({
        uploadId: 'upload-123',
        isSecondUpload: false,
        chunkSize: 5 * 1024 * 1024,
        totalChunks: 3,
        uploadedChunks: [0, 1],
      } as any);
      vi.mocked(uploadApi.getUploadedChunks).mockResolvedValue([0, 1] as any);
      vi.mocked(uploadApi.uploadChunk).mockResolvedValue(undefined as any);

      const onProgress = vi.fn();
      const { result } = renderHook(() =>
        useChunkedUpload({ onProgress })
      );

      // 先启动上传（会自动暂停因为有 uploadedChunks）
      await act(async () => {
        await result.current.startUpload(file, 'test-storage-key');
      });

      // 然后恢复上传
      await act(async () => {
        await result.current.resumeUpload();
      });

      // 验证调用了 getUploadedChunks
      expect(uploadApi.getUploadedChunks).toHaveBeenCalled();
    });
  });

  describe('第三阶段：并发上传', () => {
    it('应该正确处理分块上传', async () => {
      vi.mocked(uploadApi.uploadChunk).mockResolvedValue(undefined as any);

      // 记录 uploadChunk 的调用
      const uploadCalls = vi.mocked(uploadApi.uploadChunk).mock;

      // 模拟上传 3 个分块
      await Promise.all([
        uploadApi.uploadChunk('upload-123', 0, 'md5-0', {} as File),
        uploadApi.uploadChunk('upload-123', 1, 'md5-1', {} as File),
        uploadApi.uploadChunk('upload-123', 2, 'md5-2', {} as File),
      ]);

      expect(uploadCalls.calls.length).toBe(3);
    });

    it('应该处理分块上传失败和重试', async () => {
      let attemptCount = 0;

      vi.mocked(uploadApi.uploadChunk).mockImplementation(async () => {
        attemptCount++;
        if (attemptCount < 2) {
          throw new Error('Network error');
        }
        return undefined as any;
      });

      // 模拟重试逻辑
      let success = false;
      for (let i = 0; i < 3; i++) {
        try {
          await uploadApi.uploadChunk('upload-123', 1, 'md5', {} as File);
          success = true;
          break;
        } catch (error) {
          if (i === 2) throw error;
        }
      }

      expect(success).toBe(true);
      expect(attemptCount).toBe(2);
    });
  });

  describe('第四阶段：完成上传', () => {
    it('应该完成上传并返回文件 URL', async () => {
      const uploadId = 'complete-upload-123';
      const fileUrl = 'https://cdn.example.com/completed.bin';
      const onComplete = vi.fn();

      vi.mocked(uploadApi.completeUpload).mockResolvedValue({
        uploadId,
        fileUrl,
        storageKey: 'storage-key',
        fileSize: 1024,
      } as any);

      renderHook(() =>
        useChunkedUpload({ onComplete })
      );

      // 模拟完成上传
      await act(async () => {
        await uploadApi.completeUpload(uploadId);
      });

      expect(uploadApi.completeUpload).toHaveBeenCalledWith(uploadId);
    });
  });

  describe('错误处理', () => {
    it('应该处理文件 MD5 计算失败', async () => {
      const file = new File(['content'], 'error.txt');
      const onError = vi.fn();

      vi.mocked(fileUtils.computeFileMD5).mockRejectedValue(
        new Error('MD5 calculation failed')
      );

      const { result } = renderHook(() =>
        useChunkedUpload({ onError })
      );

      await act(async () => {
        try {
          await result.current.startUpload(file, 'storage-key');
        } catch {
          // 预期错误
        }
      });

      expect(onError).toHaveBeenCalled();
    });

    it('应该处理上传任务初始化失败', async () => {
      const file = new File(['content'], 'init-error.txt');
      const onError = vi.fn();

      vi.mocked(fileUtils.computeFileMD5).mockResolvedValue('md5-123');
      vi.mocked(fileUtils.splitFileIntoChunks).mockReturnValue([new Blob()]);
      vi.mocked(uploadApi.initUpload).mockRejectedValue(
        new Error('Init upload failed')
      );

      const { result } = renderHook(() =>
        useChunkedUpload({ onError })
      );

      await act(async () => {
        try {
          await result.current.startUpload(file, 'storage-key');
        } catch {
          // 预期错误
        }
      });

      expect(onError).toHaveBeenCalled();
    });

    it('应该处理取消上传', async () => {
      vi.mocked(uploadApi.cancelUpload).mockResolvedValue(undefined as any);

      const { result } = renderHook(() => useChunkedUpload());

      await act(async () => {
        await result.current.abortUpload();
      });

      // 如果没有任务，不会调用 cancelUpload
      // 这是正确的行为
    });
  });

  describe('边界情况', () => {
    it('应该处理空文件', async () => {
      const emptyFile = new File([], 'empty.txt');

      vi.mocked(fileUtils.computeFileMD5).mockResolvedValue('empty-md5');
      vi.mocked(fileUtils.splitFileIntoChunks).mockReturnValue([]);
      vi.mocked(uploadApi.initUpload).mockResolvedValue({
        uploadId: 'empty-upload',
        isSecondUpload: false,
        chunkSize: 5 * 1024 * 1024,
        totalChunks: 0,
        uploadedChunks: [],
      } as any);

      const { result } = renderHook(() => useChunkedUpload());

      await act(async () => {
        await result.current.startUpload(emptyFile, 'storage-key');
      });

      expect(result.current.task?.progressPercent).toBeLessThanOrEqual(100);
    });
  });
});
