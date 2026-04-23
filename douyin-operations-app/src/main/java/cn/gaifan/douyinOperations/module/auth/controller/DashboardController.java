package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.constant.RoleCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.auth.repository.AuthLoginLogRepository;
import cn.gaifan.douyinOperations.module.live.repository.LiveSessionRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrgMemberRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthOrganizationRepository;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.system.repository.SysApiCallLogRepository;
import cn.gaifan.douyinOperations.module.system.repository.SysSyncLogRepository;
import cn.gaifan.douyinOperations.module.system.service.SystemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dashboard 数据聚合接口：管理员 / 机构 / 达人 三个 Dashboard
 */
@RestController("authDashboardController")
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Dashboard", description = "三种角色的 Dashboard 数据聚合")
public class DashboardController {

    @Resource
    private AuthUserRepository authUserRepository;
    @Resource
    private AuthOrganizationRepository authOrganizationRepository;
    @Resource
    private AuthOrgMemberRepository authOrgMemberRepository;
    @Resource
    private LiveSessionRepository liveSessionRepository;
    @Autowired(required = false)
    private AuthLoginLogRepository authLoginLogRepository;
    @Autowired(required = false)
    private SystemService systemService;
    @Autowired(required = false)
    private SysApiCallLogRepository sysApiCallLogRepository;
    @Autowired(required = false)
    private SysSyncLogRepository sysSyncLogRepository;

    @PostMapping("/admin")
    @Operation(summary = "管理员 Dashboard 数据")
    public RESTResult<Map<String, Object>> adminDashboard(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!RoleCode.ADMIN.equals(roleCode)) return RESTResult.error(ErrorCode.FORBIDDEN, "无权限");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userCount", authUserRepository.count());
        data.put("orgCount", authOrganizationRepository.count());

        long talentCount = authUserRepository.count((root, query, cb) ->
                cb.and(cb.equal(root.get("roleCode"), RoleCode.TALENT), cb.equal(root.get("deleted"), 0)));
        data.put("talentCount", talentCount);

        long apiLogCount = sysApiCallLogRepository != null ? sysApiCallLogRepository.count() : 0;
        data.put("apiLogCount", apiLogCount);

        long syncLogCount = sysSyncLogRepository != null ? sysSyncLogRepository.count() : 0;
        data.put("syncLogCount", syncLogCount);

        long loginLogCount = authLoginLogRepository != null ? authLoginLogRepository.count() : 0;
        data.put("loginLogCount", loginLogCount);

        data.put("aiCallCount", apiLogCount);

        if (systemService != null) {
            try {
                Map<String, Object> health = systemService.checkHealth();
                data.put("systemOverall", health.get("_overall"));
            } catch (Exception ignored) {
                data.put("systemOverall", "UNKNOWN");
            }
            try {
                Map<String, Object> apiStats = systemService.getApiLogStats(null, null, null);
                data.put("apiTotalCalls", apiStats.get("totalCalls"));
                data.put("apiSuccessRate", apiStats.get("successRate"));
            } catch (Exception ignored) {
            }
        }

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/org")
    @Operation(summary = "机构 Dashboard 数据")
    public RESTResult<Map<String, Object>> orgDashboard(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!RoleCode.INSTITUTION.equals(roleCode)) return RESTResult.error(ErrorCode.FORBIDDEN, "无权限");

        Map<String, Object> data = new LinkedHashMap<>();

        // 旗下达人数
        var memberIds = authOrgMemberRepository.findMemberUserIdsByOrgOwnerId(userId);
        data.put("talentCount", memberIds.size());
        data.put("videoCount", 0);  // TODO: 接入短视频统计
        data.put("liveCount", memberIds.isEmpty() ? 0 : liveSessionRepository.countByUserIdInAndDeleted(memberIds, 0));
        data.put("copyCount", 0);   // TODO: 接入文案统计
        data.put("talentRanking", java.util.Collections.emptyList());
        data.put("pendingCopies", java.util.Collections.emptyList());

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/talent")
    @Operation(summary = "达人 Dashboard 数据")
    public RESTResult<Map<String, Object>> talentDashboard(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!RoleCode.TALENT.equals(roleCode)) return RESTResult.error(ErrorCode.FORBIDDEN, "无权限");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("videoCount", 0);       // TODO: 接入短视频统计
        data.put("liveCount", 0);        // TODO: 接入直播统计
        data.put("followerCount", 0);    // TODO: 接入粉丝统计
        data.put("interactionCount", 0); // TODO: 接入互动统计
        data.put("pendingContent", java.util.Collections.emptyList());
        data.put("pendingComments", java.util.Collections.emptyList());

        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
