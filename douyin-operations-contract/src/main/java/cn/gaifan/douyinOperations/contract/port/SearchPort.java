package cn.gaifan.douyinOperations.contract.port;

/**
 * 搜索 Port — 跨模块搜索能力
 */
public interface SearchPort {
    record SearchResult(String id, String title, String type, double score) {}
    java.util.List<SearchResult> globalSearch(String query, int limit);
    java.util.List<SearchResult> searchByModule(String query, String module, int limit);
}
