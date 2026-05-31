package cn.gaifan.douyinOperations.contract.openapi;

import java.util.Map;

/**
 * OpenAPI 签名调用请求。
 *
 * <p>外部开发者、企业智能体和第三方系统都必须使用同一套签名模型进入 Gaifan Ops。
 * 请求体只承载业务参数；身份、时间戳、nonce 和签名放在 HTTP Header，避免把安全字段混进业务参数。</p>
 */
public record OpenApiSignedCallRequest(
        // 调用能力编码，当前支持 video.find_similar、video.report_export、video.batch_report_export 和 brief.generate_imitation，能力目录映射到 MCP 工具注册表。
        String capabilityCode,
        // 用户或外部系统侧用户标识，用于积分账本和审计，不等同于平台登录用户。
        String userId,
        // 企业或个人智能体 ID，用于区分不同 Agent 的调用成本和行为审计。
        String agentId,
        // 业务参数，必须是合规来源数据，不允许传 Cookie、绕过字段或未授权采集参数。
        Map<String, String> arguments,
        // 调用链路 ID；为空时服务端会生成。
        String traceId
) {
}
