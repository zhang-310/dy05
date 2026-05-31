package cn.gaifan.douyinOperations.contract.openapi;

import cn.gaifan.douyinOperations.contract.credit.CreditConsumeResult;

import java.time.OffsetDateTime;

/**
 * OpenAPI 签名调用结果。
 *
 * <p>该结果把身份解析、签名验签、积分扣减、业务能力输出和审计 ID 聚合到一起，
 * 方便开发者控制台、企业智能体控制台和财务台账统一追踪。</p>
 */
public record OpenApiSignedCallResult<T>(
        // 是否允许执行。
        boolean allowed,
        // 决策编码，例如 ALLOW、SIGNATURE_INVALID、REPLAY_DETECTED。
        String decisionCode,
        // 决策说明，面向开发者可读。
        String message,
        // 解析后的 API Key 身份。
        ApiKeyResolveResult apiKey,
        // 积分消费结果；签名失败或授权失败时为空。
        CreditConsumeResult creditConsumeResult,
        // 业务能力输出。
        T payload,
        // 成功调用对应的统一用量台账 ID；拒绝路径为空。
        String usageLedgerId,
        // 审计事件 ID。
        String auditId,
        // 调用链路 ID。
        String traceId,
        // 创建时间。
        OffsetDateTime createdAt
) {
}
