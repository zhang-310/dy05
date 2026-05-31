package cn.gaifan.douyinOperations.module.shortvideo.integration;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationRequest;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationResult;
import cn.gaifan.douyinOperations.module.platform.product.ProductIntegrationService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoDeepAnalysisService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VideoInsightIntegrationBridgeTest {

    @Mock private ProductIntegrationService productIntegrationService;
    @Mock private ViralVideoDeepAnalysisService viralVideoDeepAnalysisService;

    @InjectMocks
    private VideoInsightIntegrationBridge bridge;

    @Test
    void invokeThenSkipCharge() {
        when(productIntegrationService.invoke(any(ProductIntegrationInvocationRequest.class)))
                .thenReturn(new ProductIntegrationInvocationResult(
                        "inv-1", true, "OK", "ok", null, null, null, null, null,
                        null, null, null, null, null, null, "trace-1"));
        when(viralVideoDeepAnalysisService.startDeepAnalyze(1L, 2L, true))
                .thenReturn(Map.of("status", "processing"));

        Map<String, Object> result = bridge.requestDeepAnalyzeFromShortvideo(1L, 2L, null, "trace-1");
        assertEquals("processing", result.get("status"));
        verify(viralVideoDeepAnalysisService).startDeepAnalyze(1L, 2L, true);
        verify(productIntegrationService).invoke(argThat(req ->
                ProductCode.SHORTVIDEO_MAKER.equals(req.sourceProductCode())
                        && ProductCode.VIDEO_INSIGHT.equals(req.targetProductCode())
                        && "video.analyze.standard".equals(req.targetFeatureCode())));
    }

    @Test
    void creditDeniedMapsToInsufficientCredits() {
        when(productIntegrationService.invoke(any()))
                .thenReturn(new ProductIntegrationInvocationResult(
                        "inv-2", false, "CREDIT_DENIED", "积分不足", null, null, null, null, null,
                        null, null, null, null, null, null, "trace-2"));
        BusinessException ex = assertThrows(BusinessException.class,
                () -> bridge.requestDeepAnalyzeFromShortvideo(1L, 2L, null, "trace-2"));
        assertEquals(ErrorCode.INSUFFICIENT_CREDITS, ex.getCode());
        verify(viralVideoDeepAnalysisService, never()).startDeepAnalyze(anyLong(), anyLong(), anyBoolean());
    }
}
