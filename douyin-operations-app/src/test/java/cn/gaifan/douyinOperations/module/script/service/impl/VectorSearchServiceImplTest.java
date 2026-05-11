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

import java.util.List;
import java.util.Optional;

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
        testScript.setScriptContent("测试话术内容");
        testScript.setScriptType("opening");
        testScript.setUserId(100L);
        testScript.setDeleted(0);

        testEmbedding = new ScriptVectorEmbedding();
        testEmbedding.setId(1L);
        testEmbedding.setScriptId(1L);
        testEmbedding.setEmbeddingVector("[0.1, 0.2, 0.3]");
        testEmbedding.setDeleted(0);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void testHybridSearch_WithValidQuery_ShouldReturnResults() {
        // Arrange
        HybridSearchRequestVO request = new HybridSearchRequestVO();
        request.setQuery("测试查询");
        request.setTopK(10);

        when(valueOperations.get(anyString())).thenReturn(null);
        when(vectorEmbeddingService.generateEmbedding(anyString()))
            .thenReturn(new double[]{0.1, 0.2, 0.3});
        when(scriptVectorEmbeddingRepository.findAll())
            .thenReturn(List.of(testEmbedding));
        when(scriptLibraryRepository.findById(1L))
            .thenReturn(Optional.of(testScript));

        // Act
        HybridSearchResultVO result = service.hybridSearch(request, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getResults()).isNotEmpty();
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
        cachedResult.setResults(List.of());
        when(valueOperations.get(anyString())).thenReturn(cachedResult);

        // Act
        HybridSearchResultVO result = service.hybridSearch(request, 100L);

        // Assert
        assertThat(result).isEqualTo(cachedResult);
        verify(vectorEmbeddingService, never()).generateEmbedding(anyString());
    }

    @Test
    void testVectorSearch_WithValidQuery_ShouldReturnResults() {
        // Arrange
        when(vectorEmbeddingService.generateEmbedding(anyString()))
            .thenReturn(new double[]{0.1, 0.2, 0.3});
        when(scriptVectorEmbeddingRepository.findAll())
            .thenReturn(List.of(testEmbedding));
        when(scriptLibraryRepository.findById(1L))
            .thenReturn(Optional.of(testScript));

        // Act
        List<ScriptSearchResultVO> results = service.vectorSearch("测试查询", 10, 100L);

        // Assert
        assertThat(results).isNotEmpty();
        assertThat(results.get(0).getScriptId()).isEqualTo(1L);
    }

    @Test
    void testCalculateCosineSimilarity_WithValidVectors_ShouldReturnScore() {
        // Arrange
        double[] vector1 = {1.0, 0.0, 0.0};
        double[] vector2 = {1.0, 0.0, 0.0};

        // Act
        double similarity = service.calculateCosineSimilarity(vector1, vector2);

        // Assert
        assertThat(similarity).isCloseTo(1.0, within(0.001));
    }

    @Test
    void testCalculateCosineSimilarity_WithOrthogonalVectors_ShouldReturnZero() {
        // Arrange
        double[] vector1 = {1.0, 0.0, 0.0};
        double[] vector2 = {0.0, 1.0, 0.0};

        // Act
        double similarity = service.calculateCosineSimilarity(vector1, vector2);

        // Assert
        assertThat(similarity).isCloseTo(0.0, within(0.001));
    }

    @Test
    void testCalculateCosineSimilarity_WithDifferentLengths_ShouldThrow() {
        // Arrange
        double[] vector1 = {1.0, 0.0};
        double[] vector2 = {1.0, 0.0, 0.0};

        // Act & Assert
        assertThatThrownBy(() -> service.calculateCosineSimilarity(vector1, vector2))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testRecordSearchAnalytics_ShouldSaveAnalytics() {
        // Arrange
        String query = "测试查询";
        int resultCount = 5;

        // Act
        service.recordSearchAnalytics(query, 100L, resultCount);

        // Assert
        verify(searchAnalyticsRepository).save(any(SearchAnalytics.class));
    }

    @Test
    void testGetSearchSuggestions_ShouldReturnSuggestions() {
        // Arrange
        SearchSuggestion suggestion = new SearchSuggestion();
        suggestion.setId(1L);
        suggestion.setSuggestionText("测试建议");
        when(searchSuggestionRepository.findByQueryPrefixAndDeleted(anyString(), eq(0)))
            .thenReturn(List.of(suggestion));

        // Act
        List<String> suggestions = service.getSearchSuggestions("测试", 100L);

        // Assert
        assertThat(suggestions).contains("测试建议");
    }
}
