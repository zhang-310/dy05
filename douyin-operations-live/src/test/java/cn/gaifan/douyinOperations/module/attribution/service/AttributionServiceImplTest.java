package cn.gaifan.douyinOperations.module.attribution.service;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.entity.AiModel;
import cn.gaifan.douyinOperations.module.ai.repository.AiModelRepository;
import cn.gaifan.douyinOperations.module.ai.service.LlmClient;
import cn.gaifan.douyinOperations.module.attribution.entity.Attribution;
import cn.gaifan.douyinOperations.module.attribution.repository.AttributionRepository;
import cn.gaifan.douyinOperations.module.attribution.service.impl.AttributionServiceImpl;
import cn.gaifan.douyinOperations.module.attribution.vo.AttributionTriggerVO;
import cn.gaifan.douyinOperations.module.live.entity.LiveProduct;
import cn.gaifan.douyinOperations.module.live.entity.LiveScript;
import cn.gaifan.douyinOperations.module.live.entity.LiveSession;
import cn.gaifan.douyinOperations.module.live.repository.LiveProductRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveScriptRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttributionService 单元测试")
class AttributionServiceImplTest {

    @Mock
    private AttributionRepository attributionRepository;
    @Mock
    private LiveSessionRepository sessionRepository;
    @Mock
    private LiveProductRepository productRepository;
    @Mock
    private LiveScriptRepository scriptRepository;
    @Mock
    private AiModelRepository aiModelRepository;
    @Mock
    private LlmClient llmClient;

    @InjectMocks
    private AttributionServiceImpl attributionService;

    private Long ownerId = 1L;
    private Long sessionId = 1L;
    private LiveSession mockSession;
    private LiveProduct mockProduct;
    private LiveScript mockScript;
    private Attribution mockAttribution;
    private AiModel mockModel;

    @BeforeEach
    void setUp() {
        // Mock Session
        mockSession = new LiveSession();
        mockSession.setId(sessionId);
        mockSession.setUserId(ownerId);
        mockSession.setLiveTitle("护肤品直播");

        // Mock Product
        mockProduct = new LiveProduct();
        mockProduct.setId(1L);
        mockProduct.setSessionId(sessionId);
        mockProduct.setProductId(100L);
        mockProduct.setProductName("护肤精华液");
        mockProduct.setRevenue(new BigDecimal("2990.00"));
        mockProduct.setSaleQuantity(10);

        // Mock Script
        mockScript = new LiveScript();
        mockScript.setId(1L);
        mockScript.setSessionId(sessionId);
        mockScript.setScriptType("product");
        mockScript.setScriptContent("这款精华液非常好用");
        mockScript.setExecuted(1);
        mockScript.setAiGenerated(1);
        mockScript.setDeleted(0);

        // Mock Attribution
        mockAttribution = new Attribution();
        mockAttribution.setId(1L);
        mockAttribution.setSessionId(sessionId);
        mockAttribution.setOwnerId(ownerId);
        mockAttribution.setAttributionType("overall");
        mockAttribution.setStatus(1);
        mockAttribution.setContributedGmv(new BigDecimal("2990.00"));
        mockAttribution.setContributedSales(10);
        mockAttribution.setEffectScore(85);
        mockAttribution.setDeleted(0);

        // Mock AI Model
        mockModel = new AiModel();
        mockModel.setId(1L);
        mockModel.setModelName("gpt-4");
        mockModel.setModelVersion("gpt-4-turbo");
        mockModel.setStatus(1);
        mockModel.setDeleted(0);
    }

    @Test
    @DisplayName("触发归因分析 - 应创建归因记录")
    void triggerAttribution_shouldCreateAttribution() {
        // Given
        AttributionTriggerVO triggerVO = new AttributionTriggerVO();
        triggerVO.setSessionId(sessionId);

        when(sessionRepository.findById(sessionId))
                .thenReturn(Optional.of(mockSession));
        when(attributionRepository.save(any(Attribution.class)))
                .thenAnswer(invocation -> {
                    Attribution saved = invocation.getArgument(0);
                    saved.setId(1L);
                    return saved;
                });

        // When
        long result = attributionService.triggerAttribution(triggerVO, ownerId);

        // Then
        assertThat(result).isEqualTo(1L);
        verify(attributionRepository).save(argThat(attr ->
                attr.getSessionId().equals(sessionId) &&
                attr.getOwnerId().equals(ownerId) &&
                attr.getAttributionType().equals("overall") &&
                attr.getStatus() == 0
        ));
    }

