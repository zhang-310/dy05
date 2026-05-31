package cn.gaifan.douyinOperations.contract.mcp;

import java.util.List;

public record McpGatewayOverview(
        List<McpToolDescriptor> tools,
        int toolCount,
        int auditRequiredTools,
        int aiBackedTools,
        String defaultChannel,
        String governanceMode
) {
}
