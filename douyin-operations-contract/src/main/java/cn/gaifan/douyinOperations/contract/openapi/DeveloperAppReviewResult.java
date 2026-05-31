package cn.gaifan.douyinOperations.contract.openapi;

import java.util.List;

/**
 * 开发者应用审核结果。
 *
 * <p>结果返回应用摘要和本次审核产生的风险提示，前端据此展示是否已满足上线准入要求。</p>
 */
public record DeveloperAppReviewResult(
        DeveloperAppSummary app,
        List<String> riskHints,
        String auditId
) {
}
