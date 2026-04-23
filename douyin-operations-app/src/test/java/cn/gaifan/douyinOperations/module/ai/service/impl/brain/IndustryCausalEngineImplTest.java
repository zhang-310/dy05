package cn.gaifan.douyinOperations.module.ai.service.impl.brain;

import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.brain.IndustryCausalEngine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("IndustryCausalEngineImpl 因果推理引擎测试")
class IndustryCausalEngineImplTest {

    private IndustryCausalEngineImpl causalEngine;

    @Mock
    private LlmClient llmClient;

    @Mock
    private AiModelRepository aiModelRepository;

    @BeforeEach
    void setUp() {
        causalEngine = new IndustryCausalEngineImpl();
        ReflectionTestUtils.setField(causalEngine, "enabled", true);
        ReflectionTestUtils.setField(causalEngine, "llmClient", llmClient);
        ReflectionTestUtils.setField(causalEngine, "aiModelRepository", aiModelRepository);
    }

    @Nested
    @DisplayName("isAvailable")
    class IsAvailableTests {

        @Test
        void isAvailable_enabled_shouldReturnTrue() {
            assertThat(causalEngine.isAvailable()).isTrue();
        }

        @Test
        void isAvailable_disabled_shouldReturnFalse() {
            ReflectionTestUtils.setField(causalEngine, "enabled", false);
            assertThat(causalEngine.isAvailable()).isFalse();
        }
    }

    @Nested
    @DisplayName("infer - 禁用时")
    class InferWhenDisabledTests {

        @Test
        void infer_disabled_shouldReturnEmptyResult() {
            ReflectionTestUtils.setField(causalEngine, "enabled", false);
            var result = causalEngine.infer(Map.of("scriptType", "种草"));
            assertThat(result.expectedConversionRate()).isZero();
            assertThat(result.explanation()).contains("未启用");
        }
    }

    @Nested
    @DisplayName("infer - 贝叶斯纯推理")
    class InferBayesOnlyTests {

        @Test
        void infer_noLlm_noModel_shouldReturnBayesResult() {
            when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of());
            var result = causalEngine.infer(Map.of(
                    "scriptType", "种草",
                    "persona", "专业达人",
                    "productType", "护肤",
                    "timeSlot", "晚间"
            ));
            assertThat(result.expectedConversionRate()).isBetween(0.1, 0.95);
            assertThat(result.explanation()).contains("贝叶斯");
        }

        @Test
        void infer_种草安利_shouldBoostRate() {
            when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of());
            var result = causalEngine.infer(Map.of("scriptType", "种草"));
            assertThat(result.expectedConversionRate()).isGreaterThanOrEqualTo(0.35);
        }

        @Test
        void infer_emptyInput_shouldStillReturnValidResult() {
            when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of());
            var result = causalEngine.infer(Map.of());
            assertThat(result.expectedConversionRate()).isBetween(0.1, 0.92);
        }
    }

    @Nested
    @DisplayName("infer - LLM 融合")
    class InferWithLlmTests {

        @Test
        void infer_llmSuccess_shouldBlendBayesAndLlm() {
            AiModel model = new AiModel();
            model.setId(1L);
            model.setModelProvider("openai");
            model.setModelVersion("claude-3-5-sonnet");
            model.setMaxTokens(2048);
            model.setTemperature(BigDecimal.valueOf(0.7));

            when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
            when(llmClient.chat(any(), eq("你是抖音运营因果分析专家。"), any()))
                    .thenReturn(new LlmClient.LlmResponse("{\"expectedRate\":0.65,\"factors\":[\"人设匹配\"],\"risks\":[],\"explanation\":\"LLM分析\"}", 0, true, null));

            var result = causalEngine.infer(Map.of("scriptType", "种草", "persona", "达人"));
            assertThat(result.expectedConversionRate()).isBetween(0.1, 0.95);
            assertThat(result.explanation()).isNotBlank();
        }

        @Test
        void infer_llmFails_shouldFallbackToBayes() {
            AiModel model = new AiModel();
            model.setId(1L);
            model.setModelProvider("openai");
            model.setModelVersion("claude");
            model.setMaxTokens(2048);
            model.setTemperature(BigDecimal.valueOf(0.7));

            when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model));
            when(llmClient.chat(any(), any(), any())).thenThrow(new RuntimeException("LLM 超时"));

            var result = causalEngine.infer(Map.of("scriptType", "促销"));
            assertThat(result.expectedConversionRate()).isGreaterThan(0);
            assertThat(result.explanation()).contains("贝叶斯");
        }
    }

    @Nested
    @DisplayName("explainStrategy")
    class ExplainStrategyTests {

        @Test
        void explainStrategy_enabled_shouldReturnExplanation() {
            var result = causalEngine.explainStrategy("s1", Map.of());
            assertThat(result).contains("策略");
        }

        @Test
        void explainStrategy_disabled_shouldReturnNotEnabled() {
            ReflectionTestUtils.setField(causalEngine, "enabled", false);
            assertThat(causalEngine.explainStrategy("s1", Map.of())).contains("未启用");
        }
    }
}
