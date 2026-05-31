package cn.gaifan.douyinOperations.contract.mcp;

public record McpToolDescriptor(
        String toolCode,
        String name,
        String requiredProductCode,
        String requiredFeatureCode,
        String rateLimitHint,
        boolean auditRequired
) {
}
