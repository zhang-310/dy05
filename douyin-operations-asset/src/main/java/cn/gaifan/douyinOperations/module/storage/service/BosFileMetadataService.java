package cn.gaifan.douyinOperations.module.storage.service;

/**
 * BOS 文件元数据服务：记录、查询、统计
 */
public interface BosFileMetadataService {

    /**
     * 记录文件上传元数据
     */
    void recordUpload(String bosKey, Long userId, Long taskId, String category, long fileSize, Long shotId, String sourceUrl);

    /**
     * 增加使用次数
     */
    void incrementUsageCount(String bosKey);
}
