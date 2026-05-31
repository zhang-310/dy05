package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.entity.ScriptLibrary;
import cn.gaifan.douyinOperations.module.script.entity.SearchResult;
import cn.gaifan.douyinOperations.module.script.entity.ScriptVectorEmbedding;
import cn.gaifan.douyinOperations.module.script.repository.ScriptLibraryRepository;
import cn.gaifan.douyinOperations.module.script.repository.ScriptVectorEmbeddingRepository;
import cn.gaifan.douyinOperations.module.script.repository.SearchAnalyticsRepository;
import cn.gaifan.douyinOperations.module.script.repository.SearchResultRepository;
import cn.gaifan.douyinOperations.module.script.repository.SearchSuggestionRepository;
import cn.gaifan.douyinOperations.module.script.service.SearchSuggestionService;
import cn.gaifan.douyinOperations.module.script.service.VectorEmbeddingService;
import cn.gaifan.douyinOperations.module.script.vo.HybridSearchRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.HybridSearchResultVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VectorSearchServiceImplTest {

    @Mock
    private ScriptLibraryRepository scriptLibraryRepository;

    @Mock
    private ScriptVectorEmbeddingRepository scriptVectorEmbeddingRepository;

    @Mock
    private SearchResultRepository searchResultRepository;

    @Mock
    private SearchSuggestionRepository searchSuggestionRepository;

    @Mock
    private SearchAnalyticsRepository searchAnalyticsRepository;

    @Mock
    private VectorEmbeddingService vectorEmbeddingService;

    @Mock
    private SearchSuggestionService searchSuggestionService;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private VectorSearchServiceImpl service;

    private ScriptLibrary script;

    @BeforeEach
    void setUp() {
        script = new ScriptLibrary();
        script.setId(1L);
        script.setTitle("测试话术");
        script.setContent("测试查询关键词话术内容");
        script.setCategory("护肤");
        script.setUserId(100L);
        script.setDeleted(0);
        script.setCreateTime(new Timestamp(System.currentTimeMillis()));
    }

    @Test
    void hybridSearch_keywordWeightSkipsVectorAndUsesWeightedCacheKey() {
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("测试查询");
        request.setPage(1);
        request.setRows(5);
        request.setTopK(5);
        request.setVectorWeight(BigDecimal.ZERO);
        request.setLexicalWeight(BigDecimal.ONE);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any(String.class))).thenReturn(null);
        when(scriptLibraryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(script));

        HybridSearchResultVO result = service.hybridSearch(request, 100L);

        assertThat(result.getPageNum()).isEqualTo(1);
        assertThat(result.getPageSize()).isEqualTo(5);
        verify(vectorEmbeddingService, never()).generateEmbedding(any(String.class));
        verify(valueOperations).get(contains(":page=1:rows=5:topK=5:category=:style=:vw=0.0000:lw=1.0000"));
        verify(valueOperations).set(contains(":vw=0.0000:lw=1.0000"), any(HybridSearchResultVO.class), anyLong(), any());
        verify(searchResultRepository).save(any(SearchResult.class));
    }

    @Test
    void hybridSearch_semanticWeightSkipsLexicalRepository() {
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("测试查询");
        request.setRows(5);
        request.setTopK(5);
        request.setVectorWeight(BigDecimal.ONE);
        request.setLexicalWeight(BigDecimal.ZERO);

        ScriptVectorEmbedding embedding = new ScriptVectorEmbedding();
        embedding.setScriptId(1L);
        embedding.setOwnerId(100L);
        embedding.setDeleted(0);
        embedding.setVectorEmbedding(floatArrayToBytes(0.1f, 0.2f, 0.3f));

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(any(String.class))).thenReturn(null);
        when(vectorEmbeddingService.generateEmbedding("测试查询")).thenReturn(floatArrayToBytes(0.1f, 0.2f, 0.3f));
        when(scriptVectorEmbeddingRepository.findByOwnerIdAndDeletedOrderByCreatedAtDesc(100L, 0))
                .thenReturn(List.of(embedding));
        when(scriptLibraryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(script));

        HybridSearchResultVO result = service.hybridSearch(request, 100L);

        assertThat(result.getList()).hasSize(1);
        verify(scriptLibraryRepository).findAll(any(org.springframework.data.jpa.domain.Specification.class));
        verify(valueOperations).get(contains(":vw=1.0000:lw=0.0000"));
    }

    private byte[] floatArrayToBytes(float... floats) {
        byte[] bytes = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            int intBits = Float.floatToIntBits(floats[i]);
            bytes[i * 4] = (byte) (intBits & 0xFF);
            bytes[i * 4 + 1] = (byte) ((intBits >> 8) & 0xFF);
            bytes[i * 4 + 2] = (byte) ((intBits >> 16) & 0xFF);
            bytes[i * 4 + 3] = (byte) ((intBits >> 24) & 0xFF);
        }
        return bytes;
    }
}
