package cn.gaifan.douyinOperations.module.search.service;

import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchRequestVO;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchResponseVO;
import org.springframework.cache.annotation.Cacheable;

import java.util.List;

public interface GlobalSearchService {

    // P1-2: 添加搜索结果缓存（30秒 TTL）
    @Cacheable(value = "search:global",
               key = "#request.q + ':' + #visibleUserIds",
               unless = "#result == null || #result.hits.isEmpty()")
    GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds);
}
