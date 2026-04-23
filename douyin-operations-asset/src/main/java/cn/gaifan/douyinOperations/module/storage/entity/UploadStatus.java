package cn.gaifan.douyinOperations.module.storage.entity;

/**
 * 上传任务状态枚举
 */
public enum UploadStatus {
    PENDING,      // 初始状态，待开始上传
    UPLOADING,    // 上传中（至少一个分块已上传）
    COMPLETED,    // 完成（所有分块上传并合并成功）
    CANCELLED,    // 取消
    FAILED        // 失败
}
