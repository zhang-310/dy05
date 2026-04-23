package cn.gaifan.douyinOperations.module.storage.entity;

/**
 * 分块上传状态枚举
 */
public enum ChunkStatus {
    PENDING,      // 待上传
    UPLOADING,    // 上传中
    COMPLETED,    // 已完成
    FAILED        // 失败
}
