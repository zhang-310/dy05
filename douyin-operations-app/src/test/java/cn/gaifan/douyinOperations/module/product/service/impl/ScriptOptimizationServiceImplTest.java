package cn.gaifan.douyinOperations.module.product.service.impl;

import cn.gaifan.douyinOperations.module.product.entity.ScriptAnalysisResult;
import cn.gaifan.douyinOperations.module.product.entity.ScriptOptimizationSuggestion;
import cn.gaifan.douyinOperations.module.product.entity.ScriptRegeneratedVersion;
import cn.gaifan.douyinOperations.module.product.repository.ScriptAnalysisResultRepository;
import cn.gaifan.douyinOperations.module.product.repository.ScriptOptimizationSuggestionRepository;
import cn.gaifan.douyinOperations.module.product.repository.ScriptRegeneratedVersionRepository;
import cn.gaifan.douyinOperations.module.product.vo.OptimizationSuggestionVO;
import cn.gaifan.douyinOperations.module.product.vo.RegeneratedScriptVO;
import cn.gaifan.douyinOperations.module.product.vo.ScriptAnalysisResultVO;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 话术优化建议服务单元测试
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@ExtendWith(MockitoExtension.class)
@org.mockito.junit.jupiter.MockitoSettings(strictness = org.mockito.quality.Strictness.LENIENT)
@DisplayName("话术优化建议服务测试")
class ScriptOptimizationServiceImplTest {

    @Mock
    private ScriptAnalysisResultRepository analysisResultRepository;

    @Mock
    private ScriptOptimizationSuggestionRepository suggestionRepository;

    @Mock
    private ScriptRegeneratedVersionRepository regeneratedVersionRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ScriptOptimizationServiceImpl scriptOptimizationService;

    private static final Long TEST_USER_ID = 1L;
    private static final Long SCRIPT_VERSION_ID = 100L;
    private static final Long ANALYSIS_RESULT_ID = 200L;
    private static final Long SUGGESTION_ID = 300L;

