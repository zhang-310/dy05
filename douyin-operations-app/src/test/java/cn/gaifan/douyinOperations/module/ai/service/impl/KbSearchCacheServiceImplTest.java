package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.KbSearchCacheService;
import cn.gaifan.douyinOperations.module.ai.service.KnowledgeBaseService.SearchResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 知识库检索缓存服务单元测试
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KbSearchCacheServiceImpl 缓存服务测试")
class KbSearchCacheServiceImplTest {

    @InjectMocks
    private KbSearchCacheServiceImpl kbSearchCacheService;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void buildKey_returnsConsistentKeyForSameInput() {
        ReflectionTestUtils.setField(kbSearchCacheService, "stringRedisTemplate", stringRedisTemplate);
        String key1 = kbSearchCacheService.buildKey(1L, "测试查询", 5);
        String key2 = kbSearchCacheService.buildKey(1L, "测试查询", 5);
        assertThat(key1).isNotNull().startsWith("cache:kb:1:");
        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void buildKey_differentInputs_produceDifferentKeys() {
        ReflectionTestUtils.setField(kbSearchCacheService, "stringRedisTemplate", stringRedisTemplate);
        String key1 = kbSearchCacheService.buildKey(1L, "查询A", 5);
        String key2 = kbSearchCacheService.buildKey(1L, "查询B", 5);
        String key3 = kbSearchCacheService.buildKey(2L, "查询A", 5);
        assertThat(key1).isNotEqualTo(key2).isNotEqualTo(key3);
    }

    @Test
    void get_whenRedisNull_returnsNull() {
        ReflectionTestUtils.setField(kbSearchCacheService, "stringRedisTemplate", null);
        assertThat(kbSearchCacheService.get("cache:kb:1:abc")).isNull();
    }

    @Test
    void get_whenCacheMiss_returnsNull() {
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("cache:kb:1:abc")).thenReturn(null);
        List<SearchResult> result = kbSearchCacheService.get("cache:kb:1:abc");
        assertThat(result).isNull();
    }

    @Test
    void put_whenRedisNull_doesNotThrow() {
        ReflectionTestUtils.setField(kbSearchCacheService, "stringRedisTemplate", null);
        List<SearchResult> list = List.of(new SearchResult(1L, "标题", "内容", 0.9, "vector"));
        kbSearchCacheService.put("key", list, 3600);
    }

    @Test
    void invalidateByKbId_whenRedisNull_doesNotThrow() {
        ReflectionTestUtils.setField(kbSearchCacheService, "stringRedisTemplate", null);
        kbSearchCacheService.invalidateByKbId(1L);
    }

    @Test
    void incrementStat_whenRedisNull_doesNotThrow() {
        ReflectionTestUtils.setField(kbSearchCacheService, "stringRedisTemplate", null);
        kbSearchCacheService.incrementStat("hit");
    }
}
