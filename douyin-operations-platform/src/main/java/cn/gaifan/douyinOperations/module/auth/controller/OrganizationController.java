package cn.gaifan.douyinOperations.module.auth.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.constant.RoleCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.auth.entity.AuthOrgMember;
import cn.gaifan.douyinOperations.module.auth.entity.AuthOrganization;
import cn.gaifan.douyinOperations.module.auth.entity.AuthUser;
import cn.gaifan.douyinOperations.module.auth.repository.AuthUserRepository;
import cn.gaifan.douyinOperations.module.auth.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 机构管理：机构 CRUD、成员邀请、达人搜索
 */
@RestController
@RequestMapping("/api/v1/organization")
@Tag(name = "机构管理 / Organization", description = "机构 CRUD、成员管理、邀请达人")
public class OrganizationController {

    @Resource
    private OrganizationService organizationService;
    @Resource
    private AuthUserRepository authUserRepository;

    @PostMapping("/my")
    @Operation(summary = "获取我的机构")
    public RESTResult<Map<String, Object>> getMyOrg(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        AuthOrganization org = organizationService.getOrgByOwnerId(userId);
        if (org == null) return RESTResult.getSuccess(null);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", org.getId());
        data.put("orgName", org.getOrgName());
        data.put("orgCode", org.getOrgCode());
        data.put("contactName", org.getContactName());
        data.put("contactPhone", org.getContactPhone());
        data.put("status", org.getStatus());
        data.put("ownerId", org.getOwnerId());
        data.put("createTime", org.getCreateTime());
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/create")
    @Operation(summary = "创建机构")
    public RESTResult<Map<String, Object>> createOrg(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        String roleCode = AuthTokenFilter.getRoleCode(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (!RoleCode.INSTITUTION.equals(roleCode)) return RESTResult.error(ErrorCode.FORBIDDEN, "仅机构角色可创建");

        AuthOrganization org = organizationService.createOrg(userId,
                body.get("orgName"), body.get("orgCode"),
                body.get("contactName"), body.get("contactPhone"));

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", org.getId());
        data.put("orgName", org.getOrgName());
        RESTResult<Map<String, Object>> r = RESTResult.success("创建成功", data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/update")
    @Operation(summary = "更新机构信息")
    public RESTResult<Void> updateOrg(HttpServletRequest request, @RequestBody Map<String, String> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        AuthOrganization org = organizationService.getOrgByOwnerId(userId);
        if (org == null) return RESTResult.error(ErrorCode.DATA_NOT_FOUND, "机构不存在");

        organizationService.updateOrg(org.getId(), userId,
                body.get("orgName"), body.get("contactName"), body.get("contactPhone"));
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/members")
    @Operation(summary = "获取机构成员列表")
    public RESTResult<List<Map<String, Object>>> getMembers(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        AuthOrganization org = organizationService.getOrgByOwnerId(userId);
        if (org == null) return RESTResult.getSuccess(Collections.emptyList());

        List<AuthOrgMember> members = organizationService.getMembers(org.getId());
        List<Map<String, Object>> result = members.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("orgId", m.getOrgId());
            map.put("userId", m.getUserId());
            map.put("roleInOrg", m.getRoleInOrg());
            map.put("status", m.getStatus());
            map.put("invitedAt", m.getInvitedAt());
            map.put("joinedAt", m.getJoinedAt());
            // 补充用户信息
            authUserRepository.findById(m.getUserId()).ifPresent(u -> {
                map.put("username", u.getUsername());
                map.put("nickname", u.getNickname());
            });
            return map;
        }).collect(Collectors.toList());

        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/invite")
    @Operation(summary = "邀请达人加入机构")
    public RESTResult<Void> invite(HttpServletRequest request, @RequestBody Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        AuthOrganization org = organizationService.getOrgByOwnerId(userId);
        if (org == null) return RESTResult.error(ErrorCode.DATA_NOT_FOUND, "请先创建机构");

        organizationService.inviteTalent(org.getId(), userId, body.get("userId"));
        RESTResult<Void> r = RESTResult.success("邀请已发送", null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/remove")
    @Operation(summary = "移除机构成员")
    public RESTResult<Void> remove(HttpServletRequest request, @RequestBody Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        AuthOrganization org = organizationService.getOrgByOwnerId(userId);
        if (org == null) return RESTResult.error(ErrorCode.DATA_NOT_FOUND, "机构不存在");

        organizationService.removeMember(org.getId(), userId, body.get("userId"));
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/invitations")
    @Operation(summary = "获取达人的待处理邀请")
    public RESTResult<List<Map<String, Object>>> getInvitations(HttpServletRequest request,
            @RequestBody(required = false) Map<String, Object> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        List<AuthOrgMember> pending = organizationService.getPendingInvitations(userId);
        List<Map<String, Object>> result = pending.stream().map(m -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", m.getId());
            map.put("orgId", m.getOrgId());
            map.put("invitedAt", m.getInvitedAt());
            try {
                AuthOrganization org = organizationService.getOrgById(m.getOrgId());
                map.put("orgName", org.getOrgName());
            } catch (Exception e) {
                map.put("orgName", "未知机构");
            }
            return map;
        }).collect(Collectors.toList());

        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/invitation/accept")
    @Operation(summary = "达人接受邀请")
    public RESTResult<Void> acceptInvitation(HttpServletRequest request, @RequestBody Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        organizationService.acceptInvitation(body.get("memberId"), userId);
        RESTResult<Void> r = RESTResult.success("已加入机构", null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/invitation/reject")
    @Operation(summary = "达人拒绝邀请")
    public RESTResult<Void> rejectInvitation(HttpServletRequest request, @RequestBody Map<String, Long> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");

        organizationService.rejectInvitation(body.get("memberId"), userId);
        RESTResult<Void> r = RESTResult.success("已拒绝邀请", null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/search-talents")
    @Operation(summary = "搜索达人（用于邀请）")
    public RESTResult<List<Map<String, Object>>> searchTalents(HttpServletRequest request,
            @RequestBody(required = false) Map<String, String> body) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String keyword = body != null ? body.get("keyword") : null;
        if (keyword == null || keyword.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "缺少 keyword");

        // 简单搜索：按用户名或手机号模糊匹配 talent 角色用户
        List<AuthUser> users = authUserRepository.findAll((root, query, cb) -> cb.and(
                cb.equal(root.get("roleCode"), RoleCode.TALENT),
                cb.equal(root.get("deleted"), 0),
                cb.or(
                        cb.like(root.get("username"), "%" + keyword + "%"),
                        cb.like(cb.coalesce(root.get("mobile"), ""), "%" + keyword + "%"),
                        cb.like(cb.coalesce(root.get("nickname"), ""), "%" + keyword + "%")
                )
        ));

        List<Map<String, Object>> result = users.stream().limit(20).map(u -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", u.getId());
            map.put("username", u.getUsername());
            map.put("nickname", u.getNickname());
            map.put("mobile", u.getMobile());
            return map;
        }).collect(Collectors.toList());

        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
