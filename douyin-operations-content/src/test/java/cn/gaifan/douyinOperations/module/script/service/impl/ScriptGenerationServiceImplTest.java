package cn.gaifan.douyinOperations.module.script.service.impl;

import cn.gaifan.douyinOperations.module.script.entity.ScriptGeneration;
import cn.gaifan.douyinOperations.module.script.entity.ScriptVariant;
import cn.gaifan.douyinOperations.module.script.repository.ScriptGenerationRepository;
import cn.gaifan.douyinOperations.module.script.repository.ScriptVariantRepository;
import cn.gaifan.douyinOperations.module.script.service.AiService;
import cn.gaifan.douyinOperations.module.script.service.ScriptCacheService;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptGenerationVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptOptimizationRequestVO;
import cn.gaifan.douyinOperations.module.script.vo.ScriptOptimizationVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

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

    private ScriptGenerationRequestVO generationRequest;

    @BeforeEach
    void setUp() {
        generationRequest = new ScriptGenerationRequestVO();
        generationRequest.setProductName("测试产品");
        generationRequest.setProductPrice(new BigDecimal("99.00"));
        generationRequest.setKeyFeatures(List.of("特点1", "特点2"));
        generationRequest.setDuration(30);
        generationRequest.setStyle("热情");
        generationRequest.setVariants(2);
    }

    @Test
    void generateScriptPersistsGenerationAndVariants() {
        ScriptGeneration generation = new ScriptGeneration();
        generation.setId(1L);
        generation.setOwnerId(100L);
        generation.setProductName("测试产品");
        generation.setVariantCount(2);

        ScriptVariant variant = new ScriptVariant();
        variant.setId(1L);
        variant.setGenerationId(1L);
        variant.setVariantIndex(1);
        variant.setContent("生成的话术内容");
        variant.setScore(new BigDecimal("8.5"));

        when(generationRepository.save(any(ScriptGeneration.class))).thenReturn(generation);
        when(cacheService.getScript(anyString())).thenReturn(Optional.empty());
        when(aiService.generateScript(anyString())).thenReturn("生成的话术内容");
        when(aiService.scoreScript(anyString())).thenReturn(8.5);
        when(variantRepository.save(any(ScriptVariant.class))).thenReturn(variant);

        ScriptGenerationVO result = service.generateScript(100L, generationRequest);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getVariants()).hasSize(2);
        assertThat(result.getVariants().get(0).getContent()).isEqualTo("生成的话术内容");
        verify(generationRepository).save(any(ScriptGeneration.class));
        verify(variantRepository, times(2)).save(any(ScriptVariant.class));
    }

    @Test
    void optimizeScriptUsesAiOptimizationWithoutPersistingGenerationRows() {
        ScriptOptimizationRequestVO request = new ScriptOptimizationRequestVO();
        request.setOriginalContent("这款面膜很温和，适合敏感肌。");
        request.setGoal("强化行动号召");
        request.setStyle("urgent");

        when(aiService.scoreScript("这款面膜很温和，适合敏感肌。")).thenReturn(6.2);
        when(aiService.optimizeScript("这款面膜很温和，适合敏感肌。", "urgent"))
                .thenReturn("这款面膜很温和，适合敏感肌，今天点击领取更划算。");
        when(aiService.scoreScript("这款面膜很温和，适合敏感肌，今天点击领取更划算。")).thenReturn(8.8);

        ScriptOptimizationVO result = service.optimizeScript(100L, request);

        assertThat(result.getOptimizedContent()).contains("点击领取");
        assertThat(result.getOriginalScore()).isEqualByComparingTo("6.2");
        assertThat(result.getOptimizedScore()).isEqualByComparingTo("8.8");
        assertThat(result.getSuggestions()).anyMatch(item -> item.contains("强化行动号召"));
        assertThat(result.getStyle()).isEqualTo("urgent");
    }

    @Test
    void optimizeScriptFallsBackToGoalAsStyle() {
        ScriptOptimizationRequestVO request = new ScriptOptimizationRequestVO();
        request.setOriginalContent("这款面膜很温和，适合敏感肌。");
        request.setGoal("更亲和");

        when(aiService.scoreScript(anyString())).thenReturn(7.0);
        when(aiService.optimizeScript("这款面膜很温和，适合敏感肌。", "更亲和"))
                .thenReturn("亲和优化话术");

        ScriptOptimizationVO result = service.optimizeScript(100L, request);

        assertThat(result.getStyle()).isEqualTo("更亲和");
        verify(aiService).optimizeScript("这款面膜很温和，适合敏感肌。", "更亲和");
    }

    @Test
    void optimizeScriptRejectsInvalidInputs() {
        ScriptOptimizationRequestVO request = new ScriptOptimizationRequestVO();
        request.setOriginalContent("   ");

        assertThatThrownBy(() -> service.optimizeScript(null, request))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.optimizeScript(100L, request))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.optimizeScript(100L, null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
