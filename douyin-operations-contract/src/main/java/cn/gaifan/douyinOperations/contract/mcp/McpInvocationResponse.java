package cn.gaifan.douyinOperations.contract.mcp;

import cn.gaifan.douyinOperations.contract.ai.AiGatewayResponse;
import cn.gaifan.douyinOperations.contract.auth.CommercialEntitlementDecision;
import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;

import java.time.OffsetDateTime;

public record McpInvocationResponse(
        String invocationId,
        String toolCode,
        boolean allowed,
        String decisionCode,
        String message,
        McpToolDescriptor tool,
        CommercialEntitlementDecision entitlementDecision,
        CreditConsumeResult creditConsumeResult,
        AiGatewayResponse aiResponse,
        // 工具结构化结果，例如 video.find_similar 返回的 pgvector 相似报告结果。
        Object toolResult,
        // 成功调用写入统一用量台账后的 ID；拒绝路径为空，避免把失败调用误算收入。
        String usageLedgerId,
        // 审计事件 ID。成功、授权拒绝、积分拒绝和身份范围拒绝都应返回该字段，方便按 traceId 回查。
        String auditId,
        boolean auditRequired,
        String rateLimitHint,
        OffsetDateTime createdAt
) {
}
