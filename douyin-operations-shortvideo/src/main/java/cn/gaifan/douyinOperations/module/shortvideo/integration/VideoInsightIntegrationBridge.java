package cn.gaifan.douyinOperations.module.shortvideo.integration;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationRequest;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationResult;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import cn.gaifan.douyinOperations.module.platform.product.ProductIntegrationService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoDeepAnalysisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

/**
 * shortvideo-maker → video-insight 互调：先 {@link ProductIntegrationService#invoke} 记账，再深度拆解（跳过二次扣费）。
 */
@Service
public class VideoInsightIntegrationBridge {

    private static final String TARGET_FEATURE = "video.analyze.standard";

    @Autowired(required = false)
    private ProductIntegrationService productIntegrationService;

    @Autowired(required = false)
    private ViralVideoDeepAnalysisService viralVideoDeepAnalysisService;

    public Map<String, Object> requestDeepAnalyzeFromShortvideo(
            Long viralVideoId,
            Long userId,
            String sourceFeatureCode,
            String traceId
    ) {
        if (viralVideoDeepAnalysisService == null) {
            throw new BusinessException(ErrorCode.SYSTEM_BUSY, "短视频洞察服务不可用");
        }
        if (productIntegrationService != null) {
            IdentityContext ctx = RequestIdentityHolder.current();
            String tenantId = CommercialIdentityBridge.resolveTenantId(ctx);
            if (tenantId == null || tenantId.isBlank() || "default".equals(tenantId)) {
                tenantId = "demo-tenant";
            }
            String resolvedTrace = traceId != null && !traceId.isBlank()
                    ? traceId
                    : CommercialIdentityBridge.resolveTraceId(ctx);
            String sourceFeature = sourceFeatureCode != null && !sourceFeatureCode.isBlank()
                    ? sourceFeatureCode
                    : FeatureCode.SHORTVIDEO_SCRIPT_GENERATE;
            ProductIntegrationInvocationResult gate = productIntegrationService.invoke(
                    new ProductIntegrationInvocationRequest(
                            tenantId,
                            userId != null ? String.valueOf(userId) : "shortvideo-user",
                            null,
                            ProductCode.SHORTVIDEO_MAKER,
                            sourceFeature,
                            ProductCode.VIDEO_INSIGHT,
                            TARGET_FEATURE,
                            ctx != null && ctx.channel() != null ? ctx.channel() : "WEB",
                            BigDecimal.ONE,
                            null,
                            resolvedTrace,
                            "shortvideo 触发爆款拆解 viralVideoId=" + viralVideoId,
                            false,
                            null,
                            null,
                            String.valueOf(viralVideoId),
                            null,
                            false
                    ));
            if (!gate.allowed()) {
                if ("CREDIT_DENIED".equals(gate.status())) {
                    throw new BusinessException(ErrorCode.INSUFFICIENT_CREDITS,
                            gate.message() != null ? gate.message() : "积分不足");
                }
                throw new BusinessException(ErrorCode.FORBIDDEN,
                        gate.message() != null ? gate.message() : "产品互调被拒绝");
            }
            return viralVideoDeepAnalysisService.startDeepAnalyze(viralVideoId, userId, true);
        }
        return viralVideoDeepAnalysisService.startDeepAnalyze(viralVideoId, userId, false);
    }
}