    @BeforeEach
    void setUp() throws Exception {
        // 初始化 ObjectMapper
        ObjectMapper realObjectMapper = new ObjectMapper();
        when(objectMapper.writeValueAsString(any())).thenAnswer(invocation -> {
            try {
                return realObjectMapper.writeValueAsString(invocation.getArgument(0));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        when(objectMapper.readValue(any(String.class), eq(ScriptAnalysisResultVO.WeakPoint.class)))
                .thenAnswer(invocation -> {
                    try {
                        return realObjectMapper.readValue((String) invocation.getArgument(0),
                                ScriptAnalysisResultVO.WeakPoint.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    // ==================== 分析话术测试 ====================

    @Test
    @DisplayName("成功分析话术效果")
    void testAnalyzeScriptSuccess() {
        // Arrange
        ScriptAnalysisResult expectedResult = createTestAnalysisResult();
        when(analysisResultRepository.save(any(ScriptAnalysisResult.class))).thenReturn(expectedResult);

        // Act
        ScriptAnalysisResultVO result = scriptOptimizationService.analyzeScript(
                SCRIPT_VERSION_ID, "LIVE_MONITOR", "COMPREHENSIVE", TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(SCRIPT_VERSION_ID, result.getScriptVersionId());
        assertEquals(new BigDecimal("72.50"), result.getOverallScore());
        assertEquals("FRIENDLY", result.getStyleProfile().getDominantStyle());

        verify(analysisResultRepository, times(1)).save(any(ScriptAnalysisResult.class));
    }

    @Test
    @DisplayName("话术分析 - 脚本版本 ID 无效")
    void testAnalyzeScriptInvalidScriptVersionId() {
        // Assert
        assertThrows(BusinessException.class, () -> {
            scriptOptimizationService.analyzeScript(null, "LIVE_MONITOR", "COMPREHENSIVE", TEST_USER_ID);
        });
    }

    @Test
    @DisplayName("话术分析 - 用户 ID 无效")
    void testAnalyzeScriptInvalidUserId() {
        // Assert
        assertThrows(BusinessException.class, () -> {
            scriptOptimizationService.analyzeScript(SCRIPT_VERSION_ID, "LIVE_MONITOR", "COMPREHENSIVE", null);
        });
    }

    // ==================== 获取优化建议测试 ====================

    @Test
    @DisplayName("成功获取优化建议")
    void testGetOptimizationSuggestionsSuccess() {
        // Arrange
        ScriptAnalysisResult analysisResult = createTestAnalysisResult();
        List<ScriptOptimizationSuggestion> suggestions = createTestSuggestions();

        when(analysisResultRepository.findById(ANALYSIS_RESULT_ID))
                .thenReturn(Optional.of(analysisResult));
        when(suggestionRepository.findHighPrioritySuggestions(ANALYSIS_RESULT_ID, TEST_USER_ID))
                .thenReturn(suggestions);

        // Act
        List<OptimizationSuggestionVO> result = scriptOptimizationService.getOptimizationSuggestions(
                SCRIPT_VERSION_ID, ANALYSIS_RESULT_ID, 10, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("CONTENT", result.get(0).getCategory());

        verify(analysisResultRepository, times(1)).findById(ANALYSIS_RESULT_ID);
        verify(suggestionRepository, times(1)).findHighPrioritySuggestions(ANALYSIS_RESULT_ID, TEST_USER_ID);
    }

    @Test
    @DisplayName("获取优化建议 - 分析结果不存在")
    void testGetOptimizationSuggestionsNotFound() {
        // Arrange
        when(analysisResultRepository.findById(ANALYSIS_RESULT_ID))
                .thenReturn(Optional.empty());

        // Assert
        assertThrows(BusinessException.class, () -> {
            scriptOptimizationService.getOptimizationSuggestions(
                    SCRIPT_VERSION_ID, ANALYSIS_RESULT_ID, 10, TEST_USER_ID);
        });
    }

    @Test
    @DisplayName("获取优化建议 - 无权访问")
    void testGetOptimizationSuggestionsUnauthorized() {
        // Arrange
        ScriptAnalysisResult analysisResult = createTestAnalysisResult();
        analysisResult.setOwnerId(999L); // 不同的所有者

        when(analysisResultRepository.findById(ANALYSIS_RESULT_ID))
                .thenReturn(Optional.of(analysisResult));

        // Assert
        assertThrows(BusinessException.class, () -> {
            scriptOptimizationService.getOptimizationSuggestions(
                    SCRIPT_VERSION_ID, ANALYSIS_RESULT_ID, 10, TEST_USER_ID);
        });
    }

    // ==================== 重新生成话术测试 ====================

    @Test
    @DisplayName("成功重新生成话术")
    void testRegenerateScriptSuccess() {
        // Arrange
        ScriptOptimizationSuggestion suggestion = createTestSuggestion();
        ScriptRegeneratedVersion regenerated = createTestRegeneratedVersion();

        when(suggestionRepository.findById(SUGGESTION_ID))
                .thenReturn(Optional.of(suggestion));
        when(regeneratedVersionRepository.save(any(ScriptRegeneratedVersion.class)))
                .thenReturn(regenerated);

        // Act
        List<RegeneratedScriptVO> result = scriptOptimizationService.regenerateScript(
                SCRIPT_VERSION_ID, SUGGESTION_ID, List.of("FRIENDLY", "HUMOROUS"), TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertFalse(result.isEmpty());
        assertEquals("FRIENDLY", result.get(0).getGenerationStyle());

        verify(suggestionRepository, times(1)).findById(SUGGESTION_ID);
        verify(regeneratedVersionRepository, times(2)).save(any(ScriptRegeneratedVersion.class));
    }

    @Test
    @DisplayName("重新生成话术 - 建议不存在")
    void testRegenerateScriptNotFound() {
        // Arrange
        when(suggestionRepository.findById(SUGGESTION_ID))
                .thenReturn(Optional.empty());

        // Assert
        assertThrows(BusinessException.class, () -> {
            scriptOptimizationService.regenerateScript(
                    SCRIPT_VERSION_ID, SUGGESTION_ID, List.of("FRIENDLY"), TEST_USER_ID);
        });
    }

    @Test
    @DisplayName("重新生成话术 - 生成风格为空")
    void testRegenerateScriptEmptyStyles() {
        // Assert
        assertThrows(BusinessException.class, () -> {
            scriptOptimizationService.regenerateScript(
                    SCRIPT_VERSION_ID, SUGGESTION_ID, List.of(), TEST_USER_ID);
        });
    }

    // ==================== 查看优化历史测试 ====================

    @Test
    @DisplayName("成功查看优化历史")
    void testGetOptimizationHistorySuccess() {
        // Arrange
        List<ScriptAnalysisResult> analysisResults = List.of(
                createTestAnalysisResult(),
                createTestAnalysisResult()
        );
        Page<ScriptAnalysisResult> page = new PageImpl<>(analysisResults);

        when(analysisResultRepository.findByScriptVersionIdAndOwnerIdOrderByCreatedAtDesc(
                eq(SCRIPT_VERSION_ID), eq(TEST_USER_ID), any(Pageable.class)))
                .thenReturn(page);

        // Act
        var result = scriptOptimizationService.getOptimizationHistory(
                SCRIPT_VERSION_ID, 0, 30, TEST_USER_ID);

        // Assert
        assertNotNull(result);
        assertEquals(2, result.getList().size());
        assertEquals(0, result.getPageNum());

        verify(analysisResultRepository, times(1)).findByScriptVersionIdAndOwnerIdOrderByCreatedAtDesc(
                eq(SCRIPT_VERSION_ID), eq(TEST_USER_ID), any(Pageable.class));
    }

    // ==================== 采纳建议测试 ====================

    @Test
    @DisplayName("成功采纳建议")
    void testAcceptSuggestionSuccess() {
        // Arrange
        ScriptOptimizationSuggestion suggestion = createTestSuggestion();
        suggestion.setAdoptionStatus("PENDING");

        when(suggestionRepository.findById(SUGGESTION_ID))
                .thenReturn(Optional.of(suggestion));
        when(suggestionRepository.save(any(ScriptOptimizationSuggestion.class)))
                .thenReturn(suggestion);

        // Act
        Boolean result = scriptOptimizationService.acceptSuggestion(SUGGESTION_ID, TEST_USER_ID);

        // Assert
        assertTrue(result);
        verify(suggestionRepository, times(1)).save(any(ScriptOptimizationSuggestion.class));
    }

    @Test
    @DisplayName("采纳建议 - 建议不存在")
    void testAcceptSuggestionNotFound() {
        // Arrange
        when(suggestionRepository.findById(SUGGESTION_ID))
                .thenReturn(Optional.empty());

        // Assert
        assertThrows(BusinessException.class, () -> {
            scriptOptimizationService.acceptSuggestion(SUGGESTION_ID, TEST_USER_ID);
        });
    }

    // ==================== 拒绝建议测试 ====================

    @Test
    @DisplayName("成功拒绝建议")
    void testRejectSuggestionSuccess() {
        // Arrange
        ScriptOptimizationSuggestion suggestion = createTestSuggestion();

        when(suggestionRepository.findById(SUGGESTION_ID))
                .thenReturn(Optional.of(suggestion));
        when(suggestionRepository.save(any(ScriptOptimizationSuggestion.class)))
                .thenReturn(suggestion);

        // Act
        Boolean result = scriptOptimizationService.rejectSuggestion(SUGGESTION_ID, "不适用", TEST_USER_ID);

        // Assert
        assertTrue(result);
        verify(suggestionRepository, times(1)).save(any(ScriptOptimizationSuggestion.class));
    }

    // ==================== 应用生成版本测试 ====================

    @Test
    @DisplayName("成功应用生成版本")
    void testApplyRegeneratedVersionSuccess() {
        // Arrange
        ScriptRegeneratedVersion version = createTestRegeneratedVersion();
        version.setApprovalStatus("APPROVED");
        version.setIsApplied(false);

        when(regeneratedVersionRepository.findById(version.getId()))
                .thenReturn(Optional.of(version));
        when(regeneratedVersionRepository.save(any(ScriptRegeneratedVersion.class)))
                .thenReturn(version);

        // Act
        Boolean result = scriptOptimizationService.applyRegeneratedVersion(version.getId(), TEST_USER_ID);

        // Assert
        assertTrue(result);
        verify(regeneratedVersionRepository, times(1)).save(any(ScriptRegeneratedVersion.class));
    }

    // ==================== 审批生成版本测试 ====================

    @Test
    @DisplayName("成功审批通过生成版本")
    void testApproveRegeneratedVersionSuccess() {
        // Arrange
        ScriptRegeneratedVersion version = createTestRegeneratedVersion();
        version.setApprovalStatus("PENDING");

        when(regeneratedVersionRepository.findById(version.getId()))
                .thenReturn(Optional.of(version));
        when(regeneratedVersionRepository.save(any(ScriptRegeneratedVersion.class)))
                .thenReturn(version);

        // Act
        Boolean result = scriptOptimizationService.approveRegeneratedVersion(version.getId(), TEST_USER_ID, "质量不错");

        // Assert
        assertTrue(result);
        verify(regeneratedVersionRepository, times(1)).save(any(ScriptRegeneratedVersion.class));
    }

    @Test
    @DisplayName("成功拒绝生成版本")
    void testRejectRegeneratedVersionSuccess() {
        // Arrange
        ScriptRegeneratedVersion version = createTestRegeneratedVersion();

        when(regeneratedVersionRepository.findById(version.getId()))
                .thenReturn(Optional.of(version));
        when(regeneratedVersionRepository.save(any(ScriptRegeneratedVersion.class)))
                .thenReturn(version);

        // Act
        Boolean result = scriptOptimizationService.rejectRegeneratedVersion(version.getId(), TEST_USER_ID, "内容不合适");

        // Assert
        assertTrue(result);
        verify(regeneratedVersionRepository, times(1)).save(any(ScriptRegeneratedVersion.class));
    }

    // ==================== 辅助方法 ====================

    private ScriptAnalysisResult createTestAnalysisResult() {
        return ScriptAnalysisResult.builder()
                .id(ANALYSIS_RESULT_ID)
                .scriptVersionId(SCRIPT_VERSION_ID)
                .ownerId(TEST_USER_ID)
                .overallScore(new BigDecimal("72.50"))
                .interactionRate(new BigDecimal("18.50"))
                .conversionRate(new BigDecimal("12.30"))
                .fanGrowth(254)
                .commentSentiment(new BigDecimal("0.68"))
                .dominantStyle("FRIENDLY")
                .analysisType("COMPREHENSIVE")
                .dataSource("LIVE_MONITOR")
                .weakPoints("[]")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private ScriptOptimizationSuggestion createTestSuggestion() {
        return ScriptOptimizationSuggestion.builder()
                .id(SUGGESTION_ID)
                .scriptVersionId(SCRIPT_VERSION_ID)
                .analysisResultId(ANALYSIS_RESULT_ID)
                .ownerId(TEST_USER_ID)
                .category("CONTENT")
                .priority("HIGH")
                .suggestionContent("增加互动环节")
                .adoptionStatus("PENDING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(0)
                .build();
    }

    private List<ScriptOptimizationSuggestion> createTestSuggestions() {
        return List.of(
                createTestSuggestion(),
                ScriptOptimizationSuggestion.builder()
                        .id(SUGGESTION_ID + 1)
                        .scriptVersionId(SCRIPT_VERSION_ID)
                        .analysisResultId(ANALYSIS_RESULT_ID)
                        .ownerId(TEST_USER_ID)
                        .category("PACING")
                        .priority("MEDIUM")
                        .suggestionContent("优化节奏")
                        .adoptionStatus("PENDING")
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .deleted(0)
                        .build()
        );
    }

    private ScriptRegeneratedVersion createTestRegeneratedVersion() {
        return ScriptRegeneratedVersion.builder()
                .id(400L)
                .scriptVersionId(SCRIPT_VERSION_ID)
                .suggestionId(SUGGESTION_ID)
                .ownerId(TEST_USER_ID)
                .generationStyle("FRIENDLY")
                .regeneratedContent("优化后的话术内容")
                .aiQualityScore(new BigDecimal("8.2"))
                .approvalStatus("PENDING")
                .isApplied(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .deleted(0)
                .build();
    }
}
