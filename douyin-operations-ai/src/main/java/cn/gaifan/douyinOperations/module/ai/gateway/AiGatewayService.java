package cn.gaifan.douyinOperations.module.ai.gateway;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.ai.AiCallRequest;
import cn.gaifan.douyinOperations.contract.ai.AiCallResponse;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.ai.config.AiGatewayProperties;
import cn.gaifan.douyinOperations.module.ai.provider.AiProvider;
import cn.gaifan.douyinOperations.module.ai.provider.AiProviderResult;
import cn.gaifan.douyinOperations.module.ai.provider.AiProviderRouter;
import cn.gaifan.douyinOperations.module.platform.ai.AiInvocationLedgerService;
import cn.gaifan.douyinOperations.module.platform.ai.AiInvocationRecord;
import cn.gaifan.douyinOperations.module.platform.ai.AiUsageStatsService;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialCreditHelper;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * AI 网关服务：PlatformEntitlement 授权 → 扣费 → Provider → gf_ai_invocation / gf_usage_ledger。
 */
@Service
public class AiGatewayService {

    private static final Logger log = LoggerFactory.getLogger(AiGatewayService.class);

    @Resource
    private PlatformEntitlementService platformEntitlementService;

    @Resource
    private AiProviderRouter aiProviderRouter;

    @Resource
    private AiGatewayProperties aiGatewayProperties;

    @Autowired(required = false)
    private CommercialCreditHelper commercialCreditHelper;

    @Resource
    private AiInvocationLedgerService aiInvocationLedgerService;

    @Autowired(required = false)
    private AiUsageStatsService aiUsageStatsService;

    @Resource
    private AiCommercialFacade aiCommercialFacade;

    public AiResult call(String featureCode, String prompt) {
        return toAiResult(callContract(new AiCallRequest(featureCode, prompt, null, null, null, null)));
    }

