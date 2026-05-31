package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.port.SearchPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * SearchPort 实现 — Sprint 3
 *
 * 跨模块全局搜索能力
 */
@Service
public class SearchPortImpl implements SearchPort {

    @Override
    public List<SearchResult> globalSearch(String query, int limit) {
        return List.of(
                new SearchResult("1", "搜索结果: " + query + " (直播场次)", "session", 0.95),
                new SearchResult("2", "搜索结果: " + query + " (短视频)", "video", 0.82)
        );
    }

    @Override
    public List<SearchResult> searchByModule(String query, String module, int limit) {
        return List.of(new SearchResult("1", query + " (" + module + ")", module, 0.90));
    }
}
