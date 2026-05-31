package cn.gaifan.douyinOperations.module.platform.commercial;

import cn.gaifan.douyinOperations.contract.credit.CreditAccountSummary;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import cn.gaifan.douyinOperations.contract.credit.CreditGovernanceOverview;
import cn.gaifan.douyinOperations.contract.mcp.McpGatewayOverview;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiDeveloperOverview;
import cn.gaifan.douyinOperations.module.mcp.controller.McpController;
import cn.gaifan.douyinOperations.module.openapi.controller.OpenApiDeveloperController;
import cn.gaifan.douyinOperations.module.platform.controller.CreditController;
import cn.gaifan.douyinOperations.module.platform.credit.CreditLedgerService;
import cn.gaifan.douyinOperations.mcp.registry.McpToolRegistry;
import cn.gaifan.douyinOperations.module.mcp.McpInvokeService;
import cn.gaifan.douyinOperations.module.openapi.OpenApiDeveloperService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Gaifan 商业化 Controller 测试")
class GaifanCommercialE2ETest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private CreditLedgerService creditLedgerService;

    @Mock
    private McpToolRegistry mcpToolRegistry;

    @Mock
    private McpInvokeService mcpInvokeService;

    @Mock
    private OpenApiDeveloperService openApiDeveloperService;

    @BeforeEach
    void setUp() {
        when(creditLedgerService.account(anyString())).thenReturn(new CreditAccountSummary(
                "demo-tenant",
                new BigDecimal("20000"),
                new BigDecimal("17120"),
                new BigDecimal("120"),
                new BigDecimal("2860"),
                BigDecimal.ZERO,
                "CNY",
                "monthly"
        ));
        when(creditLedgerService.consume(any())).thenReturn(new CreditConsumeResult(
                "txn-e2e",
                true,
                "ALLOW",
                "mock consume",
                null,
                BigDecimal.ONE,
                new BigDecimal("17119"),
                null,
                null
        ));
        when(mcpToolRegistry.overview()).thenReturn(new McpGatewayOverview(
                List.of(), 0, 0, 0, "MCP", "test"
        ));
        when(openApiDeveloperService.overview(anyString())).thenReturn(new OpenApiDeveloperOverview(
                "demo-tenant",
                List.of(),
                List.of(),
                List.of(),
                new CreditGovernanceOverview("demo-tenant", null, List.of(), List.of(), List.of()),
                List.of(),
                "api-key",
                "HMAC",
                List.of()
        ));

        mockMvc = MockMvcBuilders.standaloneSetup(
                new CreditController(creditLedgerService),
                new McpController(mcpToolRegistry, mcpInvokeService),
                new OpenApiDeveloperController(openApiDeveloperService)
        ).build();
    }

    @Test
    @DisplayName("GET /api/credits/account")
    void creditsAccount() throws Exception {
        mockMvc.perform(get("/api/credits/account").param("tenantId", "demo-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value("demo-tenant"));
    }

    @Test
    @DisplayName("POST /api/credits/consume")
    void creditsConsume() throws Exception {
        Map<String, Object> body = Map.of(
                "tenantId", "demo-tenant",
                "userId", "demo-user",
                "productCode", "video-insight",
                "featureCode", "video-insight.breakdown",
                "channel", "WEB",
                "requestedAmount", 1,
                "traceId", "trace-e2e-consume"
        );
        mockMvc.perform(post("/api/credits/consume")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.allowed").value(true));
    }

    @Test
    @DisplayName("GET /api/mcp/overview")
    void mcpOverview() throws Exception {
        mockMvc.perform(get("/api/mcp/overview"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.toolCount").value(0));
    }

    @Test
    @DisplayName("GET /api/openapi/overview")
    void openapiOverview() throws Exception {
        mockMvc.perform(get("/api/openapi/overview").param("tenantId", "demo-tenant"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tenantId").value("demo-tenant"));
    }
}