    @Test
    @DisplayName("触发归因分析 - 场次不存在应抛出异常")
    void triggerAttribution_sessionNotExists_shouldThrowException() {
        // Given
        AttributionTriggerVO triggerVO = new AttributionTriggerVO();
        triggerVO.setSessionId(999L);

        when(sessionRepository.findById(999L))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> attributionService.triggerAttribution(triggerVO, ownerId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("直播场次不存在");
    }

    @Test
    @DisplayName("异步归因分析 - 应创建商品归因记录")
    void asyncAttribution_shouldCreateProductAttributions() {
        // Given
        when(productRepository.findBySessionId(sessionId))
                .thenReturn(List.of(mockProduct));
        when(scriptRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(mockScript));
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of(mockModel));

        LlmClient.LlmResponse mockResponse = new LlmClient.LlmResponse(
                "综合效果评分：85分。商品贡献最大的是护肤精华液。",
                100L,
                true,
                null
        );
        when(llmClient.chat(any(AiModel.class), anyString(), anyString()))
                .thenReturn(mockResponse);
        when(attributionRepository.findBySessionIdAndAttributionTypeAndDeleted(sessionId, "overall", 0))
                .thenReturn(List.of(mockAttribution));

        // When
        attributionService.asyncAttribution(sessionId, ownerId);

        // Then
        verify(attributionRepository, atLeast(2)).save(any(Attribution.class));
    }

