package cn.gaifan.douyinOperations.module.ai.service;

import java.util.Map;

/**
 * huashu 知识库运维：清理、回填、重索引
 */
public interface HuashuMaintenanceService {

    /**
     * 清理 huashu 中 source_type=cross_share 的历史文档（软删除+向量/ES 移除）
     */
    Map<String, Object> cleanupCrossShare(Long userId);

    /**
     * 为 huashu 中 source_type 为空或 cross_share 的文档补全为 manual 并重索引
     */
    Map<String, Object> backfillSourceTypeAndReindex(Long userId, int maxDocs);
}
