package cn.gaifan.douyinOperations.module.platform.service;

import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.auth.EntitlementProvider;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.product.EntitlementDecision;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * BFF 授权：委托 {@link PlatformEntitlementService}（gf_entitlement），未开启 enforce 时保持兼容放行。
 */
@Service
public class EntitlementServiceImpl implements EntitlementProvider {

    private final PlatformEntitlementService platformEntitlementService;

    @Value("${app.credit.enforce:false}")
    private boolean creditEnforce;

    public EntitlementServiceImpl(PlatformEntitlementService platformEntitlementService) {
        this.platformEntitlementService = platformEntitlementService;
    }

    @Override
    public EntitlementDecision check(String tenantId, Long userId, String productCode, String featureCode) {
        if (userId == null) {
            return EntitlementDecision.denied(productCode, featureCode, "未登录");
        }
        if (!creditEnforce) {
            return EntitlementDecision.granted(productCode, featureCode);
        }
        IdentityContext ctx = IdentityContext.authenticated(
                tenantId != null ? tenantId : CommercialIdentityBridge.resolveTenantId(
                        IdentityContext.authenticated("default", userId, null, null, "WEB", null, null)),
                userId, null, null, "WEB", null, null);
        String commercialTenant = CommercialIdentityBridge.resolveTenantId(ctx);
        CommercialEntitlementDecision commercial = platformEntitlementService.check(new EntitlementCheckRequest(
                commercialTenant,
                CommercialIdentityBridge.resolveUserId(ctx),
                productCode,
                featureCode,
                "WEB",
                BigDecimal.ONE,
                null,
                CommercialIdentityBridge.resolveTraceId(ctx)
        ));
        if (commercial.allowed()) {
            return EntitlementDecision.granted(productCode, featureCode);
        }
        return EntitlementDecision.denied(productCode, featureCode, commercial.reason());
    }
}
