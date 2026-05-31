package cn.gaifan.douyinOperations.module.mcp;

import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementDecisionCode;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationStatus;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationRequest;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationResponse;
import cn.gaifan.douyinOperations.contract.mcp.McpToolDescriptor;
import cn.gaifan.douyinOperations.mcp.registry.McpToolRegistry;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialCreditHelper;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoDeepAnalysisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("McpInvokeService 积分不足")
class McpInvokeServiceCreditTest {

    @Mock
    private McpToolRegistry mcpToolRegistry;
    @Mock
    private PlatformEntitlementService entitlementService;
    @Mock
    private CommercialCreditHelper commercialCreditHelper;
    @Mock
    private JdbcTemplate jdbcTemplate;
    @Mock
    private ViralVideoDeepAnalysisService viralVideoDeepAnalysisService;
    @Mock
    private DeliveryLedgerService deliveryLedgerService;

    @Test
    @DisplayName("video.analyze 积分不足返回 CREDIT_DENIED 不抛 BusinessException")
    void videoAnalyze_creditDenied_returnsReject() {
        McpToolDescriptor tool = new McpToolDescriptor(
                "video.analyze", "Analyze", "video-insight",
                "video.analyze.standard", "tenant-minute:30", true);
        when(mcpToolRegistry.findTool("video.analyze")).thenReturn(java.util.Optional.of(tool));
        when(jdbcTemplate.queryForObject(
                eq("select count(*) from gf_tenant where tenant_id = ?"),
                eq(Integer.class),
                eq("demo-tenant"))).thenReturn(1);
        when(entitlementService.check(any())).thenReturn(new CommercialEntitlementDecision(
                true, EntitlementDecisionCode.ALLOW, "video-insight", "video.analyze.standard",
                "ok", null, null, false));
        when(commercialCreditHelper.reserve(any())).thenThrow(new BusinessException(2002, "积分不足"));

        McpInvokeService service = new McpInvokeService(
                mcpToolRegistry, entitlementService, commercialCreditHelper,
                jdbcTemplate, viralVideoDeepAnalysisService, deliveryLedgerService);

        McpInvocationResponse response = service.invoke(new McpInvocationRequest(
                "demo-tenant", "mcp-user", "video.analyze", "MCP",
                Map.of("viralVideoId", "1", "userId", "1"), "trace-credit-deny"));

        assertFalse(response.allowed());
        assertEquals("CREDIT_DENIED", response.decisionCode());
        verify(viralVideoDeepAnalysisService, never()).startDeepAnalyze(anyLong(), anyLong());
    }
}
