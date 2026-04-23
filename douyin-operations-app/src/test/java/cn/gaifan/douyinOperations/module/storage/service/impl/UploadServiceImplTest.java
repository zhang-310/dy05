package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.storage.entity.SysUploadChunk;
import cn.gaifan.douyinOperations.module.storage.entity.SysUploadTask;
import cn.gaifan.douyinOperations.module.storage.repository.SysUploadChunkRepository;
import cn.gaifan.douyinOperations.module.storage.repository.SysUploadTaskRepository;
import cn.gaifan.douyinOperations.module.storage.vo.UploadCompleteResultVO;
import cn.gaifan.douyinOperations.module.storage.vo.UploadInitResultVO;
import cn.gaifan.douyinOperations.module.storage.vo.UploadInitVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UploadServiceImpl 单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("UploadServiceImpl 单元测试")
class UploadServiceImplTest {

    @Mock
    private SysUploadTaskRepository taskRepository;

    @Mock
    private SysUploadChunkRepository chunkRepository;

    @InjectMocks
    private UploadServiceImpl uploadService;

    private SysUploadTask sampleTask;

    @BeforeEach
    void setUp() {
        sampleTask = new SysUploadTask();
        sampleTask.setId(1L);
        sampleTask.setOwnerId(100L);
        sampleTask.setUploadId("test-upload-id");
        sampleTask.setOriginalFilename("test-file.mp4");
        sampleTask.setStorageKey("storage/test-file.mp4");
        sampleTask.setFileSize(10485760L); // 10 MB
        sampleTask.setFileMd5("abc123md5");
        sampleTask.setChunkSize(5242880); // 5 MB
        sampleTask.setTotalChunks(2);
        sampleTask.setUploadedChunks(0);
        sampleTask.setUploadedBytes(0L);
        sampleTask.setStatus("PENDING");
        sampleTask.setDeleted(0);
    }

    // ==================== initUpload (createTask) 测试 ====================

    @Nested
    @DisplayName("initUpload 方法测试")
    class InitUploadTests {

        @Test
        @DisplayName("createTask_validInput_success - 正常初始化上传任务")
        void createTask_validInput_success() {
            UploadInitVO vo = new UploadInitVO();
            vo.setOriginalFilename("test.mp4");
            vo.setFileSize(10485760L); // 10 MB
            vo.setFileMd5("abc123md5");
            vo.setStorageKey("storage/test.mp4");
            vo.setModule("video");

            // No existing task for second upload
            when(taskRepository.findByFileMd5AndOwnerIdAndFileSizeAndDeleted(
                    "abc123md5", 100L, 10485760L, 0))
                    .thenReturn(Optional.empty());
            when(taskRepository.save(any(SysUploadTask.class))).thenAnswer(invocation -> {
                SysUploadTask saved = invocation.getArgument(0);
                saved.setId(1L);
                return saved;
            });
            when(chunkRepository.save(any(SysUploadChunk.class))).thenAnswer(invocation -> {
                SysUploadChunk chunk = invocation.getArgument(0);
                chunk.setId((long) (chunk.getChunkIndex() + 1));
                return chunk;
            });

            UploadInitResultVO result = uploadService.initUpload(100L, vo);

            assertNotNull(result);
            assertFalse(result.getIsSecondUpload());
            assertNull(result.getSecondUploadUrl());
            assertEquals(2, result.getTotalChunks());
            assertNotNull(result.getUploadId());

            verify(taskRepository).save(any(SysUploadTask.class));
            verify(chunkRepository, times(2)).save(any(SysUploadChunk.class));
        }

        @Test
        @DisplayName("createTask_fileTooLarge_throwsException - 文件超过上限抛出异常")
        void createTask_fileTooLarge_throwsException() {
            UploadInitVO vo = new UploadInitVO();
            vo.setOriginalFilename("huge-file.bin");
            vo.setFileSize(11L * 1024 * 1024 * 1024); // 11 GB, exceeds 10 GB
            vo.setFileMd5("largefile-md5");
            vo.setStorageKey("storage/huge-file.bin");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.initUpload(100L, vo));

            assertEquals(ErrorCode.STORAGE_FILE_TOO_LARGE, ex.getCode());
            assertTrue(ex.getMessage().contains("10 GB"));
        }

