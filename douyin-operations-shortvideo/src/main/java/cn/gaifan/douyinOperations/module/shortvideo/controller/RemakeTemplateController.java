package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.annotation.CurrentUserId;
import cn.gaifan.douyinOperations.common.vo.PageResultVO;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvRemakeTemplate;
import cn.gaifan.douyinOperations.module.shortvideo.service.RemakeTemplateService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSearchVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.RemakeTemplateSaveVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import java.util.Map;

/**
 * 二创模板 Controller（Phase 5）
 */
@RestController
@RequestMapping("/api/v1/short-video/remake-template")
@Tag(name = "短视频 / 二创模板", description = "爆款二创模板管理")
public class RemakeTemplateController {

    @Resource
    private RemakeTemplateService remakeTemplateService;

    @PostMapping("/list")
    @Operation(summary = "分页查询二创模板")
    public RESTResult<PageResultVO<SvRemakeTemplate>> list(@RequestBody(required = false) RemakeTemplateSearchVO vo, @CurrentUserId Long userId) {
        if (vo == null) vo = new RemakeTemplateSearchVO();
        RESTResult<PageResultVO<SvRemakeTemplate>> r = RESTResult.getSuccess(remakeTemplateService.search(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/save")
    @Operation(summary = "新增/更新二创模板")
    public RESTResult<SvRemakeTemplate> save(@RequestBody @Valid RemakeTemplateSaveVO vo, @CurrentUserId Long userId) {
        RESTResult<SvRemakeTemplate> r = RESTResult.addSuccess(remakeTemplateService.save(vo, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/delete")
    @Operation(summary = "删除二创模板")
    public RESTResult<Void> delete(@RequestBody Map<String, Long> body, @CurrentUserId Long userId) {
        Long id = body != null ? body.get("id") : null;
        if (id == null) return RESTResult.error(400, "id不能为空");
        remakeTemplateService.delete(id, userId);
        RESTResult<Void> r = RESTResult.success();
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/create-from-viral")
    @Operation(summary = "从爆款分析结果生成模板")
    public RESTResult<SvRemakeTemplate> createFromViral(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long viralVideoId = body != null && body.get("viralVideoId") != null ? ((Number) body.get("viralVideoId")).longValue() : null;
        String remakeType = body != null && body.get("remakeType") != null ? body.get("remakeType").toString() : "form_copy";
        if (viralVideoId == null) return RESTResult.error(400, "viralVideoId不能为空");
        RESTResult<SvRemakeTemplate> r = RESTResult.addSuccess(remakeTemplateService.createFromViralAnalysis(viralVideoId, remakeType, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate")
    @Operation(summary = "基于模板+变量生成脚本")
    public RESTResult<String> generate(@RequestBody Map<String, Object> body, @CurrentUserId Long userId) {
        Long templateId = body != null && body.get("templateId") != null ? ((Number) body.get("templateId")).longValue() : null;
        @SuppressWarnings("unchecked")
        Map<String, String> variables = body != null && body.get("variables") != null ? (Map<String, String>) body.get("variables") : Map.of();
        if (templateId == null) return RESTResult.error(400, "templateId不能为空");
        RESTResult<String> r = RESTResult.getSuccess(remakeTemplateService.generateFromTemplate(templateId, variables, userId));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
