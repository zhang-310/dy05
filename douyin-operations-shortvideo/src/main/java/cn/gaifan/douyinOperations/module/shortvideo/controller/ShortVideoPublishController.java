package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentAuditService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 短视频发布 API
 * 路径：/api/v1/short-video/publish
 */
@RestController
@RequestMapping("/api/v1/short-video/publish")
@Tag(name = "短视频发布", description = "标题生成、AI 审核、发布")
public class ShortVideoPublishController {

    @Resource
    private ShortVideoAiService aiService;
    @Resource
    private ContentAuditService contentAuditService;

    @PostMapping("/generate-title")
    @Operation(summary = "AI 生成标题")
    public RESTResult<Map<String, Object>> generateTitle(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String videoUrl = body.get("videoUrl") instanceof String s ? s : null;
        String script = body.get("script") instanceof String s ? s : null;
        @SuppressWarnings("unchecked")
        List<String> keywords = body.get("keywords") instanceof List<?> k ? (List<String>) k : null;
        Integer count = body.get("count") instanceof Number n ? n.intValue() : 3;
        String copyText = StringUtils.hasText(script) ? script : (StringUtils.hasText(videoUrl) ? "视频内容" : "短视频");
        var vo = new cn.gaifan.douyinOperations.module.shortvideo.vo.AiTitleGenerateVO();
        vo.setCopyText(copyText);
        vo.setCount(count);
        List<String> titles = aiService.generateTitles(vo, userId);
        List<Map<String, Object>> titleList = new ArrayList<>();
        for (int i = 0; i < (titles != null ? titles.size() : 0); i++) {
            titleList.add(Map.of("text", titles.get(i), "score", 0.9 - i * 0.05));
        }
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(Map.of("titles", titleList));
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/ai-review")
    @Operation(summary = "AI 审核（含内容审核）")
    public RESTResult<Map<String, Object>> aiReview(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String videoUrl = body.get("videoUrl") instanceof String s ? s : null;
        String title = body.get("title") instanceof String s ? s : null;
        String cover = body.get("cover") instanceof String s ? s : null;
        ContentAuditService.AuditResult videoResult = contentAuditService != null && StringUtils.hasText(videoUrl)
                ? contentAuditService.auditVideo(videoUrl) : ContentAuditService.AuditResult.pass();
        ContentAuditService.AuditResult coverResult = contentAuditService != null && StringUtils.hasText(cover)
                ? contentAuditService.auditImage(cover) : ContentAuditService.AuditResult.pass();
        ContentAuditService.AuditResult titleResult = contentAuditService != null && StringUtils.hasText(title)
                ? contentAuditService.auditText(title) : ContentAuditService.AuditResult.pass();
        boolean passed = videoResult.passed() && coverResult.passed() && titleResult.passed();
        List<String> issues = new java.util.ArrayList<>();
        if (!videoResult.passed()) issues.addAll(videoResult.issues());
        if (!coverResult.passed()) issues.addAll(coverResult.issues());
        if (!titleResult.passed()) issues.addAll(titleResult.issues());
        List<String> suggestions = passed ? titleResult.suggestions() : List.of();
        if (suggestions.isEmpty()) suggestions = List.of("建议增加字幕，提升观看体验");
        Map<String, Object> data = Map.of(
                "passed", passed,
                "issues", issues,
                "suggestions", suggestions);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/douyin")
    @Operation(summary = "抖音平台发布（需配置抖音开放平台）")
    public RESTResult<Map<String, Object>> publishDouyin(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = Map.of(
                "success", false,
                "error", "需在「系统配置」中配置 DOUYIN_CLIENT_KEY、DOUYIN_CLIENT_SECRET 及 OAuth 授权后使用",
                "platform", "douyin");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/publish")
    @Operation(summary = "发布（待接入抖音开放平台）")
    public RESTResult<Map<String, Object>> publish(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Map<String, Object> data = Map.of(
                "results", List.of(
                        Map.of("platform", "douyin", "success", false, "error", "发布功能待接入抖音开放平台"),
                        Map.of("platform", "weixin-video", "success", false, "error", "待接入")));
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
