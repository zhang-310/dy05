package cn.gaifan.douyinOperations.contract.mcp;

import java.util.Map;

public record McpInvocationRequest(
        String tenantId,
        String userId,
        String toolCode,
        String channel,
        Map<String, String> arguments,
        String traceId
) {
}
