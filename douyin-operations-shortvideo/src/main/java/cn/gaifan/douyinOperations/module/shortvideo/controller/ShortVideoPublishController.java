package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ContentAuditService;
import cn.gaifan.douyinOperations.module.shortvideo.service.DouyinPublishService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoAiService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.vo.DouyinOfficialReferenceVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
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

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    @Resource
    private ShortVideoAiService aiService;
    @Resource
    private ContentAuditService contentAuditService;
    @Resource
    private SvProjectService projectService;
    @Autowired(required = false)
    private DouyinPublishService douyinPublishService;
    @Autowired(required = false)
    private OperationalStrategyKnowledgeService operationalStrategyKnowledgeService;

    @PostMapping("/generate-title")
    @Operation(summary = "AI 生成标题")
    public RESTResult<Map<String, Object>> generateTitle(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        body = safeBody(body);
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
        PublishContext ctx = resolveContext(safeBody(body), userId);
        String videoUrl = ctx.videoUrl();
        String title = ctx.title();
        String cover = ctx.coverUrl();
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
        if (ctx.project() != null) {
            saveProjectPublishState(
                    ctx,
                    null,
                    passed ? "approved" : "needs_revision",
                    String.join("；", passed ? suggestions : issues),
                    false,
                    userId);
        }
        String officialQuery = String.join(" ",
                title != null ? title : "",
                videoUrl != null ? videoUrl : "",
                cover != null ? cover : "",
                "短视频发布审核 成片审核 素材审核 千川素材违规 商品宣传违规 抖音官方规则");
        List<DouyinOfficialReferenceVO> officialReferences = officialRefs(userId, officialQuery, true);
        boolean officialSatisfied = hasViolationRuleRef(officialReferences);
        if (!officialSatisfied) {
            passed = false;
            issues.add("未检索到 douyin_weigui 官方违规规则引用，禁止判定为审核通过");
        }
        Map<String, Object> data = Map.of(
                "passed", passed,
                "issues", issues,
                "suggestions", suggestions,
                "officialReferences", officialReferences,
                "officialReferenceRequired", true,
                "officialReferenceSatisfied", officialSatisfied,
                "officialReferenceStatus", officialSatisfied ? "satisfied" : "missing_douyin_weigui_reference",
                "projectId", ctx.projectId() != null ? ctx.projectId() : 0);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/douyin")
    @Operation(summary = "抖音平台发布（需配置抖音开放平台）")
    public RESTResult<Map<String, Object>> publishDouyin(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        PublishContext ctx = resolveContext(safeBody(body), userId);
        if (!StringUtils.hasText(ctx.videoUrl())) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoUrl 不能为空，或项目未生成成片");
        }
        List<DouyinOfficialReferenceVO> officialReferences = officialRefs(userId, publishGateQuery(ctx), true);
        if (!hasViolationRuleRef(officialReferences)) {
            return RESTResult.error(ErrorCode.COMPLIANCE_VIOLATION,
                    "官方规则引用门禁未通过：发布前必须检索到 douyin_weigui 官方违规规则引用，禁止发布",
                    officialReferenceGateData(officialReferences));
        }
        Map<String, Object> result = publishToPlatform("douyin", ctx.videoUrl(), ctx.title(), userId);
        boolean success = Boolean.TRUE.equals(result.get("success"));
        if (ctx.project() != null) {
            saveProjectPublishState(ctx, List.of("douyin"), success ? "approved" : null,
                    success ? "抖音发布成功" : String.valueOf(result.getOrDefault("error", "")), success, userId);
        }
        Map<String, Object> data = Map.of(
                "success", success,
                "results", List.of(result),
                "platform", "douyin",
                "officialReferences", officialReferences,
                "officialReferenceRequired", true,
                "officialReferenceSatisfied", true,
                "officialReferenceStatus", "satisfied",
                "projectId", ctx.projectId() != null ? ctx.projectId() : 0,
                "degraded", !success);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/publish")
    @Operation(summary = "发布到选择平台")
    public RESTResult<Map<String, Object>> publish(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        PublishContext ctx = resolveContext(safeBody(body), userId);
        if (!StringUtils.hasText(ctx.videoUrl())) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoUrl 不能为空，或项目未生成成片");
        }
        List<DouyinOfficialReferenceVO> officialReferences = officialRefs(userId, publishGateQuery(ctx), true);
        if (!hasViolationRuleRef(officialReferences)) {
            return RESTResult.error(ErrorCode.COMPLIANCE_VIOLATION,
                    "官方规则引用门禁未通过：发布前必须检索到 douyin_weigui 官方违规规则引用，禁止发布",
                    officialReferenceGateData(officialReferences));
        }
        List<Map<String, Object>> results = new ArrayList<>();
        for (String platform : ctx.platforms()) {
            results.add(publishToPlatform(platform, ctx.videoUrl(), ctx.title(), userId));
        }
        boolean success = results.stream().anyMatch(r -> Boolean.TRUE.equals(r.get("success")));
        if (ctx.project() != null) {
            saveProjectPublishState(ctx, ctx.platforms(), success ? "approved" : null,
                    success ? "至少一个平台发布成功" : "平台发布未完成，请查看返回结果", success, userId);
        }
        Map<String, Object> data = Map.of(
                "success", success,
                "results", results,
                "officialReferences", officialReferences,
                "officialReferenceRequired", true,
                "officialReferenceSatisfied", true,
                "officialReferenceStatus", "satisfied",
                "projectId", ctx.projectId() != null ? ctx.projectId() : 0,
                "degraded", !success);
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    private Map<String, Object> safeBody(Map<String, Object> body) {
        return body != null ? body : Map.of();
    }

    private PublishContext resolveContext(Map<String, Object> body, Long userId) {
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        SvProjectVO project = null;
        if (projectId != null && projectId > 0) {
            project = projectService.get(projectId, userId, List.of(userId));
        }
        String videoUrl = firstText(readString(body.get("videoUrl")), project != null ? project.getFinalVideoUrl() : null);
        String title = firstText(readString(body.get("title")),
                project != null ? project.getPublishTitle() : null,
                project != null ? project.getTitle() : null,
                "短视频");
        String coverUrl = firstText(readString(body.get("cover")),
                readString(body.get("coverUrl")),
                project != null ? project.getThumbnailUrl() : null);
        String publishTime = firstText(readString(body.get("publishTime")),
                project != null && project.getPublishTime() != null ? project.getPublishTime().toLocalDateTime().format(DATE_TIME_FMT) : null);
        List<String> platforms = readPlatforms(body.get("platforms"));
        if (platforms.isEmpty() && project != null) {
            platforms = readPlatforms(project.getPublishPlatforms());
        }
        if (platforms.isEmpty()) {
            platforms = List.of("douyin");
        }
        return new PublishContext(projectId, project, videoUrl, title, coverUrl, publishTime, platforms);
    }

    private Map<String, Object> publishToPlatform(String platform, String videoUrl, String title, Long userId) {
        String normalized = StringUtils.hasText(platform) ? platform.trim() : "douyin";
        if ("douyin".equals(normalized)) {
            if (douyinPublishService == null) {
                return Map.of("platform", normalized, "success", false, "error", "抖音发布服务未加载");
            }
            DouyinPublishService.PublishResult result = douyinPublishService.publish(videoUrl, title, userId);
            if (result != null && result.success()) {
                return Map.of("platform", normalized, "success", true, "itemId", result.itemId() != null ? result.itemId() : "");
            }
            return Map.of("platform", normalized, "success", false,
                    "error", result != null && StringUtils.hasText(result.error()) ? result.error() : "抖音发布失败");
        }
        return Map.of("platform", normalized, "success", false, "error", "平台暂未接入真实发布能力");
    }

    private void saveProjectPublishState(PublishContext ctx, List<String> platforms, String reviewStatus,
                                         String reviewComment, boolean published, Long userId) {
        SvProjectVO project = ctx.project();
        if (project == null || ctx.projectId() == null) {
            return;
        }
        SvProjectSaveVO save = new SvProjectSaveVO();
        save.setId(ctx.projectId());
        save.setTitle(project.getTitle());
        save.setProjectType(project.getProjectType());
        save.setPublishTitle(ctx.title());
        save.setPublishPlatforms(toJson(platforms != null ? platforms : ctx.platforms()));
        save.setPublishTime(StringUtils.hasText(ctx.publishTime()) ? ctx.publishTime() : (published ? LocalDateTime.now().format(DATE_TIME_FMT) : null));
        if (StringUtils.hasText(reviewStatus)) {
            save.setReviewStatus(reviewStatus);
            save.setReviewerId(userId);
        }
        if (StringUtils.hasText(reviewComment)) {
            save.setReviewComment(reviewComment);
        }
        if (published) {
            save.setStatus("published");
        }
        projectService.save(save, project.getOwnerId());
    }

    private List<String> readPlatforms(Object raw) {
        List<String> platforms = new ArrayList<>();
        if (raw instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s && StringUtils.hasText(s)) {
                    platforms.add(s.trim());
                }
            }
            return platforms;
        }
        if (raw instanceof String s && StringUtils.hasText(s)) {
            String trimmed = s.trim();
            if (trimmed.startsWith("[")) {
                try {
                    List<?> list = OBJECT_MAPPER.readValue(trimmed, List.class);
                    return readPlatforms(list);
                } catch (Exception ignored) {
                    // 非 JSON 数组时按逗号分隔处理
                }
            }
            for (String item : trimmed.split(",")) {
                if (StringUtils.hasText(item)) {
                    platforms.add(item.trim());
                }
            }
        }
        return platforms;
    }

    private String toJson(List<String> platforms) {
        try {
            return OBJECT_MAPPER.writeValueAsString(platforms != null ? platforms : List.of());
        } catch (Exception ignored) {
            return "[]";
        }
    }

    private String readString(Object value) {
        return value instanceof String s ? s : null;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private List<DouyinOfficialReferenceVO> officialRefs(Long userId, String query, boolean violationOnly) {
        if (operationalStrategyKnowledgeService == null || userId == null || !StringUtils.hasText(query)) {
            return List.of();
        }
        try {
            var context = violationOnly
                    ? operationalStrategyKnowledgeService.buildViolationRuleContext(userId, query, "short_video_publish_check", 1800)
                    : operationalStrategyKnowledgeService.buildShortVideoGenerationContext(userId, query, 1800);
            var ruleContext = violationOnly ? null
                    : operationalStrategyKnowledgeService.buildViolationRuleContext(userId, query, "short_video_publish_check", 1400);
            List<cn.gaifan.douyinOperations.module.ai.service.OperationalStrategyKnowledgeService.OfficialReference> refs =
                    new java.util.ArrayList<>();
            if (context != null && context.officialReferences() != null) {
                refs.addAll(context.officialReferences());
            }
            if (ruleContext != null && ruleContext.officialReferences() != null) {
                refs.addAll(ruleContext.officialReferences());
            }
            return refs.stream()
                    .filter(ref -> !violationOnly || "violation_rule".equals(ref.refType()))
                    .collect(java.util.stream.Collectors.toMap(
                            ref -> String.valueOf(ref.docId()) + ":" + String.valueOf(ref.chunkId()),
                            ref -> ref,
                            (a, b) -> a,
                            java.util.LinkedHashMap::new))
                    .values().stream()
                    .map(ref -> {
                        DouyinOfficialReferenceVO vo = new DouyinOfficialReferenceVO();
                        vo.setKbName(ref.kbName());
                        vo.setRefType(ref.refType());
                        vo.setDocId(ref.docId());
                        vo.setChunkId(ref.chunkId());
                        vo.setTitle(ref.title());
                        vo.setContentPreview(ref.contentPreview());
                        vo.setScore(ref.score());
                        return vo;
                    })
                    .toList();
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private String publishGateQuery(PublishContext ctx) {
        return String.join(" ",
                ctx.title() != null ? ctx.title() : "",
                ctx.videoUrl() != null ? ctx.videoUrl() : "",
                ctx.coverUrl() != null ? ctx.coverUrl() : "",
                String.join(" ", ctx.platforms()),
                "短视频发布前审核 成片发布 千川素材违规 商品宣传违规 抖音官方规则 douyin_weigui");
    }

    private Map<String, Object> officialReferenceGateData(List<DouyinOfficialReferenceVO> officialReferences) {
        return Map.of(
                "officialReferences", officialReferences != null ? officialReferences : List.of(),
                "officialReferenceRequired", true,
                "officialReferenceSatisfied", false,
                "officialReferenceStatus", "missing_douyin_weigui_reference");
    }

    private boolean hasViolationRuleRef(List<DouyinOfficialReferenceVO> refs) {
        if (refs == null || refs.isEmpty()) {
            return false;
        }
        return refs.stream().anyMatch(ref ->
                "violation_rule".equals(ref.getRefType()) || "douyin_weigui".equals(ref.getKbName()));
    }

    private record PublishContext(
            Long projectId,
            SvProjectVO project,
            String videoUrl,
            String title,
            String coverUrl,
            String publishTime,
            List<String> platforms
    ) {}
}
