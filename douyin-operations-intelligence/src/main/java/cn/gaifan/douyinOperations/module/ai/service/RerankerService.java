package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 重排序服务：对候选文档进行 Cross-Encoder 精排
 * 可选实现，未配置时跳过
 */
public interface RerankerService {

    /** 是否可用 */
    boolean isAvailable();

    /**
     * 对 (query, candidate) 对进行重排，返回相关性分数（0-1）
     *
     * @param query      用户查询
     * @param candidates 候选文本列表
     * @return 每个候选的分数；不可用时返回 null
     */
    List<Float> rerank(String query, List<String> candidates);
}
