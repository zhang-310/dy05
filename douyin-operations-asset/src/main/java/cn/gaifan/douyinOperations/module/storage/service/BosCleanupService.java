package cn.gaifan.douyinOperations.module.storage.service;

/**
 * BOS 存储清理服务：僵尸文件清理、成本优化
 */
public interface BosCleanupService {

    /**
     * 清理超过指定天数未完成项目的素材文件
     *
     * @param daysAbandoned 未完成天数阈值，默认 30
     */
    int cleanupAbandonedProjectFiles(int daysAbandoned);
}
