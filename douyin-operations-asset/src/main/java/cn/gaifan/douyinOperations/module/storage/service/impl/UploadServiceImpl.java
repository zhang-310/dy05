package cn.gaifan.douyinOperations.module.storage.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.storage.entity.SysUploadChunk;
import cn.gaifan.douyinOperations.module.storage.entity.SysUploadTask;
import cn.gaifan.douyinOperations.module.storage.repository.SysUploadChunkRepository;
import cn.gaifan.douyinOperations.module.storage.repository.SysUploadTaskRepository;
import cn.gaifan.douyinOperations.module.storage.service.UploadService;
import cn.gaifan.douyinOperations.module.storage.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import java.io.InputStream;
import java.sql.Timestamp;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UploadServiceImpl implements UploadService {

    @Resource
    private SysUploadTaskRepository taskRepository;

    @Resource
    private SysUploadChunkRepository chunkRepository;

    private static final int DEFAULT_CHUNK_SIZE = 5242880;  // 5 MB
    private static final int MAX_RETRIES = 3;

    // P1-1: 文件类型白名单
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
        "jpg", "jpeg", "png", "gif", "bmp", "webp",  // 图片
        "mp4", "avi", "mov", "wmv", "flv", "mkv",    // 视频
        "mp3", "wav", "aac", "flac", "ogg",          // 音频
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "csv",  // 文档
        "zip", "rar", "7z", "tar", "gz"              // 压缩包
    );

    /**
     * 初始化上传
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadInitResultVO initUpload(Long userId, UploadInitVO vo) {
        // P1-1: 文件类型白名单校验
        String filename = vo.getOriginalFilename();
        if (filename == null || filename.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文件名不能为空");
        }
        String extension = getFileExtension(filename).toLowerCase();
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "不支持的文件类型: " + extension);
        }

        // 参数校验
        if (vo.getFileSize() <= 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "文件大小必须大于 0");
        }
        if (vo.getFileSize() > 10L * 1024 * 1024 * 1024) {  // 10 GB 上限
            throw new BusinessException(ErrorCode.STORAGE_FILE_TOO_LARGE, "文件超过 10 GB 上限");
        }

        // 检查秒传
        String secondUploadUrl = checkSecondUpload(vo.getFileMd5(), userId, vo.getFileSize());
        if (secondUploadUrl != null) {
            return new UploadInitResultVO(
                UUID.randomUUID().toString(),
                true,
                secondUploadUrl,
                DEFAULT_CHUNK_SIZE,
                0,
                Collections.emptyList()
            );
        }

        // 创建新任务
        String uploadId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) vo.getFileSize() / DEFAULT_CHUNK_SIZE);

        SysUploadTask task = new SysUploadTask();
        task.setOwnerId(userId);
        task.setUploadId(uploadId);
        task.setOriginalFilename(vo.getOriginalFilename());
        task.setStorageKey(vo.getStorageKey());
        task.setFileSize(vo.getFileSize());
        task.setFileMd5(vo.getFileMd5());
        task.setChunkSize(DEFAULT_CHUNK_SIZE);
        task.setTotalChunks(totalChunks);
        task.setModule(vo.getModule());
        task.setStatus("PENDING");

        task = taskRepository.save(task);

        // 创建分块记录
        for (int i = 0; i < totalChunks; i++) {
            SysUploadChunk chunk = new SysUploadChunk();
            chunk.setTaskId(task.getId());
            chunk.setChunkIndex(i);
            long chunkSize = Math.min(DEFAULT_CHUNK_SIZE, vo.getFileSize() - (long) i * DEFAULT_CHUNK_SIZE);
            chunk.setChunkSize(chunkSize);
            chunk.setStatus("PENDING");
            chunkRepository.save(chunk);
        }

        log.info("初始化上传: uploadId={}, fileSize={} bytes, chunks={}", uploadId, vo.getFileSize(), totalChunks);

        return new UploadInitResultVO(
            uploadId,
            false,
            null,
            DEFAULT_CHUNK_SIZE,
            totalChunks,
            Collections.emptyList()
        );
    }

    /**
     * 秒传检查
     */
    @Override
    public String checkSecondUpload(String fileMd5, Long userId, Long fileSize) {
        Optional<SysUploadTask> existing = taskRepository.findByFileMd5AndOwnerIdAndFileSizeAndDeleted(
            fileMd5, userId, fileSize, 0
        );

        if (existing.isPresent()) {
            SysUploadTask task = existing.get();
            if ("COMPLETED".equals(task.getStatus())) {
                // 返回已有文件 URL（这里简化，实际应该关联 sys_file 表获取真实 URL）
                return task.getStorageKey();
            }
        }

        return null;
    }

    /**
     * 上传分块
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uploadChunk(Long userId, String uploadId, Integer chunkIndex,
                           String chunkMd5, MultipartFile chunkFile) {
        // 查询任务
        SysUploadTask task = taskRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND, "上传任务不存在"));

        // 权限检查
        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该上传任务");
        }

        // 状态检查
        if (!("PENDING".equals(task.getStatus()) || "UPLOADING".equals(task.getStatus()))) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "上传已完成或已取消");
        }

        // 分块索引校验
        if (chunkIndex < 0 || chunkIndex >= task.getTotalChunks()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "分块序号超出范围");
        }

        // 文件不为空
        if (chunkFile == null || chunkFile.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAIL, "分块文件不能为空");
        }

        // 查询或创建分块记录
        Optional<SysUploadChunk> existing = chunkRepository.findByTaskIdAndChunkIndex(task.getId(), chunkIndex);
        SysUploadChunk chunk;

        if (existing.isPresent()) {
            chunk = existing.get();
            // 幂等性：已完成则直接返回
            if ("COMPLETED".equals(chunk.getStatus())) {
                log.info("分块已上传: uploadId={}, chunkIndex={}, 幂等返回成功", uploadId, chunkIndex);
                return;
            }
        } else {
            chunk = new SysUploadChunk();
            chunk.setTaskId(task.getId());
            chunk.setChunkIndex(chunkIndex);
            chunk.setChunkSize(chunkFile.getSize());
            chunk.setChunkMd5(chunkMd5);
            chunk.setStatus("PENDING");
        }

        try {
            // 这里应该调用 BOS SDK 上传分块
            // 为简化，仅做状态更新
            chunk.setStatus("COMPLETED");
            chunk.setUploadedAt(new Timestamp(System.currentTimeMillis()));
            chunk.setBosEtag(UUID.randomUUID().toString());  // 模拟 ETag
            chunk.setBosPartNumber(chunkIndex + 1);

            if (chunk.getId() == null) {
                chunkRepository.save(chunk);
            } else {
                Timestamp now = new Timestamp(System.currentTimeMillis());
                chunkRepository.updateChunk(chunk.getId(), "COMPLETED", chunk.getBosEtag(),
                    chunk.getBosPartNumber(), now);
            }

            // 更新任务进度
            List<SysUploadChunk> uploadedChunks = chunkRepository.findByTaskIdAndStatus(task.getId(), "COMPLETED");
            int uploadedCount = uploadedChunks.size();
            long uploadedBytes = uploadedChunks.stream().mapToLong(SysUploadChunk::getChunkSize).sum();

            String newStatus = (uploadedCount == task.getTotalChunks()) ? "UPLOADING" : "UPLOADING";
            Timestamp now = new Timestamp(System.currentTimeMillis());
            taskRepository.updateProgress(task.getId(), uploadedCount, uploadedBytes, now);
            taskRepository.updateStatus(task.getId(), newStatus, now);

            log.info("分块上传: uploadId={}, chunkIndex={}, status=completed", uploadId, chunkIndex);

        } catch (Exception e) {
            chunk.setRetryCount(chunk.getRetryCount() + 1);
            chunk.setStatus("FAILED");
            chunkRepository.save(chunk);
            log.error("分块上传失败: uploadId={}, chunkIndex={}, 原因={}", uploadId, chunkIndex, e.getMessage());
            throw new BusinessException(ErrorCode.STORAGE_UPLOAD_FAIL, "分块上传失败: " + e.getMessage());
        }
    }

    /**
     * P1-1: 获取文件扩展名
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.') + 1);
    }

    /**
     * 查询上传进度
     */
    @Override
    public UploadProgressVO getProgress(Long userId, String uploadId) {
        SysUploadTask task = taskRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND, "上传任务不存在"));

        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查询该上传任务");
        }

        int progressPercent = (int) (task.getUploadedBytes() * 100 / task.getFileSize());

        return new UploadProgressVO(
            uploadId,
            task.getTotalChunks(),
            task.getUploadedChunks(),
            task.getUploadedBytes(),
            task.getFileSize(),
            progressPercent,
            task.getStatus()
        );
    }

    /**
     * 查询已上传分块
     */
    @Override
    public List<Integer> getUploadedChunks(Long userId, String uploadId) {
        SysUploadTask task = taskRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND, "上传任务不存在"));

        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权查询该上传任务");
        }

        return chunkRepository.findByTaskIdAndStatus(task.getId(), "COMPLETED")
            .stream()
            .map(SysUploadChunk::getChunkIndex)
            .sorted()
            .collect(Collectors.toList());
    }

    /**
     * 完成上传
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UploadCompleteResultVO completeUpload(Long userId, String uploadId) {
        SysUploadTask task = taskRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND, "上传任务不存在"));

        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该上传任务");
        }

        // 检查所有分块是否已完成
        List<SysUploadChunk> chunks = chunkRepository.findByTaskIdOrderByChunkIndex(task.getId());
        long completedCount = chunks.stream().filter(c -> "COMPLETED".equals(c.getStatus())).count();

        if (completedCount != chunks.size()) {
            throw new BusinessException(ErrorCode.OPERATION_NOT_ALLOWED, "分块不完整，不能完成上传");
        }

        // 更新任务状态
        task.setStatus("COMPLETED");
        task.setCompletedAt(new Timestamp(System.currentTimeMillis()));
        taskRepository.save(task);

        // 返回结果（模拟，实际应该创建 sys_file 记录）
        String fileUrl = "https://bucket.bj.bcebos.com/" + task.getStorageKey();

        log.info("完成上传: uploadId={}, fileUrl={}", uploadId, fileUrl);

        return new UploadCompleteResultVO(
            uploadId,
            fileUrl,
            task.getStorageKey(),
            task.getFileSize()
        );
    }

    /**
     * 取消上传
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelUpload(Long userId, String uploadId) {
        SysUploadTask task = taskRepository.findByUploadId(uploadId)
            .orElseThrow(() -> new BusinessException(ErrorCode.STORAGE_FILE_NOT_FOUND, "上传任务不存在"));

        if (!task.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该上传任务");
        }

        // 清理分块记录
        chunkRepository.deleteByTaskId(task.getId());

        // 标记任务为已取消
        task.setStatus("CANCELLED");
        taskRepository.save(task);

        log.info("取消上传: uploadId={}", uploadId);
    }

    /**
     * 定时清理过期任务
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public long cleanupExpiredTasks() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        int deletedCount = taskRepository.deleteExpiredTasks(now, now);
        log.info("清理过期上传任务: 删除 {} 个", deletedCount);
        return deletedCount;
    }
}
