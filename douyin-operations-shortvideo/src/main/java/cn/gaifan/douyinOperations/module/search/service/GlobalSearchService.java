package cn.gaifan.douyinOperations.module.search.service;

import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchRequestVO;
import cn.gaifan.douyinOperations.module.search.vo.GlobalSearchResponseVO;

import java.util.List;

public interface GlobalSearchService {

    GlobalSearchResponseVO search(GlobalSearchRequestVO request, List<Long> visibleUserIds);
}
