package cn.gaifan.douyinOperations.module.script.service;

import java.util.Optional;

public interface ScriptCacheService {
    /**
     * 获取缓存的话术
     */
    Optional<String> getScript(String key);

    /**
     * 缓存话术
     */
    void cacheScript(String key, String content, long ttlSeconds);

    /**
     * 清除缓存
     */
    void clearCache(String key);

    /**
     * 清除所有缓存
     */
    void clearAllCache();

    /**
     * 获取缓存统计
     */
    CacheStats getStats();

    class CacheStats {
        public long hits;
        public long misses;
        public long size;

        public CacheStats(long hits, long misses, long size) {
            this.hits = hits;
            this.misses = misses;
            this.size = size;
        }

        public double getHitRate() {
            long total = hits + misses;
            return total == 0 ? 0 : (double) hits / total;
        }
    }
}
