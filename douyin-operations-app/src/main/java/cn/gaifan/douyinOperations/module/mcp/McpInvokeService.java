package cn.gaifan.douyinOperations.module.mcp;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.id.Ids;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.auth.EntitlementCheckRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationActionRequest;
import cn.gaifan.douyinOperations.contract.credit.CreditReservationResult;
import cn.gaifan.douyinOperations.contract.credit.CreditReserveRequest;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationRequest;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationResponse;
import cn.gaifan.douyinOperations.contract.mcp.McpInvocationSummary;
import cn.gaifan.douyinOperations.contract.mcp.McpToolDescriptor;
import cn.gaifan.douyinOperations.mcp.registry.McpToolRegistry;
import cn.gaifan.douyinOperations.module.platform.auth.PlatformEntitlementService;
import cn.gaifan.douyinOperations.module.platform.credit.CommercialCreditHelper;
import cn.gaifan.douyinOperations.module.platform.identity.CommercialIdentityBridge;
import cn.gaifan.douyinOperations.module.platform.product.DeliveryLedgerService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ViralVideoDeepAnalysisService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 工具调用编排：身份 → 授权 → 积分 → 业务执行 → gf_mcp_invocation 台账。
 */
@Service
public class McpInvokeService {

    private static final String DEFAULT_TENANT = "demo-tenant";
    private static final String DEFAULT_USER = "mcp-user";
    private static final String DEFAULT_AGENT = "agent_mcp_default";

    private final McpToolRegistry mcpToolRegistry;
    private final PlatformEntitlementService entitlementService;
    private final CommercialCreditHelper commercialCreditHelper;
    private final JdbcTemplate jdbcTemplate;
    private final ViralVideoDeepAnalysisService viralVideoDeepAnalysisService;
    private final DeliveryLedgerService deliveryLedgerService;

    public McpInvokeService(
            McpToolRegistry mcpToolRegistry,
            PlatformEntitlementService entitlementService,
            CommercialCreditHelper commercialCreditHelper,
            JdbcTemplate jdbcTemplate,
            ViralVideoDeepAnalysisService viralVideoDeepAnalysisService,
            DeliveryLedgerService deliveryLedgerService
    ) {
        this.mcpToolRegistry = mcpToolRegistry;
        this.entitlementService = entitlementService;
        this.commercialCreditHelper = commercialCreditHelper;
        this.jdbcTemplate = jdbcTemplate;
        this.viralVideoDeepAnalysisService = viralVideoDeepAnalysisService;
        this.deliveryLedgerService = deliveryLedgerService;
    }

