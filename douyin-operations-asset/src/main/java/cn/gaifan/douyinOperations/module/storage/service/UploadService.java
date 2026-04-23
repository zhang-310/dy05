package cn.gaifan.douyinOperations.module.storage.service;

import cn.gaifan.douyinOperations.module.storage.vo.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 分块上传服务接口
 */
public interface UploadService {

    /**
     * 初始化上传
     * 返回上传 ID 和分块信息（支持秒传检查）
     */
    UploadInitResultVO initUpload(Long userId, UploadInitVO vo);

    /**
     * 秒传检查（被 initUpload 内部调用）
     */
    String checkSecondUpload(String fileMd5, Long userId, Long fileSize);

    /**
     * 上传分块
     */
    void uploadChunk(Long userId, String uploadId, Integer chunkIndex,
                     String chunkMd5, MultipartFile chunkFile);

    /**
     * 查询上传进度
     */
    UploadProgressVO getProgress(Long userId, String uploadId);

    /**
     * 查询已上传分块列表（用于断点续传恢复）
     */
    List<Integer> getUploadedChunks(Long userId, String uploadId);

    /**
     * 完成上传（触发 BOS 多部分上传合并）
     */
    UploadCompleteResultVO completeUpload(Long userId, String uploadId);

    /**
     * 取消上传（清理临时分块）
     */
    void cancelUpload(Long userId, String uploadId);

    /**
     * 定时清理过期任务（7 天未完成）
     */
    long cleanupExpiredTasks();
}
