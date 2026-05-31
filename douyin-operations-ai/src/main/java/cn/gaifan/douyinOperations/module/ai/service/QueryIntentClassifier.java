package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.search.QueryIntent;

/**
 * S-4：根据用户查询文本分类检索意图（规则为主，可关闭）。
 */
public interface QueryIntentClassifier {

    /**
     * @param query 已 trim 的查询；空则 {@link QueryIntent#GENERAL}
     */
    QueryIntent classify(String query);
}