    public McpInvocationResponse invoke(McpInvocationRequest request) {
        McpToolDescriptor tool = mcpToolRegistry.findTool(request.toolCode()).orElse(null);
        if (tool == null) {
            return reject(null, request, null, "TOOL_NOT_FOUND", "MCP 工具不存在", null, null);
        }

        IdentityContext identity = RequestIdentityHolder.current();
        String tenantId = resolveTenant(identity, request);
        String userId = resolveUserId(identity, request);
        String agentId = resolveAgentId(identity, request);
        String channel = resolveChannel(identity, request);
        String traceId = resolveTraceId(identity, request);

        if (!tenantExists(tenantId)) {
            return reject(tool, request, null, "TENANT_NOT_FOUND", "MCP 租户不存在", null, null);
        }

        CommercialEntitlementDecision decision = entitlementService.check(new EntitlementCheckRequest(
                tenantId,
                userId,
                tool.requiredProductCode(),
                tool.requiredFeatureCode(),
                channel,
                BigDecimal.ONE,
                null,
                traceId
        ));
        if (!decision.allowed()) {
            String invocationId = Ids.compactUuid("mcp_inv");
            recordInvocation(invocationId, tenantId, userId, agentId, tool, channel, false, traceId);
            return new McpInvocationResponse(
                    invocationId,
                    tool.toolCode(),
                    false,
                    decision.decisionCode().name(),
                    "MCP 授权拒绝：" + decision.reason(),
                    tool,
                    decision,
                    null,
                    null,
                    null,
                    null,
                    recordRejectedAudit(tenantId, userId, tool, channel, traceId, decision.decisionCode().name(), decision.reason()),
                    tool.auditRequired(),
                    tool.rateLimitHint(),
                    OffsetDateTime.now()
            );
        }

        if ("video.analyze".equals(tool.toolCode())) {
            return invokeVideoAnalyze(tool, request, decision, tenantId, userId, agentId, channel, traceId);
        }

        CreditConsumeResult creditResult;
        try {
            creditResult = commercialCreditHelper.consume(new CreditConsumeRequest(
                    tenantId,
                    userId,
                    agentId,
                    tool.requiredProductCode(),
                    tool.requiredFeatureCode(),
                    channel,
                    BigDecimal.ONE,
                    pricingTier(tool.requiredFeatureCode()),
                    traceId,
                    "MCP 工具调用：" + tool.toolCode()
            ));
        } catch (BusinessException ex) {
            String invocationId = Ids.compactUuid("mcp_inv");
            recordInvocation(invocationId, tenantId, userId, agentId, tool, channel, false, traceId);
            return reject(tool, request, decision, "CREDIT_DENIED", ex.getMessage(), null, null);
        }

        Object toolResult = Map.of(
                "status", "NOT_IMPLEMENTED",
                "toolCode", tool.toolCode(),
                "message", "工具已登记台账，业务实现待扩展",
                "traceId", traceId
        );
        String invocationId = Ids.compactUuid("mcp_inv");
        recordInvocation(invocationId, tenantId, userId, agentId, tool, channel, true, traceId);
        return new McpInvocationResponse(
                invocationId,
                tool.toolCode(),
                true,
                decision.decisionCode().name(),
                "MCP 调用完成（占位）",
                tool,
                decision,
                creditResult,
                null,
                toolResult,
                null,
                recordAudit(tenantId, userId, tool, channel, traceId),
                tool.auditRequired(),
                tool.rateLimitHint(),
                OffsetDateTime.now()
        );
    }

    public List<McpInvocationSummary> listInvocations(String tenantId) {
        String normalized = tenantId == null || tenantId.isBlank() ? DEFAULT_TENANT : tenantId.trim();
        return jdbcTemplate.query(
                """
                        select invocation_id, tenant_id, user_id, agent_id, tool_code, product_code, feature_code,
                               channel, allowed, trace_id, created_at
                        from gf_mcp_invocation
                        where tenant_id = ?
                        order by created_at desc
                        limit 50
                        """,
                (rs, rowNum) -> new McpInvocationSummary(
                        rs.getString("invocation_id"),
                        rs.getString("tenant_id"),
                        rs.getString("user_id"),
                        rs.getString("agent_id"),
                        rs.getString("tool_code"),
                        rs.getString("product_code"),
                        rs.getString("feature_code"),
                        rs.getString("channel"),
                        rs.getBoolean("allowed"),
                        rs.getString("trace_id"),
                        rs.getObject("created_at", OffsetDateTime.class)
                ),
                normalized
        );
    }

