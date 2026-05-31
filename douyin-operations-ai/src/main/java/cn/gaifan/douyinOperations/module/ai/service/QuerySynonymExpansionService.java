package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 检索同义词扩展：在 {@link QueryRewriteService} 之后追加少量等价词替换变体。
 */
public interface QuerySynonymExpansionService {

    List<String> expandQueries(List<String> queries);
}
