package cn.gaifan.douyinOperations.contract.credit;

import java.util.List;

/**
 * 积分治理总览。
 *
 * <p>开放 API、MCP 智能体、Web、App、小程序和企业智能体都应展示同一套账户、
 * 定价规则和账本流水，避免未来多端、多产品商业化时出现多套余额。</p>
 */
public record CreditGovernanceOverview(
        String tenantId,
        CreditAccountSummary account,
        List<CreditQuoteResult> pricingRules,
        List<CreditLedgerEntrySummary> recentLedger,
        List<String> governanceChecklist
) {
}
