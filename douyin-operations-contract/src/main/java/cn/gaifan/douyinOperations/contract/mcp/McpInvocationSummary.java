package cn.gaifan.douyinOperations.contract.mcp;

import java.time.OffsetDateTime;

/**
 * MCP 调用摘要。
 *
 * <p>该合同用于 MCP 控制台展示真实调用记录。它只暴露商业治理需要的结构化字段：
 * 租户、用户、Agent、工具、产品、功能、渠道、是否允许、traceId 和时间，不暴露原始输入、
 * 用户素材、模型完整输出或任何密钥。</p>
 */
public record McpInvocationSummary(
        String invocationId,
        String tenantId,
        String userId,
        String agentId,
        String toolCode,
        String productCode,
        String featureCode,
        String channel,
        boolean allowed,
        String traceId,
        OffsetDateTime createdAt
) {
}
