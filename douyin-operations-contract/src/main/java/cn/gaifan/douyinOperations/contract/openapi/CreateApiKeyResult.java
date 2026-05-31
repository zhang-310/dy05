package cn.gaifan.douyinOperations.contract.openapi;

/**
 * 创建 OpenAPI API Key 结果。
 *
 * <p>plainApiKey 和 plainApiSecret 只允许在本次响应里出现一次。
 * 数据库、列表接口、日志和审计都只能保存哈希、密文或脱敏摘要。</p>
 */
public record CreateApiKeyResult(
        // 可长期展示的脱敏摘要。
        ApiKeySummary summary,
        // 一次性展示的 API Key 明文，用于请求头 X-Gaifan-Api-Key。
        String plainApiKey,
        // 一次性展示的 API Secret 明文，用于生成 HMAC-SHA256 签名。
        String plainApiSecret,
        // 安全提示，提醒调用方立即保存。
        String warning
) {
}
