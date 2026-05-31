package cn.gaifan.douyinOperations.module.live.service.impl;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductStrategyRecommenderOfficialGateTest {

    @Mock
    private LiveProductRepository liveProductRepository;
    @Mock
    private LiveScriptRepository liveScriptRepository;
    @Mock
    private LlmClient llmClient;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @InjectMocks
    private ProductStrategyRecommenderImpl recommender;

    @Test
    @DisplayName("直播排品缺 douyin_weigui 引用时直接阻断")
    void recommendBatchOrder_shouldBlockWhenViolationReferenceMissing() {
        when(liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(18L))
                .thenReturn(List.of(product("爆品洁面", "hot", 1)));
        when(operationalStrategyKnowledgeService.buildLiveGenerationContext(any(), any(), any(), anyInt()))
                .thenReturn(contextWithRefs(List.of(
                        new OperationalStrategyKnowledgeService.OfficialReference(
                                "douyin", "official_learning", 1L, 11L, "官方学习", "排品规则", 0.92)
                )));

        assertThatThrownBy(() -> recommender.recommendBatchOrder(7L, 18L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("官方规则引用门禁未通过");

        verify(llmClient, never()).chatWithFallback(any(), any(), any());
    }

    @Test
    @DisplayName("直播排品命中 douyin 和 douyin_weigui 后才允许调用模型")
    void recommendBatchOrder_shouldCallLlmWhenOfficialReferencesSatisfied() {
        when(liveProductRepository.findBySessionIdOrderByPositionAscIdAsc(18L))
                .thenReturn(List.of(product("爆品洁面", "hot", 1)));
        when(operationalStrategyKnowledgeService.buildLiveGenerationContext(any(), any(), any(), anyInt()))
                .thenReturn(contextWithRefs(List.of(
                        new OperationalStrategyKnowledgeService.OfficialReference(
                                "douyin", "official_learning", 1L, 11L, "官方学习", "排品规则", 0.92),
                        new OperationalStrategyKnowledgeService.OfficialReference(
                                "douyin_weigui", "violation_rule", 2L, 22L, "违规规则", "宣传违规", 0.91)
                )));
        when(aiModelRepository.findByStatusAndDeleted(1, 0)).thenReturn(List.of(model()));
        when(llmClient.chatWithFallback(any(), any(), any()))
                .thenReturn(new LlmClient.LlmResponse("{\"recommendedOrder\":[]}", 88, true, null));

        Map<String, Object> result = recommender.recommendBatchOrder(7L, 18L);

        assertThat(result.get("status")).isEqualTo("success");
        assertThat(result.get("officialReferenceRequired")).isEqualTo(true);
        assertThat(result.get("officialReferenceSatisfied")).isEqualTo(true);
        assertThat(result.get("officialReferenceStatus")).isEqualTo("satisfied");
    }

    private LiveProduct product(String name, String type, int position) {
        LiveProduct product = new LiveProduct();
        product.setId((long) position);
        product.setSessionId(18L);
        product.setProductId(100L + position);
        product.setProductName(name);
        product.setProductType(type);
        product.setSaleQuantity(100);
        product.setRevenue(new BigDecimal("1999.00"));
        product.setPosition(position);
        return product;
    }

    private AiModel model() {
        AiModel model = new AiModel();
        model.setId(1L);
        model.setModelName("test-model");
        model.setModelProvider("openai");
        model.setModelVersion("gpt-test");
        model.setStatus(1);
        model.setDeleted(0);
        return model;
    }

    private OperationalStrategyKnowledgeService.PromptContext contextWithRefs(
            List<OperationalStrategyKnowledgeService.OfficialReference> refs) {
        return new OperationalStrategyKnowledgeService.PromptContext(
                "<douyin_ops_learning_context>官方规则</douyin_ops_learning_context>",
                List.of(),
                refs);
    }
}
