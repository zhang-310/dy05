package cn.gaifan.douyinOperations.contract.openapi;

import cn.gaifan.douyinOperations.contract.bff.OpenApiCapabilitySummary;
import cn.gaifan.douyinOperations.contract.credit.CreditGovernanceOverview;
import cn.gaifan.douyinOperations.contract.governance.UsageLedgerEntry;

import java.util.List;

/**
 * 开放 API 开发者总览。
 *
 * <p>用于开发者中控台展示租户下应用、API Key、开放能力和治理规则。
 * 它是未来外部系统调用 MCP/video insight 的入口视图。</p>
 */
public record OpenApiDeveloperOverview(
        String tenantId,
        List<DeveloperAppSummary> apps,
        List<ApiKeySummary> apiKeys,
        List<OpenApiCapabilitySummary> capabilities,
        CreditGovernanceOverview creditGovernance,
        List<UsageLedgerEntry> recentUsageLedger,
        String authMode,
        String signatureMode,
        List<String> governanceChecklist
) {
}