        @Test
        @DisplayName("createTask_zeroFileSize_throwsException - 文件大小为 0 抛出异常")
        void createTask_zeroFileSize_throwsException() {
            UploadInitVO vo = new UploadInitVO();
            vo.setOriginalFilename("empty.txt");
            vo.setFileSize(0L);
            vo.setFileMd5("empty-md5");
            vo.setStorageKey("storage/empty.txt");

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.initUpload(100L, vo));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("createTask_secondUpload_returnsExistingUrl - 秒传返回已有URL")
        void createTask_secondUpload_returnsExistingUrl() {
            UploadInitVO vo = new UploadInitVO();
            vo.setOriginalFilename("existing.mp4");
            vo.setFileSize(10485760L);
            vo.setFileMd5("existing-md5");
            vo.setStorageKey("storage/existing.mp4");

            SysUploadTask existingTask = new SysUploadTask();
            existingTask.setStatus("COMPLETED");
            existingTask.setStorageKey("storage/existing.mp4");

            when(taskRepository.findByFileMd5AndOwnerIdAndFileSizeAndDeleted(
                    "existing-md5", 100L, 10485760L, 0))
                    .thenReturn(Optional.of(existingTask));

            UploadInitResultVO result = uploadService.initUpload(100L, vo);

            assertNotNull(result);
            assertTrue(result.getIsSecondUpload());
            assertEquals("storage/existing.mp4", result.getSecondUploadUrl());
            assertEquals(0, result.getTotalChunks());
        }
    }

    // ==================== uploadChunk 测试 ====================

    @Nested
    @DisplayName("uploadChunk 方法测试")
    class UploadChunkTests {

        @Test
        @DisplayName("uploadChunk_validChunk_success - 正常上传分块")
        void uploadChunk_validChunk_success() {
            MultipartFile chunkFile = mock(MultipartFile.class);
            when(chunkFile.isEmpty()).thenReturn(false);

            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            SysUploadChunk existingChunk = new SysUploadChunk();
            existingChunk.setId(1L);
            existingChunk.setTaskId(1L);
            existingChunk.setChunkIndex(0);
            existingChunk.setChunkSize(5242880L);
            existingChunk.setStatus("PENDING");
            existingChunk.setRetryCount(0);

            when(chunkRepository.findByTaskIdAndChunkIndex(1L, 0)).thenReturn(Optional.of(existingChunk));

            // After updateChunk, findByTaskIdAndStatus returns completed chunks with valid chunkSize
            SysUploadChunk completedChunk = new SysUploadChunk();
            completedChunk.setId(1L);
            completedChunk.setChunkIndex(0);
            completedChunk.setChunkSize(5242880L);
            completedChunk.setStatus("COMPLETED");
            when(chunkRepository.findByTaskIdAndStatus(1L, "COMPLETED")).thenReturn(List.of(completedChunk));

            assertDoesNotThrow(() ->
                    uploadService.uploadChunk(100L, "test-upload-id", 0, "chunk-md5", chunkFile));

            verify(chunkRepository).updateChunk(eq(1L), eq("COMPLETED"), anyString(), eq(1), any(Timestamp.class));
        }

        @Test
        @DisplayName("uploadChunk_wrongUser_throwsException - 非文件所有者上传抛出异常")
        void uploadChunk_wrongUser_throwsException() {
            MultipartFile chunkFile = mock(MultipartFile.class);
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadChunk(200L, "test-upload-id", 0, "md5", chunkFile));

            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
            assertTrue(ex.getMessage().contains("无权操作"));
        }

        @Test
        @DisplayName("uploadChunk_taskNotFound_throwsException - 任务不存在抛出异常")
        void uploadChunk_taskNotFound_throwsException() {
            MultipartFile chunkFile = mock(MultipartFile.class);
            when(taskRepository.findByUploadId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadChunk(100L, "nonexistent", 0, "md5", chunkFile));

            assertEquals(ErrorCode.STORAGE_FILE_NOT_FOUND, ex.getCode());
        }

        @Test
        @DisplayName("uploadChunk_completedTask_throwsException - 已完成任务拒绝上传")
        void uploadChunk_completedTask_throwsException() {
            sampleTask.setStatus("COMPLETED");
            MultipartFile chunkFile = mock(MultipartFile.class);
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadChunk(100L, "test-upload-id", 0, "md5", chunkFile));

            assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, ex.getCode());
        }

        @Test
        @DisplayName("uploadChunk_invalidChunkIndex_throwsException - 分块序号超范围抛出异常")
        void uploadChunk_invalidChunkIndex_throwsException() {
            MultipartFile chunkFile = mock(MultipartFile.class);
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadChunk(100L, "test-upload-id", 10, "md5", chunkFile));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("uploadChunk_emptyFile_throwsException - 空文件抛出异常")
        void uploadChunk_emptyFile_throwsException() {
            MultipartFile chunkFile = mock(MultipartFile.class);
            when(chunkFile.isEmpty()).thenReturn(true);
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.uploadChunk(100L, "test-upload-id", 0, "md5", chunkFile));

            assertEquals(ErrorCode.VALIDATION_FAIL, ex.getCode());
        }

        @Test
        @DisplayName("uploadChunk_alreadyCompleted_idempotent - 已完成的分块幂等返回")
        void uploadChunk_alreadyCompleted_idempotent() {
            MultipartFile chunkFile = mock(MultipartFile.class);
            when(chunkFile.isEmpty()).thenReturn(false);
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            SysUploadChunk completedChunk = new SysUploadChunk();
            completedChunk.setId(1L);
            completedChunk.setTaskId(1L);
            completedChunk.setChunkIndex(0);
            completedChunk.setStatus("COMPLETED");

            when(chunkRepository.findByTaskIdAndChunkIndex(1L, 0)).thenReturn(Optional.of(completedChunk));

            assertDoesNotThrow(() ->
                    uploadService.uploadChunk(100L, "test-upload-id", 0, "md5", chunkFile));

            // Should not update chunk again
            verify(chunkRepository, never()).updateChunk(anyLong(), anyString(), anyString(), anyInt(), any());
        }
    }

    // ==================== completeUpload 测试 ====================

    @Nested
    @DisplayName("completeUpload 方法测试")
    class CompleteUploadTests {

        @Test
        @DisplayName("completeUpload_allChunksReceived_success - 所有分块完成后合并成功")
        void completeUpload_allChunksReceived_success() {
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            SysUploadChunk chunk1 = new SysUploadChunk();
            chunk1.setId(1L);
            chunk1.setChunkIndex(0);
            chunk1.setStatus("COMPLETED");

            SysUploadChunk chunk2 = new SysUploadChunk();
            chunk2.setId(2L);
            chunk2.setChunkIndex(1);
            chunk2.setStatus("COMPLETED");

            when(chunkRepository.findByTaskIdOrderByChunkIndex(1L)).thenReturn(List.of(chunk1, chunk2));
            when(taskRepository.save(any(SysUploadTask.class))).thenReturn(sampleTask);

            UploadCompleteResultVO result = uploadService.completeUpload(100L, "test-upload-id");

            assertNotNull(result);
            assertEquals("test-upload-id", result.getUploadId());
            assertNotNull(result.getFileUrl());
            assertEquals(sampleTask.getFileSize(), result.getFileSize());

            verify(taskRepository).save(argThat(task -> "COMPLETED".equals(task.getStatus())));
        }

        @Test
        @DisplayName("completeUpload_incompleteChunks_throwsException - 分块不完整抛出异常")
        void completeUpload_incompleteChunks_throwsException() {
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            SysUploadChunk chunk1 = new SysUploadChunk();
            chunk1.setId(1L);
            chunk1.setChunkIndex(0);
            chunk1.setStatus("COMPLETED");

            SysUploadChunk chunk2 = new SysUploadChunk();
            chunk2.setId(2L);
            chunk2.setChunkIndex(1);
            chunk2.setStatus("PENDING"); // Not completed

            when(chunkRepository.findByTaskIdOrderByChunkIndex(1L)).thenReturn(List.of(chunk1, chunk2));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.completeUpload(100L, "test-upload-id"));

            assertEquals(ErrorCode.OPERATION_NOT_ALLOWED, ex.getCode());
            assertTrue(ex.getMessage().contains("分块不完整"));
        }

        @Test
        @DisplayName("completeUpload_wrongUser_throwsException - 非文件所有者完成上传抛出异常")
        void completeUpload_wrongUser_throwsException() {
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.completeUpload(200L, "test-upload-id"));

            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }

        @Test
        @DisplayName("completeUpload_taskNotFound_throwsException - 任务不存在抛出异常")
        void completeUpload_taskNotFound_throwsException() {
            when(taskRepository.findByUploadId("nonexistent")).thenReturn(Optional.empty());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.completeUpload(100L, "nonexistent"));

            assertEquals(ErrorCode.STORAGE_FILE_NOT_FOUND, ex.getCode());
        }
    }

    // ==================== cancelUpload 测试 ====================

    @Nested
    @DisplayName("cancelUpload 方法测试")
    class CancelUploadTests {

        @Test
        @DisplayName("cancelUpload_success - 正常取消上传")
        void cancelUpload_success() {
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));
            when(taskRepository.save(any(SysUploadTask.class))).thenReturn(sampleTask);

            assertDoesNotThrow(() -> uploadService.cancelUpload(100L, "test-upload-id"));

            verify(chunkRepository).deleteByTaskId(1L);
            verify(taskRepository).save(argThat(task -> "CANCELLED".equals(task.getStatus())));
        }

        @Test
        @DisplayName("cancelUpload_wrongUser_throwsException - 非所有者取消抛出异常")
        void cancelUpload_wrongUser_throwsException() {
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.cancelUpload(200L, "test-upload-id"));

            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }
    }

    // ==================== getProgress 测试 ====================

    @Nested
    @DisplayName("getProgress 方法测试")
    class GetProgressTests {

        @Test
        @DisplayName("getProgress_validTask_returnsProgress - 正常查询进度")
        void getProgress_validTask_returnsProgress() {
            sampleTask.setUploadedChunks(1);
            sampleTask.setUploadedBytes(5242880L);
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            var result = uploadService.getProgress(100L, "test-upload-id");

            assertNotNull(result);
            assertEquals("test-upload-id", result.getUploadId());
            assertEquals(2, result.getTotalChunks());
            assertEquals(1, result.getUploadedChunks());
            assertEquals(50, result.getProgressPercent());
        }

        @Test
        @DisplayName("getProgress_wrongUser_throwsException - 非所有者查询进度抛出异常")
        void getProgress_wrongUser_throwsException() {
            when(taskRepository.findByUploadId("test-upload-id")).thenReturn(Optional.of(sampleTask));

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> uploadService.getProgress(200L, "test-upload-id"));

            assertEquals(ErrorCode.FORBIDDEN, ex.getCode());
        }
    }
}
