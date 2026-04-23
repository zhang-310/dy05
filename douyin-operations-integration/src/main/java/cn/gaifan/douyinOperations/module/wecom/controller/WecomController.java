package cn.gaifan.douyinOperations.module.wecom.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.wecom.service.impl.WecomServiceImpl;
import cn.gaifan.douyinOperations.module.wecom.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wecom")
@Tag(name = "企业微信 / WeCom", description = "企业微信机器人管理（需登录）")
public class WecomController {

    @Resource
    private WecomServiceImpl wecomService;

    // ==================== 机器人管理 ====================

    @PostMapping("/robot/list")
    @Operation(summary = "机器人列表（分页）")
    public RESTResult<PageResultVO<WcRobotConfigVO>> robotList(HttpServletRequest request,
            @RequestBody(required = false) WcRobotSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new WcRobotSearchVO();
        vo.setOwnerId(userId);
        RESTResult<PageResultVO<WcRobotConfigVO>> r = RESTResult.getSuccess(wecomService.searchRobots(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/robot/get")
    @Operation(summary = "机器人详情")
    public RESTResult<WcRobotConfigVO> robotGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<WcRobotConfigVO> r = RESTResult.getSuccess(wecomService.getRobotById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/robot/save")
    @Operation(summary = "新增/更新机器人")
    public RESTResult<Long> robotSave(HttpServletRequest request, @Valid @RequestBody WcRobotConfigSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(wecomService.saveRobot(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/robot/delete")
    @Operation(summary = "删除机器人")
    public RESTResult<Void> robotDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        wecomService.deleteRobot(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/robot/update-status")
    @Operation(summary = "启用/禁用机器人")
    public RESTResult<Void> robotStatus(HttpServletRequest request,
            @RequestParam Long id, @RequestParam Integer status) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        wecomService.updateRobotStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 推送规则 ====================

    @PostMapping("/rule/list")
    @Operation(summary = "推送规则列表")
    public RESTResult<List<WcPushRuleVO>> ruleList(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<List<WcPushRuleVO>> r = RESTResult.getSuccess(wecomService.listRules(userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/rule/get")
    @Operation(summary = "推送规则详情")
    public RESTResult<WcPushRuleVO> ruleGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<WcPushRuleVO> r = RESTResult.getSuccess(wecomService.getRuleById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/rule/save")
    @Operation(summary = "新增/更新推送规则")
    public RESTResult<Long> ruleSave(HttpServletRequest request, @Valid @RequestBody WcPushRuleSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(wecomService.saveRule(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/rule/delete")
    @Operation(summary = "删除推送规则")
    public RESTResult<Void> ruleDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        wecomService.deleteRule(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/rule/update-status")
    @Operation(summary = "启用/禁用推送规则")
    public RESTResult<Void> ruleStatus(HttpServletRequest request,
            @RequestParam Long id, @RequestParam Integer status) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        wecomService.updateRuleStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 消息日志 ====================

    @PostMapping("/log/list")
    @Operation(summary = "消息日志列表（分页）")
    public RESTResult<PageResultVO<WcMessageLogVO>> logList(HttpServletRequest request,
            @RequestBody(required = false) WcMessageLogSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new WcMessageLogSearchVO();
        vo.setOwnerId(userId);
        RESTResult<PageResultVO<WcMessageLogVO>> r = RESTResult.getSuccess(wecomService.searchLogs(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 手动推送 ====================

    @PostMapping("/push")
    @Operation(summary = "手动推送消息")
    public RESTResult<Void> push(HttpServletRequest request, @Valid @RequestBody WcSendMessageVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        wecomService.sendMessage(vo, userId);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
