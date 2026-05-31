package cn.gaifan.douyinOperations.module.openapi;

import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.bff.OpenApiCapabilitySummary;
import cn.gaifan.douyinOperations.contract.credit.CreditGovernanceOverview;
import cn.gaifan.douyinOperations.contract.mcp.McpToolDescriptor;
import cn.gaifan.douyinOperations.contract.openapi.ApiKeyStatus;
import cn.gaifan.douyinOperations.contract.openapi.ApiKeySummary;
import cn.gaifan.douyinOperations.contract.openapi.DeveloperAppStatus;
import cn.gaifan.douyinOperations.contract.openapi.DeveloperAppSummary;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiDeveloperOverview;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiMcpQuickstart;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiMcpSandboxCallRequest;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiMcpSandboxCallResult;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiSignedCallRequest;
import cn.gaifan.douyinOperations.contract.openapi.OpenApiSignedCallResult;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationRequest;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationResponse;
import cn.gaifan.douyinOperations.mcp.registry.McpToolRegistry;
import cn.gaifan.douyinOperations.module.mcp.McpInvokeService;
import cn.gaifan.douyinOperations.module.platform.credit.CreditLedgerService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class OpenApiDeveloperService {

    private static final String DEFAULT_TENANT = "demo-tenant";

    private final McpToolRegistry mcpToolRegistry;
    private final CreditLedgerService creditLedgerService;
    private final McpInvokeService mcpInvokeService;
    private final JdbcTemplate jdbcTemplate;

    public OpenApiDeveloperService(
            McpToolRegistry mcpToolRegistry,
            CreditLedgerService creditLedgerService,
            McpInvokeService mcpInvokeService,
            JdbcTemplate jdbcTemplate
    ) {
        this.mcpToolRegistry = mcpToolRegistry;
        this.creditLedgerService = creditLedgerService;
        this.mcpInvokeService = mcpInvokeService;
        this.jdbcTemplate = jdbcTemplate;
    }

    public OpenApiDeveloperOverview overview(String tenantId) {
        String normalized = normalizeTenant(tenantId);
        List<OpenApiCapabilitySummary> capabilities = mcpToolRegistry.listTools().stream()
                .map(this::capability)
                .toList();
        CreditGovernanceOverview credit = creditLedgerService.overview(normalized);
        return new OpenApiDeveloperOverview(
                normalized,
                apps(normalized, capabilities),
                apiKeys(normalized),
                capabilities,
                credit,
                List.of(),
                "api-key + tenant-entitlement",
                "HMAC_SHA256",
                List.of(
                        "开放应用归属租户",
                        "调用需 product/feature 授权",
                        "MCP 与 OpenAPI 共用工具注册表"
                )
        );
    }

    public OpenApiMcpQuickstart quickstart(String tenantId) {
        return new OpenApiMcpQuickstart(
                normalizeTenant(tenantId),
                "https://docs.local",
                "http://localhost:8080",
                "http://localhost:8080/mcp",
                "/api/openapi/sandbox/mcp-call",
                "HMAC_SHA256",
                "300s",
                Map.of(
                        "X-Gaifan-Api-Key", "<apiKey>",
                        "X-Gaifan-Timestamp", "<unixSeconds>",
                        "X-Gaifan-Nonce", "<nonce>",
                        "X-Gaifan-Signature", "<signature>"
                ),
                List.of("拼 canonical string", "HMAC-SHA256", "附带 traceId"),
                "METHOD\\nPATH\\nTIMESTAMP\\nNONCE\\nBODY",
                List.of(),
                List.of("租户授权", "积分台账", "MCP 审计"),
                List.of("禁止 Cookie 绕过", "禁止未授权采集"),
                List.of("开通产品", "创建 API Key", "沙箱调用"),
                List.of("tools/list", "tools/call video.analyze"),
                List.of("gf_credit_ledger", "gf_mcp_invocation"),
                List.of("INSUFFICIENT_CREDITS → 充值", "NO_ENTITLEMENT → 开通产品"),
                List.of("生产启用 IP 白名单", "轮换 API Key")
        );
    }

    public OpenApiMcpSandboxCallResult sandboxCall(OpenApiMcpSandboxCallRequest request) {
        String toolCode = request.capabilityCode() != null ? request.capabilityCode() : "video.analyze";
        String traceId = Ids.compactUuid("trace_openapi");
        OpenApiSignedCallRequest signedBody = new OpenApiSignedCallRequest(
                toolCode,
                request.userId(),
                request.agentId(),
                request.arguments(),
                traceId
        );
        McpInvocationResponse response = mcpInvokeService.invoke(new McpInvocationRequest(
                normalizeTenant(request.tenantId()),
                request.userId() != null ? request.userId() : "openapi-user",
                toolCode,
                "OPENAPI",
                request.arguments(),
                traceId
        ));
        OpenApiSignedCallResult<Object> callResult = new OpenApiSignedCallResult<>(
                response.allowed(),
                response.decisionCode(),
                response.message(),
                null,
                response.creditConsumeResult(),
                response.toolResult(),
                response.usageLedgerId(),
                response.auditId(),
                traceId,
                OffsetDateTime.now()
        );
        return new OpenApiMcpSandboxCallResult(
                "http://localhost:8080",
                "http://localhost:8080/mcp",
                "/api/openapi/sandbox/mcp-call",
                "POST",
                "gf_****demo",
                Map.of("X-Gaifan-Trace-Id", traceId),
                "POST\\n/api/openapi/sandbox/mcp-call\\n...",
                "sig_preview_***",
                signedBody,
                callResult,
                List.of("invocationId=" + response.invocationId(), "auditId=" + response.auditId()),
                List.of("查看 gf_mcp_invocation", "核对 traceId 与积分流水")
        );
    }

    private List<DeveloperAppSummary> apps(String tenantId, List<OpenApiCapabilitySummary> capabilities) {
        List<String> products = capabilities.stream().map(OpenApiCapabilitySummary::productCode).distinct().toList();
        return List.of(new DeveloperAppSummary(
                "app_demo_video_insight",
                tenantId,
                "演示视频洞察应用",
                DeveloperAppStatus.ACTIVE,
                products,
                capabilities.stream().map(OpenApiCapabilitySummary::capabilityCode).limit(5).toList(),
                List.of("https://localhost/callback"),
                List.of("127.0.0.1"),
                true,
                "本地演示",
                "system",
                OffsetDateTime.now().minusDays(1),
                OffsetDateTime.now().minusDays(30)
        ));
    }

    private List<ApiKeySummary> apiKeys(String tenantId) {
        return List.of(new ApiKeySummary(
                "key_demo",
                "app_demo_video_insight",
                "gf_****demo",
                ApiKeyStatus.ACTIVE,
                "system",
                OffsetDateTime.now().minusDays(7),
                null,
                OffsetDateTime.now().plusYears(1),
                null,
                null
        ));
    }

    private OpenApiCapabilitySummary capability(McpToolDescriptor tool) {
        return new OpenApiCapabilitySummary(
                tool.toolCode(),
                tool.name(),
                tool.requiredProductCode(),
                tool.requiredFeatureCode(),
                tool.rateLimitHint(),
                tool.auditRequired(),
                "ACTIVE"
        );
    }

    private static String normalizeTenant(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT : tenantId.trim();
    }
}
