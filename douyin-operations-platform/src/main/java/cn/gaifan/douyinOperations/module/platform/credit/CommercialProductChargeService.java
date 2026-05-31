package cn.gaifan.douyinOperations.module.platform.credit;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryProduct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * 六产品统一商业化：授权检查 → 积分 consume → 可选交付台账。
 */
@Service
public class CommercialProductChargeService {

    private final PlatformEntitlementService entitlementService;
    private final CommercialCreditHelper commercialCreditHelper;
    private final DeliveryLedgerService deliveryLedgerService;

    public CommercialProductChargeService(
            PlatformEntitlementService entitlementService,
            CommercialCreditHelper commercialCreditHelper,
            DeliveryLedgerService deliveryLedgerService
    ) {
        this.entitlementService = entitlementService;
        this.commercialCreditHelper = commercialCreditHelper;
        this.deliveryLedgerService = deliveryLedgerService;
    }

    /**
     * 授权 + 扣费 + 交付记录（enforce=false 时跳过真实扣费但仍可写交付）。
     */
    public void charge(CommercialProductChargeCommand command) {
        IdentityContext ctx = RequestIdentityHolder.current();
        String tenantId = command.tenantId() != null && !command.tenantId().isBlank()
                ? command.tenantId()
                : CommercialIdentityBridge.resolveTenantId(ctx);
        if ("default".equals(tenantId) || tenantId == null || tenantId.isBlank()) {
            tenantId = "demo-tenant";
        }
        String userId = command.userId() != null && !command.userId().isBlank()
                ? command.userId()
                : CommercialIdentityBridge.resolveUserId(ctx);
        if (userId == null) {
            userId = "unknown-user";
        }
        String channel = command.channel() != null && !command.channel().isBlank()
                ? command.channel()
                : (ctx != null && ctx.channel() != null ? ctx.channel() : "WEB");
        String traceId = command.traceId() != null && !command.traceId().isBlank()
                ? command.traceId()
                : CommercialIdentityBridge.resolveTraceId(ctx);

        CommercialEntitlementDecision decision = entitlementService.check(new EntitlementCheckRequest(
                tenantId,
                userId,
                command.productCode(),
                command.featureCode(),
                channel,
                command.amount() != null ? command.amount() : BigDecimal.ONE,
                command.pricingTier(),
                traceId
        ));
        if (!decision.allowed()) {
            throw new BusinessException(ErrorCode.ENTITLEMENT_DENIED, "产品未授权：" + decision.reason());
        }

        if (commercialCreditHelper != null) {
            commercialCreditHelper.consume(new CreditConsumeRequest(
                    tenantId,
                    userId,
                    command.agentId(),
                    command.productCode(),
                    command.featureCode(),
                    channel,
                    command.amount() != null ? command.amount() : BigDecimal.ONE,
                    command.pricingTier() != null ? command.pricingTier() : "standard",
                    traceId,
                    command.reason() != null ? command.reason() : command.featureCode()
            ));
        }

        if (deliveryLedgerService != null && command.deliveryProduct() != null) {
            String status = command.deliveryStatus() != null ? command.deliveryStatus() : "STARTED";
            deliveryLedgerService.recordDelivery(command.deliveryProduct(), tenantId, traceId, status);
        }
    }

    public record CommercialProductChargeCommand(
            String productCode,
            String featureCode,
            String tenantId,
            String userId,
            String agentId,
            String channel,
            String traceId,
            String reason,
            BigDecimal amount,
            String pricingTier,
            DeliveryProduct deliveryProduct,
            String deliveryStatus
    ) {
        public static CommercialProductChargeCommand of(
                String productCode,
                String featureCode,
                String reason,
                DeliveryProduct deliveryProduct
        ) {
            return new CommercialProductChargeCommand(
                    productCode, featureCode, null, null, null, null, null,
                    reason, BigDecimal.ONE, "standard", deliveryProduct, "STARTED"
            );
        }
    }
}
