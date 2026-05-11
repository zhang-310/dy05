package cn.gaifan.douyinOperations.module.douyin.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.douyin.entity.DyPersona;
import cn.gaifan.douyinOperations.module.douyin.service.DouyinPersonaService;
import cn.gaifan.douyinOperations.module.douyin.vo.PersonaByAccountQueryVO;
import cn.gaifan.douyinOperations.module.douyin.vo.PersonaSaveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 人设管理控制器
 */
@RestController
@RequestMapping("/api/v1/douyin/persona")
@Tag(name = "人设管理 / Persona Management", description = "抖音人设管理")
public class DouyinPersonaController {

    @Resource
    private DouyinPersonaService personaService;

    /**
     * 保存或更新人设
     */
    @PostMapping("/save")
    @Operation(summary = "保存人设 / Save Persona")
    public RESTResult<Long> save(@RequestBody PersonaSaveVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        Long id = personaService.savePersona(vo, userId);
        RESTResult<Long> r = RESTResult.getSuccess(id);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取人设列表
     */
    @PostMapping("/list")
    @Operation(summary = "人设列表 / List Personas")
    public RESTResult<List<DyPersona>> list(@RequestParam(required = false) String personaType,
                                            HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        List<DyPersona> list = personaService.listPersonas(userId, personaType);
        RESTResult<List<DyPersona>> r = RESTResult.getSuccess(list);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取人设详情
     */
    @PostMapping("/get")
    @Operation(summary = "人设详情 / Get Persona")
    public RESTResult<DyPersona> get(@RequestParam Long id, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        DyPersona persona = personaService.getPersona(id, userId);
        RESTResult<DyPersona> r = RESTResult.getSuccess(persona);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 删除人设
     */
    @PostMapping("/delete")
    @Operation(summary = "删除人设 / Delete Persona")
    public RESTResult<Void> delete(@RequestParam Long id, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        personaService.deletePersona(id, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 设置默认人设
     */
    @PostMapping("/set-default")
    @Operation(summary = "设置默认人设 / Set Default Persona")
    public RESTResult<Void> setDefault(@RequestParam Long id, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        personaService.setDefaultPersona(id, userId);
        RESTResult<Void> r = RESTResult.getSuccess(null);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取默认人设
     */
    @PostMapping("/get-default")
    @Operation(summary = "获取默认人设 / Get Default Persona")
    public RESTResult<DyPersona> getDefault(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        DyPersona persona = personaService.getDefaultPersona(userId);
        RESTResult<DyPersona> r = RESTResult.getSuccess(persona);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 获取系统模板
     */
    @PostMapping("/templates")
    @Operation(summary = "系统模板 / System Templates")
    public RESTResult<List<DyPersona>> templates(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        List<DyPersona> templates = personaService.getSystemTemplates();
        RESTResult<List<DyPersona>> r = RESTResult.getSuccess(templates);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    /**
     * 根据账号ID获取人设（1:1关系）
     */
    @PostMapping("/get-by-account")
    @Operation(summary = "按账号获取人设 / Get Persona By Account")
    public RESTResult<DyPersona> getByAccount(@Valid @RequestBody PersonaByAccountQueryVO vo, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        }

        DyPersona persona = personaService.getPersonaByAccountId(vo.getAccountId(), userId);
        RESTResult<DyPersona> r = RESTResult.getSuccess(persona);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
