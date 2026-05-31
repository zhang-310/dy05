package cn.gaifan.douyinOperations.contract.bff;

/**
 * 客户端指标摘要。
 *
 * <p>用于 BFF 聚合首页顶部指标，避免 App、小程序直接理解多个后端领域模型。</p>
 */
public record ClientMetricSummary(
        // 指标编码。
        String code,
        // 展示名称。
        String name,
        // 展示值，保留字符串形式以兼容金额、百分比和数量。
        String value,
        // 指标说明。
        String description
) {
}