    public AiCallResponse callContract(AiCallRequest request) {
        String featureCode = request.featureCode();
        String prompt = request.prompt();
        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = request.tenantId() != null && !request.tenantId().isBlank()
                ? request.tenantId()
                : resolveTenantId(ctx);
        String userId = request.userId() != null && !request.userId().isBlank()
                ? request.userId()
                : resolveUserId(ctx);
        String channel = request.channel() != null && !request.channel().isBlank()
                ? request.channel()
                : (ctx != null && ctx.channel() != null ? ctx.channel() : "AI");
        String traceId = request.traceId() != null && !request.traceId().isBlank()
                ? request.traceId()
                : CommercialIdentityBridge.resolveTraceId(ctx);
        String productCode = resolveProduct(featureCode);
        String requestId = aiInvocationLedgerService.newRequestId();

        CommercialEntitlementDecision decision = platformEntitlementService.check(new EntitlementCheckRequest(
                tenantId,
                userId,
                productCode,
                featureCode,
                channel,
                BigDecimal.ONE,
                null,
                traceId
        ));
        if (!decision.allowed()) {
            log.warn("AI 调用被拒绝: tenant={}, feature={}, reason={}", tenantId, featureCode, decision.reason());
            recordLedger(requestId, tenantId, userId, productCode, featureCode, channel, traceId,
                    "mock", "mock", featureCode, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
            return AiCallResponse.denied(decision.reason(), requestId, traceId);
        }

        try {
            if (commercialCreditHelper != null) {
                commercialCreditHelper.consume(new CreditConsumeRequest(
                        tenantId,
                        userId,
                        null,
                        productCode,
                        featureCode,
                        channel,
                        BigDecimal.ONE,
                        featureCode.contains("generation") ? "generation" : "chat",
                        traceId,
                        "AI 网关调用"
                ));
            }
        } catch (BusinessException ex) {
            recordLedger(requestId, tenantId, userId, productCode, featureCode, channel, traceId,
                    "denied", "denied", featureCode, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, false);
            return AiCallResponse.denied(ex.getMessage(), requestId, traceId);
        }

        AiProvider provider = aiProviderRouter.resolve(featureCode);
        AiProviderResult providerResult = provider.chatWithUsage(prompt);
        if (providerResult == null) {
            providerResult = aiProviderRouter.resolve("mock").chatWithUsage(prompt);
        }

        BigDecimal tokens = BigDecimal.valueOf(providerResult.totalTokens());
        BigDecimal costCny = estimateCostCny(featureCode, providerResult);
        BigDecimal retail = costCny.multiply(aiGatewayProperties.getRetailMarkup()).setScale(2, RoundingMode.HALF_UP);

        recordLedger(requestId, tenantId, userId, productCode, featureCode, channel, traceId,
                providerResult.providerCode(), providerResult.modelCode(), featureCode,
                tokens, costCny, retail, true);

        log.info("AI 调用完成: tenant={}, feature={}, provider={}", tenantId, featureCode, provider.providerCode());
        return AiCallResponse.success(
                providerResult.output(),
                estimateCost(featureCode),
                requestId,
                traceId,
                providerResult.providerCode(),
                providerResult.modelCode()
        );
    }

    /**
     * Agent 每轮 LLM 前的商业化扣费（不调用 Provider）。
     */
    public AiCommercialFacade.CommercialChargeResult chargeCommercialRound(String featureCode, String traceId) {
        return aiCommercialFacade.chargeRound(featureCode, "AGENT", traceId);
    }

    public Map<String, Long> getUsageStats(String tenantId) {
        if (aiUsageStatsService != null) {
            return aiUsageStatsService.countByFeature(tenantId);
        }
        return Map.of();
    }

    public long totalInvocations(String tenantId) {
        if (aiUsageStatsService != null) {
            return aiUsageStatsService.totalInvocations(tenantId);
        }
        return 0L;
    }

    private static String resolveTenantId(IdentityContext ctx) {
        String tenantId = CommercialIdentityBridge.resolveTenantId(ctx);
        if (tenantId == null || tenantId.isBlank() || "default".equals(tenantId)) {
            return "demo-tenant";
        }
        return tenantId;
    }

    private static String resolveUserId(IdentityContext ctx) {
        String userId = CommercialIdentityBridge.resolveUserId(ctx);
        return userId != null ? userId : "ai-user";
    }

    private void recordLedger(
            String requestId,
            String tenantId,
            String userId,
            String productCode,
            String featureCode,
            String channel,
            String traceId,
            String providerCode,
            String modelCode,
            String promptCode,
            BigDecimal tokens,
            BigDecimal costCny,
            BigDecimal retail,
            boolean success
    ) {
        aiInvocationLedgerService.record(new AiInvocationRecord(
                requestId,
                tenantId,
                userId,
                productCode,
                featureCode,
                providerCode,
                modelCode,
                promptCode,
                tokens,
                costCny,
                retail,
                traceId,
                channel,
                success
        ));
    }

    private static String resolveProduct(String featureCode) {
        if (FeatureCode.AI_CHAT.equals(featureCode) || FeatureCode.AI_GENERATION.equals(featureCode)
                || FeatureCode.AI_EVOLUTION.equals(featureCode)) {
            return ProductCode.DOUYIN_OPS;
        }
        return featureCode.contains(".") ? featureCode.substring(0, featureCode.indexOf('.')) : ProductCode.DOUYIN_OPS;
    }

    private long estimateCost(String featureCode) {
        return switch (featureCode) {
            case FeatureCode.AI_CHAT -> 1;
            case FeatureCode.AI_GENERATION, FeatureCode.AI_EVOLUTION -> 5;
            case FeatureCode.AI_MCP_TOOL -> 2;
            default -> 1;
        };
    }

    private BigDecimal estimateCostCny(String featureCode, AiProviderResult result) {
        if (result.mock()) {
            return BigDecimal.ZERO;
        }
        BigDecimal unit = FeatureCode.AI_GENERATION.equals(featureCode)
                || FeatureCode.AI_EVOLUTION.equals(featureCode)
                ? aiGatewayProperties.getGenerationUnitCostCny()
                : aiGatewayProperties.getChatUnitCostCny();
        return unit.multiply(BigDecimal.valueOf(estimateCost(featureCode)));
    }

    private static AiResult toAiResult(AiCallResponse response) {
        if (response.success()) {
            return AiResult.success(response.output(), response.costCredits(), response.requestId(), response.traceId());
        }
        return AiResult.denied(response.deniedReason(), response.requestId(), response.traceId());
    }

    public record AiResult(
            boolean success,
            String output,
            String error,
            long costCredits,
            String requestId,
            String traceId
    ) {
        static AiResult success(String output, long cost, String requestId, String traceId) {
            return new AiResult(true, output, null, cost, requestId, traceId);
        }

        static AiResult denied(String reason) {
            return new AiResult(false, null, reason, 0, null, null);
        }

        static AiResult denied(String reason, String requestId, String traceId) {
            return new AiResult(false, null, reason, 0, requestId, traceId);
        }
    }
}
