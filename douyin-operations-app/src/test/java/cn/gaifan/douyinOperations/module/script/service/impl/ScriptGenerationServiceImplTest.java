package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.entity.ScriptGeneration;
import cn.gaifan.douyinOperations.module.script.entity.ScriptVariant;
import cn.gaifan.douyinOperations.module.script.repository.ScriptGenerationRepository;
import cn.gaifan.douyinOperations.module.script.repository.ScriptVariantRepository;
import cn.gaifan.douyinOperations.module.script.service.AiService;
import cn.gaifan.douyinOperations.module.script.service.ScriptCacheService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptVariantVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScriptGenerationServiceImplTest {

    @Mock
    private ScriptGenerationRepository generationRepository;

    @Mock
    private ScriptVariantRepository variantRepository;

    @Mock
    private AiService aiService;

    @Mock
    private ScriptCacheService cacheService;

    @InjectMocks
    private ScriptGenerationServiceImpl service;

    private ScriptGenerationRequestVO testRequest;
    private ScriptGeneration testGeneration;
    private ScriptVariant testVariant;

    @BeforeEach
    void setUp() {
        testRequest = new ScriptGenerationRequestVO();
        testRequest.setProductName("测试产品");
        testRequest.setProductPrice(new BigDecimal("99.00"));
        testRequest.setKeyFeatures(List.of("特点1", "特点2"));
        testRequest.setDuration(30);
        testRequest.setStyle("热情");
        testRequest.setVariants(3);

        testGeneration = new ScriptGeneration();
        testGeneration.setId(1L);
        testGeneration.setOwnerId(100L);
        testGeneration.setProductName("测试产品");
        testGeneration.setVariantCount(3);

        testVariant = new ScriptVariant();
        testVariant.setId(1L);
        testVariant.setGenerationId(1L);
        testVariant.setVariantIndex(0);
        testVariant.setScriptContent("生成的话术内容");
        testVariant.setQualityScore(new BigDecimal("85.5"));
    }

    @Test
    void testGenerateScript_WithValidRequest_ShouldReturnResult() {
        // Arrange
        when(generationRepository.save(any(ScriptGeneration.class)))
            .thenReturn(testGeneration);
        when(aiService.generateScriptContent(anyString()))
            .thenReturn("生成的话术内容");
        when(variantRepository.save(any(ScriptVariant.class)))
            .thenReturn(testVariant);

        // Act
        ScriptGenerationVO result = service.generateScript(100L, testRequest);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getGenerationId()).isEqualTo(1L);
        assertThat(result.getVariants()).hasSize(3);
        verify(generationRepository).save(any(ScriptGeneration.class));
        verify(variantRepository, times(3)).save(any(ScriptVariant.class));
    }

    @Test
    void testGenerateScript_WithNullUserId_ShouldThrow() {
        // Act & Assert
        assertThatThrownBy(() -> service.generateScript(null, testRequest))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGenerateScript_WithNullRequest_ShouldThrow() {
        // Act & Assert
        assertThatThrownBy(() -> service.generateScript(100L, null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetGenerationById_WhenExists_ShouldReturn() {
        // Arrange
        when(generationRepository.findById(1L))
            .thenReturn(Optional.of(testGeneration));
        when(variantRepository.findByGenerationIdOrderByVariantIndexAsc(1L))
            .thenReturn(List.of(testVariant));

        // Act
        ScriptGenerationVO result = service.getGenerationById(1L, 100L);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getGenerationId()).isEqualTo(1L);
        assertThat(result.getVariants()).hasSize(1);
    }

    @Test
    void testGetGenerationById_WhenNotExists_ShouldThrow() {
        // Arrange
        when(generationRepository.findById(999L))
            .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.getGenerationById(999L, 100L))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testGetGenerationById_WithWrongOwner_ShouldThrow() {
        // Arrange
        when(generationRepository.findById(1L))
            .thenReturn(Optional.of(testGeneration));

        // Act & Assert
        assertThatThrownBy(() -> service.getGenerationById(1L, 999L))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void testListGenerations_ShouldReturnList() {
        // Arrange
        when(generationRepository.findByOwnerIdOrderByCreateTimeDesc(100L))
            .thenReturn(List.of(testGeneration));

        // Act
        List<ScriptGenerationVO> results = service.listGenerations(100L);

        // Assert
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getGenerationId()).isEqualTo(1L);
    }

    @Test
    void testDeleteGeneration_WhenExists_ShouldSuccess() {
        // Arrange
        when(generationRepository.findById(1L))
            .thenReturn(Optional.of(testGeneration));

        // Act
        service.deleteGeneration(1L, 100L);

        // Assert
        verify(generationRepository).delete(testGeneration);
        verify(variantRepository).deleteByGenerationId(1L);
    }

    @Test
    void testDeleteGeneration_WithWrongOwner_ShouldThrow() {
        // Arrange
        when(generationRepository.findById(1L))
            .thenReturn(Optional.of(testGeneration));

        // Act & Assert
        assertThatThrownBy(() -> service.deleteGeneration(1L, 999L))
            .isInstanceOf(SecurityException.class);
    }

    @Test
    void testRateVariant_ShouldUpdateScore() {
        // Arrange
        when(variantRepository.findById(1L))
            .thenReturn(Optional.of(testVariant));

        // Act
        service.rateVariant(1L, new BigDecimal("90.0"), 100L);

        // Assert
        verify(variantRepository).save(argThat(variant ->
            variant.getQualityScore().compareTo(new BigDecimal("90.0")) == 0
        ));
    }

    @Test
    void testBuildPrompt_ShouldIncludeAllFeatures() {
        // Act
        String prompt = service.buildPrompt(testRequest);

        // Assert
        assertThat(prompt).contains("测试产品");
        assertThat(prompt).contains("99.00");
        assertThat(prompt).contains("特点1");
        assertThat(prompt).contains("特点2");
        assertThat(prompt).contains("30");
        assertThat(prompt).contains("热情");
    }

    @Test
    void testCalculateQualityScore_ShouldReturnValidScore() {
        // Arrange
        String content = "这是一段优质的话术内容，包含产品特点和价格信息。";

        // Act
        BigDecimal score = service.calculateQualityScore(content);

        // Assert
        assertThat(score).isBetween(new BigDecimal("0"), new BigDecimal("100"));
    }
}
