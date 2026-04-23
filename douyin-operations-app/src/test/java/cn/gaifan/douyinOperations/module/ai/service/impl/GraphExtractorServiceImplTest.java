package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.repository.AiGraphEdgeRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiGraphNodeRepository;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * GraphExtractorServiceImpl 单元测试
 * extractFromDocument 在 llmClient 为 null 或 content 过短时提前返回
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GraphExtractorServiceImpl 单元测试")
class GraphExtractorServiceImplTest {

    @Mock
    private AiGraphNodeRepository nodeRepository;
    @Mock
    private AiGraphEdgeRepository edgeRepository;
    @Mock
    private AiModelRepository modelRepository;

    @InjectMocks
    private GraphExtractorServiceImpl graphExtractorService;

    @Test
    @DisplayName("extractFromDocument_nullContent_shouldReturnEarly")
    void extractFromDocument_nullContent_shouldReturnEarly() {
        graphExtractorService.extractFromDocument(1L, null, 1L);
        verify(nodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("extractFromDocument_shortContent_shouldReturnEarly")
    void extractFromDocument_shortContent_shouldReturnEarly() {
        graphExtractorService.extractFromDocument(1L, "短文本", 1L);
        verify(nodeRepository, never()).save(any());
    }
}
