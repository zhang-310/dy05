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
        testVariant.setContent("生成的话术内容");
        testVariant.setScore(new BigDecimal("85.5"));
    }

    @Test
    void testGenerateScript_WithValidRequest_ShouldReturnResult() {
        // Arrange
        when(generationRepository.save(any(ScriptGeneration.class)))
            .thenReturn(testGeneration);
        when(aiService.generateScript(anyString()))
            .thenReturn("生成的话术内容");
        when(aiService.scoreScript(anyString()))
            .thenReturn(8.5);
        when(variantRepository.save(any(ScriptVariant.class)))
            .thenReturn(testVariant);

        // Act
        ScriptGenerationVO result = service.generateScript(100L, testRequest);

        // Assert
        assertThat(result).isNotNull();
        verify(generationRepository).save(any(ScriptGeneration.class));
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
}
