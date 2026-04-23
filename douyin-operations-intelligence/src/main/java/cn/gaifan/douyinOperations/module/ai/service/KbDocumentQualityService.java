package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 知识库文档质量分层（E-3）：检索反馈与用户启发式扫描，写入 {@code AiKbDocument.quality_tier}。
 */
public interface KbDocumentQualityService {

    int TIER_NEUTRAL = 0;
    int TIER_HEALTHY = 1;
    int TIER_LOW = 2;

    /** 将文档标记为指定 tier（0–2），并更新 last_quality_eval_at */
    void markTier(Long docId, int tier);

    /**
     * 启发式分页扫描：按检索/引用/boost/时效等计算 0–100 分并回写 tier。
     *
     * @return 本批实际更新条数；若该页无数据返回 **-1**（表示已扫完）
     */
    int scanHeuristicPage(int pageIndex, int pageSize);
}
