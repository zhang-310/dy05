package cn.gaifan.douyinOperations.common.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class CacheConfig {

    @Bean("violationWordCache")
    public Cache<String, Object> violationWordCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    @Bean("knowledgeSearchCache")
    public Cache<String, Object> knowledgeSearchCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(1000)
                .build();
    }

    @Bean("modelConfigCache")
    public Cache<String, Object> modelConfigCache() {
        return Caffeine.newBuilder()
                .maximumSize(50)
                .build();
    }

    /**
     * P2-5: 账号统计 L1 本地缓存
     * 配合 Redis L2 缓存使用，减少网络开销
     */
    @Bean("accountStatisticsCache")
    public Cache<Long, Object> accountStatisticsCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /**
     * P0-6: 智能体列表 L1 本地缓存
     * 缓存用户的智能体列表查询结果，减少数据库查询
     * TTL: 5 分钟（智能体配置变更频率低）
     * 容量: 1000 个查询结果（支持 1000 个不同的查询条件组合）
     */
    @Bean("agentListCache")
    public Cache<String, Object> agentListCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    /**
     * P0-6: 智能体详情 L1 本地缓存
     * 缓存单个智能体的详细信息，减少数据库查询
     * TTL: 10 分钟（智能体配置变更频率低）
     * 容量: 500 个智能体（覆盖热门智能体）
     */
    @Bean("agentDetailCache")
    public Cache<Long, Object> agentDetailCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /**
     * P0-10: 直播场次列表 L1 本地缓存
     * 缓存用户的场次列表查询结果，减少数据库查询
     * TTL: 3 分钟（场次数据变更频率中等）
     * 容量: 1000 个查询结果（支持 1000 个不同的查询条件组合）
     */
    @Bean("liveSessionListCache")
    public Cache<String, Object> liveSessionListCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    /**
     * P0-10: 直播场次详情 L1 本地缓存
     * 缓存单个场次的详细信息，减少数据库查询
     * TTL: 5 分钟（场次配置变更频率低）
     * 容量: 500 个场次（覆盖热门场次）
     */
    @Bean("liveSessionDetailCache")
    public Cache<Long, Object> liveSessionDetailCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /**
     * P0-10: 直播话术列表 L1 本地缓存
     * 缓存场次的话术列表查询结果
     * TTL: 3 分钟（话术变更频率中等）
     * 容量: 500 个查询结果
     */
    @Bean("liveScriptListCache")
    public Cache<String, Object> liveScriptListCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(3, TimeUnit.MINUTES)
                .maximumSize(500)
                .build();
    }

    /**
     * P0-10: 直播话术详情 L1 本地缓存
     * 缓存单条话术的详细信息
     * TTL: 5 分钟
     * 容量: 1000 条话术
     */
    @Bean("liveScriptDetailCache")
    public Cache<Long, Object> liveScriptDetailCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(1000)
                .build();
    }

    /**
     * P0-3: 文案库详情 L1 本地缓存
     * 缓存单个文案的详细信息，减少数据库查询
     * TTL: 10 分钟（文案变更频率低）
     * 容量: 1000 个文案（覆盖热门文案）
     */
    @Bean("copyLibraryCache")
    public Cache<Long, Object> copyLibraryCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(1000)
                .recordStats()
                .build();
    }

    /**
     * P0-3: 文案模板详情 L1 本地缓存
     * 缓存单个模板的详细信息
     * TTL: 10 分钟（模板变更频率低）
     * 容量: 500 个模板
     */
    @Bean("copyTemplateCache")
    public Cache<Long, Object> copyTemplateCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }

    /**
     * P0-3: 文案审批详情 L1 本地缓存
     * 缓存单个审批记录的详细信息
     * TTL: 5 分钟（审批状态变更频率中等）
     * 容量: 500 个审批记录
     */
    @Bean("copyApprovalCache")
    public Cache<Long, Object> copyApprovalCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500)
                .recordStats()
                .build();
    }

    /**
     * P1-8: 系统配置 L1 本地缓存
     * 缓存系统配置项，减少 Redis 网络开销
     * TTL: 1 小时（配置变更频率低）
     * 容量: 10000 个配置项（覆盖所有配置）
     */
    @Bean("sysConfigCache")
    public Cache<String, Object> sysConfigCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(1, TimeUnit.HOURS)
                .maximumSize(10000)
                .recordStats()
                .build();
    }
}
