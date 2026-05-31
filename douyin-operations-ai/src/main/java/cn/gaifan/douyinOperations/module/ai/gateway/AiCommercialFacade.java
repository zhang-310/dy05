package cn.gaifan.douyinOperations.module.ai.gateway;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.module.platform.ai.AiInvocationLedgerService;
import cn.gaifan.douyinOperations.module.platform.ai.AiInvocationRecord;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialCreditHelper;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Agent / Evolve 等场景的轻量商业化：授权 + 扣费 + 台账，不重复调用 LLM Provider。
 */
@Service
public class AiCommercialFacade {

    @Resource
    private PlatformEntitlementService platformEntitlementService;

    @Autowired(required = false)
    private CommercialCreditHelper commercialCreditHelper;

    @Resource
    private AiInvocationLedgerService aiInvocationLedgerService;

    public CommercialChargeResult chargeRound(String featureCode, String channel, String traceId) {
        return chargeRound(featureCode, channel, traceId, null, null);
    }

    public CommercialChargeResult chargeRound(String featureCode, String channel, String traceId,
                                               String tenantOverride, String userOverride) {
        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = tenantOverride != null && !tenantOverride.isBlank()
                ? tenantOverride
                : resolveTenantId(ctx);
        String userId = userOverride != null && !userOverride.isBlank()
                ? userOverride
                : resolveUserId(ctx);
        String ch = channel != null && !channel.isBlank() ? channel : (ctx != null && ctx.channel() != null ? ctx.channel() : "AI");
        String tid = traceId != null && !traceId.isBlank() ? traceId : CommercialIdentityBridge.resolveTraceId(ctx);
        String productCode = resolveProduct(featureCode);
        String requestId = aiInvocationLedgerService.newRequestId();

        CommercialEntitlementDecision decision = platformEntitlementService.check(new EntitlementCheckRequest(
                tenantId, userId, productCode, featureCode, ch, BigDecimal.ONE, null, tid));
        if (!decision.allowed()) {
            record(requestId, tenantId, userId, productCode, featureCode, ch, tid, false);
            return CommercialChargeResult.denied(decision.reason(), requestId, tid);
        }
        try {
            if (commercialCreditHelper != null) {
                commercialCreditHelper.consume(new CreditConsumeRequest(
                        tenantId, userId, null, productCode, featureCode, ch,
                        BigDecimal.ONE,
                        featureCode.contains("generation") ? "generation" : "chat",
                        tid,
                        "AI 商业化扣费"
                ));
            }
        } catch (BusinessException ex) {
            record(requestId, tenantId, userId, productCode, featureCode, ch, tid, false);
            return CommercialChargeResult.denied(ex.getMessage(), requestId, tid);
        }
        record(requestId, tenantId, userId, productCode, featureCode, ch, tid, true);
        return CommercialChargeResult.ok(requestId, tid);
    }

    private void record(String requestId, String tenantId, String userId, String productCode,
                        String featureCode, String channel, String traceId, boolean success) {
        aiInvocationLedgerService.record(new AiInvocationRecord(
                requestId, tenantId, userId, productCode, featureCode,
                "agent", "agent-llm", featureCode,
                BigDecimal.ONE, BigDecimal.ZERO, BigDecimal.ZERO,
                traceId, channel, success));
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

    private static String resolveProduct(String featureCode) {
        if (FeatureCode.AI_CHAT.equals(featureCode) || FeatureCode.AI_GENERATION.equals(featureCode)
                || FeatureCode.AI_EVOLUTION.equals(featureCode)) {
            return ProductCode.DOUYIN_OPS;
        }
        return featureCode.contains(".") ? featureCode.substring(0, featureCode.indexOf('.')) : ProductCode.DOUYIN_OPS;
    }

    public record CommercialChargeResult(boolean allowed, String reason, String requestId, String traceId) {
        static CommercialChargeResult ok(String requestId, String traceId) {
            return new CommercialChargeResult(true, null, requestId, traceId);
        }

        static CommercialChargeResult denied(String reason, String requestId, String traceId) {
            return new CommercialChargeResult(false, reason, requestId, traceId);
        }
    }
}
