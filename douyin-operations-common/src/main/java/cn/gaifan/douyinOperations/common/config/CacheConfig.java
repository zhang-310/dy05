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
}
