package cn.gaifan.douyinOperations.module.messaging.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.messaging.service.MessagingPlatformService;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSearchVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigSaveVO;
import cn.gaifan.douyinOperations.module.messaging.vo.MsgPlatformConfigVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/messaging")
@Tag(name = "企微/飞书接入", description = "入站 Webhook 配置管理（需登录）")
public class MessagingController {

    @Resource
    private MessagingPlatformService messagingPlatformService;

    @PostMapping("/config/list")
    @Operation(summary = "配置列表（分页）")
    public RESTResult<PageResultVO<MsgPlatformConfigVO>> list(HttpServletRequest request,
            @RequestBody(required = false) MsgPlatformConfigSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new MsgPlatformConfigSearchVO();
        vo.setOwnerId(userId);
        RESTResult<PageResultVO<MsgPlatformConfigVO>> r = RESTResult.getSuccess(messagingPlatformService.search(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/config/get")
    @Operation(summary = "配置详情")
    public RESTResult<MsgPlatformConfigVO> get(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        // P1-1: IDOR 防护 - 传入 userId 校验所有权
        RESTResult<MsgPlatformConfigVO> r = RESTResult.getSuccess(messagingPlatformService.getById(id, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/config/save")
    @Operation(summary = "新增/更新配置")
    public RESTResult<Long> save(HttpServletRequest request, @Valid @RequestBody MsgPlatformConfigSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        // P1-1: IDOR 防护 - 传入 userId 校验所有权
        RESTResult<Long> r = RESTResult.addSuccess(messagingPlatformService.save(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/config/delete")
    @Operation(summary = "删除配置")
    public RESTResult<Void> delete(HttpServletRequest request, @RequestParam Long id) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        // P1-1: IDOR 防护 - 传入 userId 校验所有权
        messagingPlatformService.delete(id, userId);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
