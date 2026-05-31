package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 查询改写服务：结合用户画像将原始查询改写为 2-3 个更精准子查询
 */
public interface QueryRewriteService {

    /**
     * 改写查询（若用户已绑定抖音账号则结合画像，否则返回原查询）
     *
     * @param userId 用户 ID
     * @param query  原始查询
     * @return 子查询列表（至少包含原查询）
     */
    List<String> rewrite(Long userId, String query);
}
