package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.script.entity.*;
import cn.gaifan.douyinOperations.module.script.repository.*;
import cn.gaifan.douyinOperations.module.script.service.VectorEmbeddingService;
import cn.gaifan.douyinOperations.module.script.service.SearchSuggestionService;
import cn.gaifan.douyinOperations.module.script.vo.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.sql.Timestamp;
import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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

    private ScriptLibrary testScript;
    private ScriptVectorEmbedding testEmbedding;

    @BeforeEach
    void setUp() {
        testScript = new ScriptLibrary();
        testScript.setId(1L);
        testScript.setTitle("测试话术");
        testScript.setContent("测试话术内容");
        testScript.setCategory("开场");
        testScript.setUserId(100L);
        testScript.setDeleted(0);
        testScript.setCreateTime(new Timestamp(System.currentTimeMillis()));

        testEmbedding = new ScriptVectorEmbedding();
        testEmbedding.setId(1L);
        testEmbedding.setScriptId(1L);
        testEmbedding.setOwnerId(100L);
        testEmbedding.setVectorEmbedding(floatArrayToBytes(0.1f, 0.2f, 0.3f));
        testEmbedding.setDeleted(0);
    }

    @Test
    void testHybridSearch_WithValidQuery_ShouldReturnResults() {
        // Arrange
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("测试查询");
        request.setTopK(10);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(vectorEmbeddingService.generateEmbedding(anyString()))
            .thenReturn(floatArrayToBytes(0.1f, 0.2f, 0.3f));
        when(scriptVectorEmbeddingRepository.findByOwnerIdAndDeletedOrderByCreatedAtDesc(100L, 0))
            .thenReturn(List.of(testEmbedding));
        when(scriptLibraryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
            .thenReturn(List.of(testScript));

        // Act
        HybridSearchResultVO result = service.hybridSearch(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getList()).isNotEmpty();
        verify(searchResultRepository).save(any(SearchResult.class));
    }

    @Test
    void testHybridSearch_WithNullQuery_ShouldThrow() {
        // Arrange
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery(null);

        // Act & Assert
        assertThatThrownBy(() -> service.hybridSearch(request, 100L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);
    }

    @Test
    void testHybridSearch_WithEmptyQuery_ShouldThrow() {
        // Arrange
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("");

        // Act & Assert
        assertThatThrownBy(() -> service.hybridSearch(request, 100L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);
    }

    @Test
    void testHybridSearch_WithInvalidUserId_ShouldThrow() {
        // Arrange
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("测试");

        // Act & Assert
        assertThatThrownBy(() -> service.hybridSearch(request, null))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);

        assertThatThrownBy(() -> service.hybridSearch(request, 0L))
            .isInstanceOf(BusinessException.class)
            .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VALIDATION_FAIL);
    }

    @Test
    void testHybridSearch_WithCachedResult_ShouldReturnFromCache() {
        // Arrange
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("测试查询");

        HybridSearchResultVO cachedResult = new HybridSearchResultVO();
        cachedResult.setList(List.of());
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(cachedResult);

        // Act
        HybridSearchResultVO result = service.hybridSearch(request, 100L);

        // Assert
        assertThat(result).isEqualTo(cachedResult);
        verify(vectorEmbeddingService, never()).generateEmbedding(anyString());
    }

    @Test
    void testHybridSearch_WithKeywordWeight_ShouldSkipVectorAndUseWeightedCacheKey() {
        // Arrange
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("测试查询");
        request.setPage(1);
        request.setRows(5);
        request.setTopK(5);
        request.setVectorWeight(BigDecimal.ZERO);
        request.setLexicalWeight(BigDecimal.ONE);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);
        when(scriptLibraryRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
            .thenReturn(List.of(testScript));

        // Act
        HybridSearchResultVO result = service.hybridSearch(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        verify(vectorEmbeddingService, never()).generateEmbedding(anyString());
        verify(valueOperations).get(contains(":page=1:rows=5:topK=5:category=:style=:vw=0.0000:lw=1.0000"));
        verify(valueOperations).set(contains(":vw=0.0000:lw=1.0000"), any(HybridSearchResultVO.class), anyLong(), any());
    }

    @Test
    void testGetSearchSuggestions_ShouldReturnSuggestions() {
        // Arrange
        SearchSuggestionVO suggestionVO = new SearchSuggestionVO();
        suggestionVO.setSuggestions(List.of(SearchSuggestionVO.SuggestionItemVO.builder()
            .text("测试建议")
            .type("SYSTEM")
            .popularity(10)
            .resultCount(1)
            .build()));
        when(searchSuggestionService.getSuggestions(eq("测试"), eq(100L), eq(10)))
            .thenReturn(suggestionVO);

        // Act
        SearchSuggestionVO result = service.getSearchSuggestions("测试", 100L, 10);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getSuggestions()).isNotNull();
    }

    /**
     * Helper method to encode float array as byte array (4 bytes per float, IEEE 754)
     */
    private byte[] floatArrayToBytes(float... floats) {
        byte[] bytes = new byte[floats.length * 4];
        for (int i = 0; i < floats.length; i++) {
            int intBits = Float.floatToIntBits(floats[i]);
            bytes[i*4] = (byte) (intBits & 0xFF);
            bytes[i*4+1] = (byte) ((intBits >> 8) & 0xFF);
            bytes[i*4+2] = (byte) ((intBits >> 16) & 0xFF);
            bytes[i*4+3] = (byte) ((intBits >> 24) & 0xFF);
        }
        return bytes;
    }
}
