package cn.gaifan.douyinOperations.contract.product;

/**
 * 产品间能力调用规则。
 *
 * <p>Gaifan Ops 的产品必须可以独立购买、独立授权、独立计费，同时又能在工作流中相互调用。
 * 该规则描述“哪个产品可以调用哪个产品的哪个功能”，用于避免产品之间直接硬编码依赖，
 * 也避免抖音运营把短视频洞察、数字人、短剧等能力重新变成自己的内部子功能。</p>
 */
public record ProductIntegrationRuleSummary(
        String ruleCode,
        String sourceProductCode,
        String targetProductCode,
        String targetFeatureCode,
        String invocationName,
        String scenario,
        String billingPolicy,
        boolean auditRequired,
        boolean enabled
) {
}
