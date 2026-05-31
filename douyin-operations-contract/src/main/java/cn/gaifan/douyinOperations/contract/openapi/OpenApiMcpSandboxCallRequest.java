package cn.gaifan.douyinOperations.contract.openapi;

import java.util.Map;

/**
 * OpenAPI/MCP 沙箱签名调用请求。
 *
 * <p>沙箱由服务端使用受控演示 Key 生成签名并完成一次真实治理链路调用。
 * 前端不能提交 API Secret，避免把密钥材料放入浏览器或日志。</p>
 */
public record OpenApiMcpSandboxCallRequest(
        // 租户 ID，默认 demo-tenant。
        String tenantId,
        // 能力编码，默认 brief.generate_imitation。
        String capabilityCode,
        // 外部业务用户，用于积分、用量和审计台账。
        String userId,
        // 外部 Agent ID，用于区分自动化调用来源。
        String agentId,
        // 合规业务参数，只允许 mock、用户提交或授权来源数据。
        Map<String, String> arguments
) {
}
