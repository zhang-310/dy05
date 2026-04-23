package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.service.RerankerService;
import cn.gaifan.douyinOperations.module.config.service.ConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("RerankerServiceImpl 重排序服务测试")
class RerankerServiceImplTest {

    private RerankerServiceImpl rerankerService;

    @Mock
    private ConfigService configService;

    @BeforeEach
    void setUp() {
        rerankerService = new RerankerServiceImpl();
        ReflectionTestUtils.setField(rerankerService, "configService", configService);
        ReflectionTestUtils.setField(rerankerService, "rerankerEnabled", true);
        ReflectionTestUtils.setField(rerankerService, "rerankerProvider", "ollama");
        ReflectionTestUtils.setField(rerankerService, "rerankerModelDefault", "qwen3-embedding:4b");
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTests {

        @Test
        void isAvailable_disabled_shouldReturnFalse() {
            ReflectionTestUtils.setField(rerankerService, "rerankerEnabled", false);
            assertThat(rerankerService.isAvailable()).isFalse();
        }

        @Test
        void isAvailable_enabled_shouldReturnTrue() {
            assertThat(rerankerService.isAvailable()).isTrue();
        }
    }

    @Nested
    @DisplayName("rerank")
    class RerankTests {

        @Test
        void rerank_disabled_shouldReturnNull() {
            ReflectionTestUtils.setField(rerankerService, "rerankerEnabled", false);
            List<Float> result = rerankerService.rerank("query", List.of("doc1"));
            assertThat(result).isNull();
        }

        @Test
        void rerank_nullQuery_shouldReturnNull() {
            List<Float> result = rerankerService.rerank(null, List.of("doc1"));
            assertThat(result).isNull();
        }

        @Test
        void rerank_nullCandidates_shouldReturnNull() {
            List<Float> result = rerankerService.rerank("query", null);
            assertThat(result).isNull();
        }

        @Test
        void rerank_emptyCandidates_shouldReturnNull() {
            List<Float> result = rerankerService.rerank("query", List.of());
            assertThat(result).isNull();
        }

    }
}
