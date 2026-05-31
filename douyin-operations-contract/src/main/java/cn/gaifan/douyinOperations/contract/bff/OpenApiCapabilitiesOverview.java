package cn.gaifan.douyinOperations.contract.bff;

import java.util.List;

/**
 * 开放 API 能力总览。
 *
 * <p>用于未来开发者中控台和外部 Agent 发现平台能力。MVP 只返回只读能力目录。</p>
 */
public record OpenApiCapabilitiesOverview(
        // 当前租户。
        String tenantId,
        // 能力目录。
        List<OpenApiCapabilitySummary> capabilities,
        // 治理模式说明。
        String governanceMode,
        // 接入前置条件。
        List<String> requirements
) {
}
