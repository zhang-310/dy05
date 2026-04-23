package cn.gaifan.douyinOperations.module.sms.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.sms.service.SmsService;
import cn.gaifan.douyinOperations.module.sms.vo.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/sms")
@Tag(name = "短信服务 / SMS", description = "短信服务商配置、模板、发送日志、验证码管理（需登录）")
public class SmsController {

    @Resource
    private SmsService smsService;

    // ==================== 服务商配置 ====================

    @PostMapping("/provider/list")
    @Operation(summary = "服务商列表（分页）")
    public RESTResult<PageResultVO<SmsProviderConfigVO>> providerList(HttpServletRequest request,
            @RequestBody(required = false) SmsProviderConfigSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SmsProviderConfigSearchVO();
        vo.setOwnerId(userId);
        RESTResult<PageResultVO<SmsProviderConfigVO>> r = RESTResult.getSuccess(smsService.searchProviderConfigs(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/provider/get")
    @Operation(summary = "服务商配置详情")
    public RESTResult<SmsProviderConfigVO> providerGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<SmsProviderConfigVO> r = RESTResult.getSuccess(smsService.getProviderConfigById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/provider/save")
    @Operation(summary = "新增/更新服务商配置")
    public RESTResult<Long> providerSave(HttpServletRequest request, @Valid @RequestBody SmsProviderConfigSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(smsService.saveProviderConfig(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/provider/delete")
    @Operation(summary = "删除服务商配置")
    public RESTResult<Void> providerDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        smsService.deleteProviderConfig(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/provider/update-status")
    @Operation(summary = "启用/禁用服务商配置")
    public RESTResult<Void> providerStatus(HttpServletRequest request,
            @RequestParam Long id, @RequestParam Integer status) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        smsService.updateProviderConfigStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/provider/set-default")
    @Operation(summary = "设置为默认服务商")
    public RESTResult<Void> setDefaultProvider(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        smsService.setDefaultProviderConfig(id);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 短信模板 ====================

    @PostMapping("/template/list")
    @Operation(summary = "模板列表（分页）")
    public RESTResult<PageResultVO<SmsTemplateVO>> templateList(HttpServletRequest request,
            @RequestBody(required = false) SmsTemplateSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SmsTemplateSearchVO();
        vo.setOwnerId(userId);
        RESTResult<PageResultVO<SmsTemplateVO>> r = RESTResult.getSuccess(smsService.searchTemplates(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/template/get")
    @Operation(summary = "模板详情")
    public RESTResult<SmsTemplateVO> templateGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<SmsTemplateVO> r = RESTResult.getSuccess(smsService.getTemplateById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/template/save")
    @Operation(summary = "新增/更新模板")
    public RESTResult<Long> templateSave(HttpServletRequest request, @Valid @RequestBody SmsTemplateSaveVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo.getId() == null) vo.setOwnerId(userId);
        RESTResult<Long> r = RESTResult.addSuccess(smsService.saveTemplate(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/template/delete")
    @Operation(summary = "删除模板")
    public RESTResult<Void> templateDelete(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        smsService.deleteTemplate(id);
        RESTResult<Void> r = RESTResult.deleteSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/template/update-status")
    @Operation(summary = "启用/禁用模板")
    public RESTResult<Void> templateStatus(HttpServletRequest request,
            @RequestParam Long id, @RequestParam Integer status) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        smsService.updateTemplateStatus(id, status);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 短信发送日志 ====================

    @PostMapping("/log/list")
    @Operation(summary = "发送日志列表（分页）")
    public RESTResult<PageResultVO<SmsSendLogVO>> logList(HttpServletRequest request,
            @RequestBody(required = false) SmsSendLogSearchVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        if (vo == null) vo = new SmsSendLogSearchVO();
        vo.setOwnerId(userId);
        RESTResult<PageResultVO<SmsSendLogVO>> r = RESTResult.getSuccess(smsService.searchSendLogs(vo));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/log/get")
    @Operation(summary = "发送日志详情")
    public RESTResult<SmsSendLogVO> logGet(HttpServletRequest request, @RequestParam Long id) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<SmsSendLogVO> r = RESTResult.getSuccess(smsService.getSendLogById(id));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    // ==================== 短信验证码 ====================

    @PostMapping("/code/send")
    @Operation(summary = "发送验证码")
    public RESTResult<Void> sendVerificationCode(HttpServletRequest request,
            @Valid @RequestBody SmsVerificationCodeSendVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        vo.setOwnerId(userId);
        smsService.sendVerificationCode(vo);
        RESTResult<Void> r = RESTResult.addSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/code/verify")
    @Operation(summary = "验证码验证")
    public RESTResult<Void> verifyCode(HttpServletRequest request,
            @Valid @RequestBody SmsVerificationCodeVerifyVO vo) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        vo.setOwnerId(userId);
        smsService.verifyCode(vo);
        RESTResult<Void> r = RESTResult.updateSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/code/get-latest")
    @Operation(summary = "获取最新验证码信息")
    public RESTResult<SmsVerificationCodeVO> getLatestCode(HttpServletRequest request,
            @RequestParam String phoneNumber, @RequestParam String bizType) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        RESTResult<SmsVerificationCodeVO> r = RESTResult.getSuccess(smsService.getLatestVerificationCode(phoneNumber, bizType));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