    @Test
    @DisplayName("根据场次ID获取归因 - 应返回归因列表")
    void getBySessionId_shouldReturnList() {
        // Given
        when(attributionRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(mockAttribution));

        // When
        List<Map<String, Object>> result = attributionService.getBySessionId(sessionId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).get("sessionId")).isEqualTo(sessionId);
        assertThat(result.get(0).get("attributionType")).isEqualTo("overall");
    }

    @Test
    @DisplayName("根据ID获取归因 - 存在应返回Map")
    void getById_exists_shouldReturnMap() {
        // Given
        when(attributionRepository.findByIdAndDeleted(1L, 0))
                .thenReturn(Optional.of(mockAttribution));

        // When
        Map<String, Object> result = attributionService.getById(1L);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.get("id")).isEqualTo(1L);
        assertThat(result.get("sessionId")).isEqualTo(sessionId);
    }

    @Test
    @DisplayName("根据ID获取归因 - 不存在应抛出异常")
    void getById_notExists_shouldThrowException() {
        // Given
        when(attributionRepository.findByIdAndDeleted(999L, 0))
                .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> attributionService.getById(999L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("归因数据不存在");
    }

    @Test
    @DisplayName("获取归因摘要 - 应返回汇总数据")
    void getSummary_shouldReturnSummary() {
        // Given
        Attribution productAttr = new Attribution();
        productAttr.setAttributionType("product_gmv");
        productAttr.setContributedGmv(new BigDecimal("2990.00"));
        productAttr.setContributedSales(10);
        productAttr.setStatus(1);

        Attribution scriptAttr = new Attribution();
        scriptAttr.setAttributionType("script_sales");
        scriptAttr.setStatus(1);

        when(attributionRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(mockAttribution, productAttr, scriptAttr));

        // When
        Map<String, Object> result = attributionService.getSummary(sessionId);

        // Then
        assertThat(result.get("sessionId")).isEqualTo(sessionId);
        assertThat(result.get("totalGmv")).isEqualTo(new BigDecimal("2990.00"));
        assertThat(result.get("totalSales")).isEqualTo(10);
        assertThat(result.get("productAttributions")).isEqualTo(1L);
        assertThat(result.get("scriptAttributions")).isEqualTo(1L);
        assertThat(result.get("overallScore")).isEqualTo(85);
        assertThat(result.get("status")).isEqualTo("completed");
    }

    @Test
    @DisplayName("获取归因摘要 - 无数据应返回零值")
    void getSummary_noData_shouldReturnZeros() {
        // Given
        when(attributionRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of());

        // When
        Map<String, Object> result = attributionService.getSummary(sessionId);

        // Then
        assertThat(result.get("totalGmv")).isEqualTo(BigDecimal.ZERO);
        assertThat(result.get("totalSales")).isEqualTo(0);
        assertThat(result.get("productAttributions")).isEqualTo(0L);
        assertThat(result.get("scriptAttributions")).isEqualTo(0L);
    }

    @Test
    @DisplayName("删除场次归因 - 应标记所有记录为已删除")
    void deleteBySessionId_shouldMarkAllAsDeleted() {
        // Given
        Attribution attr1 = new Attribution();
        attr1.setId(1L);
        attr1.setDeleted(0);

        Attribution attr2 = new Attribution();
        attr2.setId(2L);
        attr2.setDeleted(0);

        when(attributionRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(attr1, attr2));

        // When
        attributionService.deleteBySessionId(sessionId);

        // Then
        verify(attributionRepository).saveAll(argThat(attrs -> {
            List<Attribution> list = (List<Attribution>) attrs;
            return list.size() == 2 &&
                   list.stream().allMatch(a -> a.getDeleted() == 1);
        }));
    }

    @Test
    @DisplayName("异步归因分析 - 无商品应不创建商品归因")
    void asyncAttribution_noProducts_shouldNotCreateProductAttributions() {
        // Given
        when(productRepository.findBySessionId(sessionId))
                .thenReturn(List.of());
        when(scriptRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of());
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of());

        // When
        attributionService.asyncAttribution(sessionId, ownerId);

        // Then
        // 只验证没有抛出异常，不会创建商品归因
        verify(attributionRepository, never()).save(argThat(attr ->
                "product_gmv".equals(attr.getAttributionType())
        ));
    }

    @Test
    @DisplayName("异步归因分析 - 无已执行话术应不创建话术归因")
    void asyncAttribution_noExecutedScripts_shouldNotCreateScriptAttributions() {
        // Given
        mockScript.setExecuted(0); // 未执行

        when(productRepository.findBySessionId(sessionId))
                .thenReturn(List.of());
        when(scriptRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(mockScript));
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of());

        // When
        attributionService.asyncAttribution(sessionId, ownerId);

        // Then
        verify(attributionRepository, never()).save(argThat(attr ->
                "script_sales".equals(attr.getAttributionType())
        ));
    }

    @Test
    @DisplayName("异步归因分析 - 无可用AI模型应跳过AI归因")
    void asyncAttribution_noAiModel_shouldSkipAiAttribution() {
        // Given
        when(productRepository.findBySessionId(sessionId))
                .thenReturn(List.of(mockProduct));
        when(scriptRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(mockScript));
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of());

        // When
        attributionService.asyncAttribution(sessionId, ownerId);

        // Then
        verify(llmClient, never()).chat(any(), anyString(), anyString());
    }

    @Test
    @DisplayName("异步归因分析 - AI调用失败应不影响其他归因")
    void asyncAttribution_aiCallFails_shouldNotAffectOtherAttributions() {
        // Given
        when(productRepository.findBySessionId(sessionId))
                .thenReturn(List.of(mockProduct));
        when(scriptRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(mockScript));
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of(mockModel));
        when(llmClient.chat(any(AiModel.class), anyString(), anyString()))
                .thenThrow(new RuntimeException("AI 调用失败"));

        // When & Then - 不应抛出异常
        assertThatCode(() -> attributionService.asyncAttribution(sessionId, ownerId))
                .doesNotThrowAnyException();

        // 商品归因应该已创建
        verify(attributionRepository, atLeastOnce()).save(argThat(attr ->
                "product_gmv".equals(attr.getAttributionType())
        ));
    }

    @Test
    @DisplayName("异步归因分析 - AI成功应更新overall归因")
    void asyncAttribution_aiSuccess_shouldUpdateOverallAttribution() {
        // Given
        when(productRepository.findBySessionId(sessionId))
                .thenReturn(List.of(mockProduct));
        when(scriptRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(mockScript));
        when(aiModelRepository.findByStatusAndDeleted(1, 0))
                .thenReturn(List.of(mockModel));

        LlmClient.LlmResponse mockResponse = new LlmClient.LlmResponse(
                "综合效果评分：85分。商品贡献最大的是护肤精华液。",
                100L,
                true,
                null
        );
        when(llmClient.chat(any(AiModel.class), anyString(), anyString()))
                .thenReturn(mockResponse);
        when(attributionRepository.findBySessionIdAndAttributionTypeAndDeleted(sessionId, "overall", 0))
                .thenReturn(List.of(mockAttribution));

        // When
        attributionService.asyncAttribution(sessionId, ownerId);

        // Then
        verify(attributionRepository).save(argThat(attr ->
                "overall".equals(attr.getAttributionType()) &&
                attr.getAnalysis() != null &&
                attr.getAnalysis().contains("85分")
        ));
        verify(aiModelRepository).incrementQuotaUsed(mockModel.getId(), 100L);
    }

    @Test
    @DisplayName("获取归因摘要 - 部分未完成应返回processing状态")
    void getSummary_partialComplete_shouldReturnProcessingStatus() {
        // Given
        Attribution attr1 = new Attribution();
        attr1.setAttributionType("product_gmv");
        attr1.setStatus(1);

        Attribution attr2 = new Attribution();
        attr2.setAttributionType("overall");
        attr2.setStatus(0); // 未完成

        when(attributionRepository.findBySessionIdAndDeleted(sessionId, 0))
                .thenReturn(List.of(attr1, attr2));

        // When
        Map<String, Object> result = attributionService.getSummary(sessionId);

        // Then
        assertThat(result.get("status")).isEqualTo("processing");
    }
}