    private McpInvocationResponse invokeVideoAnalyze(
            McpToolDescriptor tool,
            McpInvocationRequest request,
            CommercialEntitlementDecision decision,
            String tenantId,
            String userId,
            String agentId,
            String channel,
            String traceId
    ) {
        Map<String, String> args = request.arguments() == null ? Map.of() : request.arguments();
        Long viralVideoId = parseLong(firstNonBlank(args.get("viralVideoId"), args.get("videoId")));
        Long numericUserId = parseLong(firstNonBlank(args.get("userId"), userId));
        if (viralVideoId == null) {
            return reject(tool, request, decision, "INVALID_ARGUMENT", "video.analyze 需要 viralVideoId 或 videoId", null, null);
        }
        if (numericUserId == null) {
            numericUserId = 1L;
        }

        String businessKey = "mcp-video-" + viralVideoId + "-" + traceId;
        CreditReservationResult reserved;
        try {
            reserved = commercialCreditHelper.reserve(new CreditReserveRequest(
                    tenantId,
                    userId,
                    agentId,
                    tool.requiredProductCode(),
                    tool.requiredFeatureCode(),
                    channel,
                    BigDecimal.ONE,
                    pricingTier(tool.requiredFeatureCode()),
                    traceId,
                    businessKey,
                    "MCP video.analyze 冻结"
            ));
        } catch (BusinessException ex) {
            String deniedId = Ids.compactUuid("mcp_inv");
            recordInvocation(deniedId, tenantId, userId, agentId, tool, channel, false, traceId);
            return reject(tool, request, decision, "CREDIT_DENIED", ex.getMessage(), null, null);
        }

        String invocationId = Ids.compactUuid("mcp_inv");
        try {
            Map<String, Object> analyzeResult = viralVideoDeepAnalysisService.startDeepAnalyze(viralVideoId, numericUserId);
            CreditReservationResult committed = commercialCreditHelper.commit(
                    reserved.reservationId(),
                    new CreditReservationActionRequest(tenantId, userId, agentId, channel, traceId, "MCP video.analyze 提交扣费")
            );
            deliveryLedgerService.recordVideoInsightDelivery(tenantId, traceId, "MCP_ACCEPTED");
            recordInvocation(invocationId, tenantId, userId, agentId, tool, channel, true, traceId);
            Map<String, Object> toolResult = new LinkedHashMap<>(analyzeResult);
            toolResult.put("traceId", traceId);
            toolResult.put("reservationId", reserved.reservationId());
            CreditConsumeResult creditView = toConsumeView(committed);
            return new McpInvocationResponse(
                    invocationId,
                    tool.toolCode(),
                    true,
                    "ACCEPTED",
                    "爆款拆解任务已提交",
                    tool,
                    decision,
                    creditView,
                    null,
                    toolResult,
                    null,
                    recordAudit(tenantId, userId, tool, channel, traceId),
                    true,
                    tool.rateLimitHint(),
                    OffsetDateTime.now()
            );
        } catch (RuntimeException ex) {
            commercialCreditHelper.release(
                    reserved.reservationId(),
                    new CreditReservationActionRequest(tenantId, userId, agentId, channel, traceId, "MCP video.analyze 失败释放：" + ex.getMessage())
            );
            recordInvocation(invocationId, tenantId, userId, agentId, tool, channel, false, traceId);
            return new McpInvocationResponse(
                    invocationId,
                    tool.toolCode(),
                    false,
                    "EXECUTION_FAILED",
                    ex.getMessage(),
                    tool,
                    decision,
                    null,
                    null,
                    null,
                    null,
                    recordRejectedAudit(tenantId, userId, tool, channel, traceId, "EXECUTION_FAILED", ex.getMessage()),
                    true,
                    tool.rateLimitHint(),
                    OffsetDateTime.now()
            );
        }
    }

    private static CreditConsumeResult toConsumeView(CreditReservationResult committed) {
        return new CreditConsumeResult(
                committed.reservationId(),
                committed.allowed(),
                committed.decisionCode(),
                committed.message(),
                committed.quote(),
                committed.reservedCredits(),
                committed.balanceAfter(),
                committed.reserveEntry(),
                committed.commitEntry()
        );
    }

    private McpInvocationResponse reject(
            McpToolDescriptor tool,
            McpInvocationRequest request,
            CommercialEntitlementDecision decision,
            String code,
            String message,
            CreditConsumeResult credit,
            Object toolResult
    ) {
        return new McpInvocationResponse(
                Ids.compactUuid("mcp_inv_reject"),
                request.toolCode(),
                false,
                code,
                message,
                tool,
                decision,
                credit,
                null,
                toolResult,
                null,
                null,
                tool != null && tool.auditRequired(),
                tool != null ? tool.rateLimitHint() : null,
                OffsetDateTime.now()
        );
    }

    private void recordInvocation(
            String invocationId,
            String tenantId,
            String userId,
            String agentId,
            McpToolDescriptor tool,
            String channel,
            boolean allowed,
            String traceId
    ) {
        jdbcTemplate.update(
                """
                        insert into gf_mcp_invocation (invocation_id, tenant_id, user_id, agent_id, tool_code,
                            product_code, feature_code, channel, allowed, trace_id, created_at)
                        values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        on conflict (invocation_id) do nothing
                        """,
                invocationId,
                tenantId,
                userId,
                agentId,
                tool.toolCode(),
                tool.requiredProductCode(),
                tool.requiredFeatureCode(),
                channel,
                allowed,
                traceId,
                OffsetDateTime.now()
        );
    }

