package cn.gaifan.douyinOperations.contract.bff;

import java.util.List;

/**
 * 客户端首页聚合总览。
 *
 * <p>这是 Web/App/MiniApp 共用的只读 BFF 合同，聚合产品入口、商业指标、AI/MCP 状态和下一步动作。
 * 它不替代领域接口，只减少端侧启动时的多接口请求。</p>
 */
public record ClientDashboardOverview(
        // 当前客户端入口。
        ClientSurface surface,
        // 当前租户。
        String tenantId,
        // 当前用户。
        String userId,
        // 产品入口列表。
        List<ClientProductEntry> products,
        // 首页指标列表。
        List<ClientMetricSummary> metrics,
        // AI 可用 Prompt 数量。
        int aiPromptCount,
        // MCP 工具数量。
        int mcpToolCount,
        // 素材数量。
        int assetCount,
        // 进行中工作流任务数量。
        int activeWorkflowTaskCount,
        // 端侧下一步建议。
        List<String> nextActions
) {
}
