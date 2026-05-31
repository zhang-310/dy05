package cn.gaifan.douyinOperations.module.dashboard.service;

import cn.gaifan.douyinOperations.contract.role.OperationRole;
import cn.gaifan.douyinOperations.contract.role.RoleDashboardProvider;
import cn.gaifan.douyinOperations.module.live.service.GmvTrackingService;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.time.LocalDate;
import java.util.Map;

/**
 * 角色专属 Dashboard 实现
 *
 * Vision: 6 角色各看到不同的首页数据
 */
@Service
public class RoleDashboardServiceImpl implements RoleDashboardProvider {

    @Resource
    private GmvTrackingService gmvTrackingService;

    @Override
    public Object buildDashboard(String roleCode, Long userId) {
        OperationRole role = OperationRole.fromCode(roleCode);

        // 公共数据
        var topSessions = gmvTrackingService.getTopSessionsByGmv(5);
        long totalGmv = topSessions.stream().mapToLong(s -> s.netGmv()).sum();

        return switch (role) {
            case ADMIN -> Map.of(
                    "type", "admin",
                    "totalGmv", totalGmv,
                    "topSessions", topSessions.stream().limit(5).toList(),
                    "pendingApprovals", 0, // TODO: DB query
                    "activeSessions", 0
            );
            case OPERATIONS -> Map.of(
                    "type", "operations",
                    "todaySessions", 0,
                    "pendingScripts", 0,
                    "pendingProducts", 0
            );
            case ANCHOR -> Map.of(
                    "type", "anchor",
                    "mySessions", 0,
                    "myScripts", 0,
                    "upcomingSchedule", java.util.Collections.emptyList()
            );
            case DIRECTOR -> Map.of(
                    "type", "director",
                    "liveControlPanel", Map.of("sessionId", 0, "status", "idle"),
                    "realtimeData", Map.of("viewers", 0, "gmv", 0)
            );
            case PHOTOGRAPHER -> Map.of(
                    "type", "photographer",
                    "todayTasks", 0,
                    "materialsDue", 0
            );
            case SELECTOR -> Map.of(
                    "type", "selector",
                    "pendingSelections", 0,
                    "competitorProducts", java.util.Collections.emptyList()
            );
        };
    }

    @Override
    public DailyBriefing buildDailyBriefing() {
        var topSessions = gmvTrackingService.getTopSessionsByGmv(1);
        return new DailyBriefing(
                LocalDate.now().toString(),
                0, // todaySessions - DB query
                topSessions.isEmpty() ? 0 : topSessions.get(0).netGmv(),
                0, // pendingApprovals
                "", // topVideo
                ""  // urgent
        );
    }
}
