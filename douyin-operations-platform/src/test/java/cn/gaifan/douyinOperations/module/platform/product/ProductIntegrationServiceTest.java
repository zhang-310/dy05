package cn.gaifan.douyinOperations.module.platform.product;

import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementDecisionCode;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import cn.gaifan.douyinOperations.contract.credit.CreditQuoteResult;
import cn.gaifan.douyinOperations.contract.product.FeatureCode;
import cn.gaifan.douyinOperations.contract.product.ProductCode;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationCheckRequest;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationDecision;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationRequest;
import cn.gaifan.douyinOperations.contract.product.ProductIntegrationInvocationResult;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.credit.CreditLedgerService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductIntegrationService 单测")
class ProductIntegrationServiceTest {

    @Mock
    private PlatformEntitlementService entitlementService;

    @Mock
    private CreditLedgerService creditLedgerService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ProductIntegrationService productIntegrationService;

    @Test
    @DisplayName("互调规则矩阵非空")
    void listRules_notEmpty() {
        assertFalse(productIntegrationService.listRules().isEmpty());
    }

    @Test
    @DisplayName("互调矩阵包含 mcp-to-video-insight")
    void listRules_containsMcpBridge() {
        assertTrue(productIntegrationService.listRules().stream()
                .anyMatch(rule -> "mcp-to-video-insight".equals(rule.ruleCode())));
    }

    @Test
    @DisplayName("互调矩阵包含 shortvideo-maker-to-video-insight")
    void listRules_containsShortvideoToInsight() {
        assertTrue(productIntegrationService.listRules().stream()
                .anyMatch(rule -> "shortvideo-maker-to-video-insight".equals(rule.ruleCode())
                        && ProductCode.SHORTVIDEO_MAKER.equals(rule.sourceProductCode())
                        && ProductCode.VIDEO_INSIGHT.equals(rule.targetProductCode())));
    }

    @Test
    @DisplayName("未登记规则拒绝")
    void check_unknownRule_denied() {
        ProductIntegrationDecision decision = productIntegrationService.check(new ProductIntegrationCheckRequest(
                "demo-tenant",
                "demo-user",
                "unknown",
                "unknown.feature",
                "unknown-target",
                "unknown.target",
                "WEB",
                BigDecimal.ONE,
                "trace-x"
        ));
        assertFalse(decision.allowed());
    }

    @Test
    @DisplayName("invoke 授权通过且扣费成功时写入互调流水 RECORDED")
    void invoke_allowed_recordsInvocation() {
        when(entitlementService.check(any())).thenReturn(allowedDecision());
        when(creditLedgerService.consume(any())).thenReturn(successConsume());

        ProductIntegrationInvocationResult result = productIntegrationService.invoke(invocationRequest("trace-invoke-ok"));

        assertTrue(result.allowed());
        assertEquals("RECORDED", result.status());
        assertNotNull(result.invocationId());
        verify(jdbcTemplate, atLeastOnce()).update(
                argThat((String sql) -> sql != null && sql.contains("gf_product_integration_invocation")),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    @DisplayName("invoke 积分不足时 CREDIT_DENIED 仍写入互调流水")
    void invoke_creditDenied() {
        when(entitlementService.check(any())).thenReturn(allowedDecision());
        when(creditLedgerService.consume(any())).thenReturn(creditDenied());

        ProductIntegrationInvocationResult result = productIntegrationService.invoke(invocationRequest("trace-invoke-deny"));

        assertFalse(result.allowed());
        assertEquals("CREDIT_DENIED", result.status());
        verify(jdbcTemplate, atLeastOnce()).update(
                argThat((String sql) -> sql != null && sql.contains("gf_product_integration_invocation")),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private static CommercialEntitlementDecision allowedDecision() {
        return new CommercialEntitlementDecision(
                true,
                EntitlementDecisionCode.ALLOW,
                ProductCode.VIDEO_INSIGHT,
                "video.mcp.invoke",
                "allowed",
                null,
                null,
                false
        );
    }

    private static CreditConsumeResult successConsume() {
        CreditQuoteResult quote = new CreditQuoteResult(
                "quote-1",
                "demo-tenant",
                ProductCode.VIDEO_INSIGHT,
                "video.mcp.invoke",
                "WEB",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                new BigDecimal("17120"),
                true,
                "test",
                "trace-invoke-ok"
        );
        return new CreditConsumeResult(
                "credit_txn_1",
                true,
                "ALLOW",
                "ok",
                quote,
                BigDecimal.ONE,
                new BigDecimal("17119"),
                null,
                null
        );
    }

    private static CreditConsumeResult creditDenied() {
        CreditQuoteResult quote = new CreditQuoteResult(
                "quote-deny",
                "demo-tenant",
                ProductCode.VIDEO_INSIGHT,
                "video.mcp.invoke",
                "WEB",
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ONE,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                false,
                "insufficient",
                "trace-invoke-deny"
        );
        return new CreditConsumeResult(
                null,
                false,
                "INSUFFICIENT_CREDITS",
                "积分不足",
                quote,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                null,
                null
        );
    }

    @Test
    @DisplayName("invoke drama→shortvideo-maker 规则路径写入互调流水")
    void invoke_dramaToShortvideoMaker_recordsInvocation() {
        when(entitlementService.check(any())).thenReturn(allowedDecision());
        when(creditLedgerService.consume(any())).thenReturn(successConsume());

        ProductIntegrationInvocationRequest req = new ProductIntegrationInvocationRequest(
                "demo-tenant",
                "demo-user",
                null,
                ProductCode.DRAMA_AI,
                FeatureCode.DRAMA_SCRIPT,
                ProductCode.SHORTVIDEO_MAKER,
                FeatureCode.SHORTVIDEO_EXPORT,
                "WEB",
                BigDecimal.ONE,
                "standard",
                "trace-drama-export",
                "drama export integration test",
                false,
                null,
                null,
                "99",
                null,
                false
        );
        ProductIntegrationInvocationResult result = productIntegrationService.invoke(req);
        assertTrue(result.allowed());
        assertEquals("RECORDED", result.status());
        verify(jdbcTemplate, atLeastOnce()).update(
                argThat((String sql) -> sql != null && sql.contains("gf_product_integration_invocation")),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any()
        );
    }

    private static ProductIntegrationInvocationRequest invocationRequest(String traceId) {
        return new ProductIntegrationInvocationRequest(
                "demo-tenant",
                "demo-user",
                "agent-test",
                "mcp-service",
                "video.mcp.invoke",
                ProductCode.VIDEO_INSIGHT,
                "video.mcp.invoke",
                "WEB",
                BigDecimal.ONE,
                "standard",
                traceId,
                "invoke unit test",
                false,
                null,
                null,
                null,
                null,
                false
        );
    }
}
