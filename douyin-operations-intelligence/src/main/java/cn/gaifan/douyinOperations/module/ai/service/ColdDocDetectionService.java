package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 冷门文档检测：90 天未检索、未引用的文档，用于归档建议。
 */
public interface ColdDocDetectionService {

    /**
     * 检测冷门文档（默认 90 天）
     *
     * @param coldDays 判定冷门的天数阈值
     * @param maxResults 最多返回条数
     * @return 冷门文档列表及统计
     */
    Map<String, Object> detectColdDocs(int coldDays, int maxResults);

    /**
     * 归档指定冷门文档（软删除，删除向量与 ES 索引）
     *
     * @param docIds 要归档的文档 ID 列表
     * @return 成功数量、失败数量、错误信息
     */
    Map<String, Object> archiveColdDocs(List<Long> docIds);
}