    private String recordAudit(String tenantId, String userId, McpToolDescriptor tool, String channel, String traceId) {
        String auditId = Ids.compactUuid("audit_mcp");
        jdbcTemplate.update(
                """
                        insert into gf_audit_event (audit_id, tenant_id, user_id, product_code, feature_code,
                            channel, risk_level, status, trace_id, decision_note, created_at)
                        values (?, ?, ?, ?, ?, ?, 'MEDIUM', 'RECORDED', ?, ?, ?)
                        on conflict (audit_id) do nothing
                        """,
                auditId,
                tenantId,
                userId,
                tool.requiredProductCode(),
                tool.requiredFeatureCode(),
                channel,
                traceId,
                "MCP 工具调用已记录 toolCode=" + tool.toolCode(),
                OffsetDateTime.now()
        );
        return auditId;
    }

    private String recordRejectedAudit(
            String tenantId,
            String userId,
            McpToolDescriptor tool,
            String channel,
            String traceId,
            String decisionCode,
            String reason
    ) {
        String auditId = Ids.compactUuid("audit_mcp_reject");
        jdbcTemplate.update(
                """
                        insert into gf_audit_event (audit_id, tenant_id, user_id, product_code, feature_code,
                            channel, risk_level, status, trace_id, decision_note, created_at)
                        values (?, ?, ?, ?, ?, ?, 'HIGH', 'REJECTED', ?, ?, ?)
                        on conflict (audit_id) do nothing
                        """,
                auditId,
                tenantId,
                userId,
                tool.requiredProductCode(),
                tool.requiredFeatureCode(),
                channel,
                traceId,
                "MCP 拒绝 toolCode=" + tool.toolCode() + " decision=" + decisionCode + " reason=" + reason,
                OffsetDateTime.now()
        );
        return auditId;
    }

    private boolean tenantExists(String tenantId) {
        Integer count = jdbcTemplate.queryForObject(
                "select count(*) from gf_tenant where tenant_id = ?",
                Integer.class,
                tenantId
        );
        return count != null && count > 0;
    }

    private static String resolveTenant(IdentityContext identity, McpInvocationRequest request) {
        if (identity != null && identity.authenticated()) {
            return CommercialIdentityBridge.resolveTenantId(identity);
        }
        return firstNonBlank(request.tenantId(), DEFAULT_TENANT);
    }

    private static String resolveUserId(IdentityContext identity, McpInvocationRequest request) {
        if (identity != null && identity.authenticated()) {
            String uid = CommercialIdentityBridge.resolveUserId(identity);
            if (uid != null && !uid.isBlank()) {
                return uid;
            }
        }
        return firstNonBlank(request.userId(), DEFAULT_USER);
    }

    private static String resolveAgentId(IdentityContext identity, McpInvocationRequest request) {
        Map<String, String> args = request.arguments();
        if (args != null && args.get("agentId") != null && !args.get("agentId").isBlank()) {
            return args.get("agentId").trim();
        }
        return DEFAULT_AGENT;
    }

    private static String resolveChannel(IdentityContext identity, McpInvocationRequest request) {
        String channel = request.channel();
        if (identity != null && identity.authenticated() && identity.channel() != null) {
            channel = identity.channel();
        }
        return channel == null || channel.isBlank() ? "MCP" : channel.trim().toUpperCase();
    }

    private static String resolveTraceId(IdentityContext identity, McpInvocationRequest request) {
        if (identity != null && identity.traceId() != null && !identity.traceId().isBlank()) {
            return identity.traceId();
        }
        return firstNonBlank(request.traceId(), Ids.compactUuid("trace_mcp"));
    }

    private static String pricingTier(String featureCode) {
        return "video.analyze.deep".equals(featureCode) ? "deep" : "standard";
    }

    private static Long parseLong(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            if (value.startsWith("user-")) {
                value = value.substring(5);
            }
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return "";
    }
}
