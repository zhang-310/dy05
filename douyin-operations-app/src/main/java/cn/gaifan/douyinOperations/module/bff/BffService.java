package cn.gaifan.douyinOperations.module.bff;

import cn.gaifan.douyinOperations.common.config.RequestIdentityHolder;
import cn.gaifan.douyinOperations.contract.bff.BffDashboardVO;
import cn.gaifan.douyinOperations.contract.identity.IdentityContext;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * BFF 聚合服务
 *
 * 聚合多域数据用于前端仪表盘。
 * 每个数据源对应一个独立域，后续可拆分为独立微服务调用。
 */
@Service
public class BffService {

    /**
     * 构建概览仪表盘数据
     *
     * 当前为骨架实现，各数据源逐步接入：
     * - 用户画像 → identity 域
     * - 平台概览 → platform 域
     * - AI 用量   → ai-mcp 域
     * - 最近活动  → content 域
     */
    public BffDashboardVO buildDashboard(IdentityContext ctx) {
        // TODO: 逐步替换为实际服务调用
        Map<String, Object> profile = Map.of(
                "userId", ctx.userId(),
                "tenantId", ctx.tenantId(),
                "roleCode", ctx.roleCode()
        );

        return new BffDashboardVO(
                profile,
                Map.of("productCount", 0, "activeUsers", 0),
                Map.of("totalCalls", 0, "totalCost", 0),
                Map.of("recentActions", java.util.Collections.emptyList())
        );
    }
}
