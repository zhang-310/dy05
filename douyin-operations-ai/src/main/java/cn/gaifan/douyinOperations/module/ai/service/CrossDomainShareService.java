package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 跨域共享 Agent：将高分/高引用知识迁移到其他知识库
 * 阶段四实现框架
 */
public interface CrossDomainShareService {

    /**
     * 执行跨 KB 知识共享（同用户多 KB 场景）
     * 将高引用 evolved 文档的引用/摘要同步到其他 KB，供检索时参考
     *
     * @param userId 用户 ID
     * @param maxShares 本次最多处理条数
     * @return 执行结果：sharedCount, skippedCount, errors
     */
    Map<String, Object> runCrossDomainShare(Long userId, int maxShares);

    /**
     * 获取可共享的高价值文档列表（citation_count 或 retrieval_count 靠前）
     *
     * @param kbId 源知识库 ID
     * @param topN 取前 N 条
     * @return 文档列表，含 id, title, citationCount, retrievalCount
     */
    List<Map<String, Object>> getShareableDocs(Long kbId, int topN);
}
