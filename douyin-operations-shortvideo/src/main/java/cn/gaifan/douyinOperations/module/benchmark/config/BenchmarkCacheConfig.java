package cn.gaifan.douyinOperations.module.benchmark.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * 对标账号分析系统缓存配置
 *
 * 缓存策略：
 * - L1 缓存：Caffeine 本地内存缓存（快速访问）
 * - L2 缓存：Redis 分布式缓存（跨实例共享）
 */
@Configuration
@EnableCaching
public class BenchmarkCacheConfig {

    // 缓存名称常量
    public static final String CACHE_ACCOUNT_SEARCH = "benchmarkAccountSearch";
    public static final String CACHE_ACCOUNT_DETAIL = "benchmarkAccountDetail";
    public static final String CACHE_VIDEO_LIST = "benchmarkVideoList";
    public static final String CACHE_QUALITY_SCRIPTS = "qualityScripts";
    public static final String CACHE_SIMILARITIES = "similarities";
    public static final String CACHE_RECOMMENDATIONS = "recommendations";
    public static final String CACHE_EMBEDDINGS = "embeddings";

    /**
     * 对标账号搜索缓存（5分钟过期）
     * 用于缓存账号搜索结果
     */
    @Bean
    public CacheManager benchmarkAccountSearchCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_ACCOUNT_SEARCH);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }

    /**
     * 对标账号详情缓存（10分钟过期）
     * 用于缓存单个账号详情
     */
    @Bean
    public CacheManager benchmarkAccountDetailCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_ACCOUNT_DETAIL);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(300)
                .recordStats());
        return cacheManager;
    }

    /**
     * 对标视频列表缓存（5分钟过期）
     * 用于缓存账号下的视频列表
     */
    @Bean
    public CacheManager benchmarkVideoListCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_VIDEO_LIST);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(800)
                .recordStats());
        return cacheManager;
    }

    /**
     * 质量脚本缓存（5分钟过期）
     * 用于缓存频繁访问的质量脚本数据
     */
    @Bean
    public CacheManager benchmarkQualityScriptCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_QUALITY_SCRIPTS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats());
        return cacheManager;
    }

    /**
     * 相似度计算缓存（10分钟过期）
     * 用于缓存向量相似度计算结果
     */
    @Bean
    public CacheManager benchmarkSimilarityCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_SIMILARITIES);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(5000)
                .recordStats());
        return cacheManager;
    }

    /**
     * 推荐结果缓存（15分钟过期）
     * 用于缓存推荐查询结果
     */
    @Bean
    public CacheManager benchmarkRecommendationCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_RECOMMENDATIONS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(15, TimeUnit.MINUTES)
                .maximumSize(2000)
                .recordStats());
        return cacheManager;
    }

    /**
     * 向量嵌入缓存（30分钟过期）
     * 用于缓存生成的向量嵌入
     */
    @Bean
    public CacheManager benchmarkEmbeddingCacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager(CACHE_EMBEDDINGS);
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats());
        return cacheManager;
    }
}
