package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.CinematicKnowledgeService;
import cn.gaifan.douyinOperations.module.ai.service.VideoQualityScoreService;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoMaterialService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 短视频素材生产 API
 * 路径：/api/v1/short-video/material
 */
@RestController
@RequestMapping("/api/v1/short-video/material")
@Tag(name = "短视频素材", description = "关键帧、配音、图生视频")
public class ShortVideoMaterialController {

    private static final Logger log = LoggerFactory.getLogger(ShortVideoMaterialController.class);

    @Resource
    private ShortVideoMaterialService materialService;
    @Resource
    private CinematicKnowledgeService cinematicKnowledgeService;
    @Resource
    private VideoQualityScoreService videoQualityScoreService;

    /**
     * 批量生成关键帧（SSE 实时进度）
     * 返回 text/event-stream，客户端解析 progress 事件获取进度，done 事件获取结果
     */
    @PostMapping(value = "/generate-keyframes-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "批量生成关键帧（SSE 实时进度）")
    public SseEmitter generateKeyframesStream(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            SseEmitter err = createSseEmitter(1000L, "short-video-keyframes-auth-error");
            err.completeWithError(new RuntimeException("未登录"));
            return err;
        }
        Long shotListId = body.get("shotListId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        String characterRef = body.get("characterReferenceUrl") instanceof String s ? s : null;
        String sceneRef = body.get("sceneReferenceUrl") instanceof String s ? s : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> shotsList = (List<Map<String, Object>>) body.get("shots");
        if (shotsList == null || shotsList.isEmpty()) {
            SseEmitter err = createSseEmitter(1000L, "short-video-keyframes-validation-error");
            err.completeWithError(new RuntimeException("shots 不能为空"));
            return err;
        }
        List<ShortVideoMaterialService.KeyframeInput> inputs = shotsList.stream()
                .map(m -> new ShortVideoMaterialService.KeyframeInput(
                        m.get("shotId") instanceof Number n ? n.longValue() : null,
                        m.get("shotNumber") instanceof Number n ? n.intValue() : null,
                        m.get("sceneDescription") instanceof String s ? s : "",
                        m.get("style") instanceof String s ? s : null,
                        characterRef, sceneRef))
                .collect(Collectors.toList());

        SseEmitter emitter = createSseEmitter(300_000L, "short-video-keyframes"); // 5 分钟超时
        try {
            List<ShortVideoMaterialService.KeyframeResult> keyframes = materialService.generateKeyframesWithProgress(
                    projectId, shotListId, inputs, userId,
                    evt -> {
                        try {
                            Map<String, Object> data = Map.of(
                                    "step", evt.step(), "current", evt.current(), "total", evt.total(),
                                    "percent", evt.percent(), "message", evt.message() != null ? evt.message() : ""
                            );
                            emitter.send(SseEmitter.event().name("progress").data(data, MediaType.APPLICATION_JSON));
                        } catch (Exception e) {
                            try {
                                Map<String, Object> err = Map.of("error", e.getMessage() != null ? e.getMessage() : "进度推送失败");
                                emitter.send(SseEmitter.event().name("error").data(err, MediaType.APPLICATION_JSON));
                            } catch (Exception ignored) {
                                // SSE错误消息发送失败，客户端已断开
                            }
                            emitter.complete();
                        }
                    });
            Map<String, Object> done = Map.of("keyframes",
                    keyframes.stream().map(k -> Map.of(
                            "shotId", k.shotId() != null ? k.shotId() : 0,
                            "shotNumber", k.shotNumber() != null ? k.shotNumber() : 0,
                            "imageUrl", k.imageUrl() != null ? k.imageUrl() : "",
                            "bosKey", k.bosKey() != null ? k.bosKey() : "",
                            "prompt", k.prompt() != null ? k.prompt() : "",
                            "endFrameUrl", k.endFrameUrl() != null ? k.endFrameUrl() : "",
                            "endFrameBosKey", k.endFrameBosKey() != null ? k.endFrameBosKey() : ""
                    )).toList());
            emitter.send(SseEmitter.event().name("done").data(done, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            try {
                String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : (e.getCause() != null ? e.getCause().getMessage() : null);
                if (msg == null || msg.isBlank()) msg = "图像生成失败，请确认 Stable Diffusion 服务已启动 (http://localhost:7860) 或 ComfyUI 已配置";
                Map<String, Object> err = Map.of("error", msg);
                emitter.send(SseEmitter.event().name("error").data(err, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                log.debug("SSE error事件发送失败: {}", ex.getMessage());
            }
            emitter.complete();
        } finally {
            emitter.complete();
        }
        return emitter;
    }

    @PostMapping("/generate-keyframes")
    @Operation(summary = "批量生成关键帧")
    public RESTResult<Map<String, Object>> generateKeyframes(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long shotListId = body.get("shotListId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        String characterRef = body.get("characterReferenceUrl") instanceof String s ? s : null;
        String sceneRef = body.get("sceneReferenceUrl") instanceof String s ? s : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> shotsList = (List<Map<String, Object>>) body.get("shots");
        if (shotsList == null || shotsList.isEmpty()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "shots 不能为空");
        List<ShortVideoMaterialService.KeyframeInput> inputs = shotsList.stream()
                .map(m -> new ShortVideoMaterialService.KeyframeInput(
                        m.get("shotId") instanceof Number n ? n.longValue() : null,
                        m.get("shotNumber") instanceof Number n ? n.intValue() : null,
                        m.get("sceneDescription") instanceof String s ? s : "",
                        m.get("style") instanceof String s ? s : null,
                        characterRef, sceneRef))
                .collect(Collectors.toList());
        List<ShortVideoMaterialService.KeyframeResult> keyframes = materialService.generateKeyframes(projectId, shotListId, inputs, userId);
        return RESTResult.getSuccess(Map.of("keyframes", keyframes));
    }

    @PostMapping("/generate-voice-batch")
    @Operation(summary = "批量生成配音")
    public RESTResult<Map<String, Object>> generateVoiceBatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long shotListId = body.get("shotListId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> shotsList = (List<Map<String, Object>>) body.get("shots");
        if (shotsList == null || shotsList.isEmpty()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "shots 不能为空");
        List<ShortVideoMaterialService.VoiceInput> inputs = shotsList.stream()
                .map(m -> new ShortVideoMaterialService.VoiceInput(
                        m.get("shotId") instanceof Number n ? n.longValue() : null,
                        m.get("shotNumber") instanceof Number n ? n.intValue() : null,
                        m.get("dialogue") instanceof String s ? s : "",
                        m.get("voice") instanceof String s ? s : null,
                        m.get("speed") instanceof Number n ? n.doubleValue() : null))
                .collect(Collectors.toList());
        List<ShortVideoMaterialService.VoiceResult> voices = materialService.generateVoiceBatch(projectId, shotListId, inputs, userId);
        return RESTResult.getSuccess(Map.of("voices", voices));
    }

    /**
     * 图生视频批量（SSE 实时进度）
     * 返回 text/event-stream，客户端解析 progress 事件获取进度，done 事件获取结果
     */
    @PostMapping(value = "/img2video-batch-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "图生视频批量（SSE 实时进度）")
    public SseEmitter img2videoBatchStream(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            SseEmitter err = createSseEmitter(1000L, "short-video-img2video-auth-error");
            err.completeWithError(new RuntimeException("未登录"));
            return err;
        }
        Long shotListId = body.get("shotListId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyframesList = (List<Map<String, Object>>) body.get("keyframes");
        if (keyframesList == null || keyframesList.isEmpty()) {
            SseEmitter err = createSseEmitter(1000L, "short-video-img2video-validation-error");
            err.completeWithError(new RuntimeException("keyframes 不能为空"));
            return err;
        }
        List<ShortVideoMaterialService.Img2VideoInput> inputs = keyframesList.stream()
                .map(m -> new ShortVideoMaterialService.Img2VideoInput(
                        m.get("shotId") instanceof Number n ? n.longValue() : null,
                        m.get("shotNumber") instanceof Number n ? n.intValue() : null,
                        m.get("imageUrl") instanceof String s ? s : "",
                        m.get("endFrameUrl") instanceof String s ? s : null,
                        m.get("duration") instanceof Number n ? n.intValue() : 5,
                        m.get("motion") instanceof String s ? s : "zoom-in",
                        m.get("sceneDescription") instanceof String s ? s : null,
                        m.get("quality") instanceof String s ? s : null,
                        m.get("aspectRatio") instanceof String s ? s : null,
                        m.get("cameraType") instanceof String s ? s : null,
                        m.get("mood") instanceof String s ? s : null,
                        m.get("action") instanceof String s ? s : null))
                .collect(Collectors.toList());

        SseEmitter emitter = createSseEmitter(600_000L, "short-video-img2video"); // 10 分钟超时（视频生成较慢）
        try {
            List<ShortVideoMaterialService.VideoResult> videos = materialService.img2videoBatchWithProgress(
                    projectId, shotListId, inputs, userId,
                    evt -> {
                        try {
                            Map<String, Object> data = Map.of(
                                    "step", evt.step(), "current", evt.current(), "total", evt.total(),
                                    "percent", evt.percent(), "message", evt.message() != null ? evt.message() : ""
                            );
                            emitter.send(SseEmitter.event().name("progress").data(data, MediaType.APPLICATION_JSON));
                        } catch (Exception e) {
                            try {
                                Map<String, Object> err = Map.of("error", e.getMessage() != null ? e.getMessage() : "进度推送失败");
                                emitter.send(SseEmitter.event().name("error").data(err, MediaType.APPLICATION_JSON));
                            } catch (Exception ignored) {
                                // SSE错误消息发送失败，客户端已断开
                            }
                            emitter.complete();
                        }
                    });
            Map<String, Object> done = Map.of("videos",
                    videos.stream().map(v -> Map.of(
                            "shotId", v.shotId() != null ? v.shotId() : 0,
                            "shotNumber", v.shotNumber() != null ? v.shotNumber() : 0,
                            "videoUrl", v.videoUrl() != null ? v.videoUrl() : "",
                            "bosKey", v.bosKey() != null ? v.bosKey() : "",
                            "duration", v.duration() != null ? v.duration() : 0
                    )).toList());
            emitter.send(SseEmitter.event().name("done").data(done, MediaType.APPLICATION_JSON));
        } catch (Exception e) {
            try {
                String msg = e.getMessage() != null && !e.getMessage().isBlank() ? e.getMessage() : (e.getCause() != null ? e.getCause().getMessage() : null);
                if (msg == null || msg.isBlank()) msg = "图生视频失败";
                Map<String, Object> err = Map.of("error", msg);
                emitter.send(SseEmitter.event().name("error").data(err, MediaType.APPLICATION_JSON));
            } catch (Exception ex) {
                log.debug("SSE error事件发送失败: {}", ex.getMessage());
            }
            emitter.complete();
        } finally {
            emitter.complete();
        }
        return emitter;
    }

    private SseEmitter createSseEmitter(long timeoutMs, String streamName) {
        SseEmitter emitter = new SseEmitter(timeoutMs);
        emitter.onCompletion(() -> log.debug("SSE completed: {}", streamName));
        emitter.onTimeout(() -> log.debug("SSE timeout: {}", streamName));
        emitter.onError(error -> log.debug("SSE error: {}, {}", streamName, error.getMessage()));
        return emitter;
    }

    @PostMapping("/img2video-batch")
    @Operation(summary = "图生视频批量")
    public RESTResult<Map<String, Object>> img2videoBatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long shotListId = body.get("shotListId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> keyframesList = (List<Map<String, Object>>) body.get("keyframes");
        if (keyframesList == null || keyframesList.isEmpty()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "keyframes 不能为空");
        List<ShortVideoMaterialService.Img2VideoInput> inputs = keyframesList.stream()
                .map(m -> new ShortVideoMaterialService.Img2VideoInput(
                        m.get("shotId") instanceof Number n ? n.longValue() : null,
                        m.get("shotNumber") instanceof Number n ? n.intValue() : null,
                        m.get("imageUrl") instanceof String s ? s : "",
                        m.get("endFrameUrl") instanceof String s ? s : null,
                        m.get("duration") instanceof Number n ? n.intValue() : 5,
                        m.get("motion") instanceof String s ? s : "zoom-in",
                        m.get("sceneDescription") instanceof String s ? s : null,
                        m.get("quality") instanceof String s ? s : null,
                        m.get("aspectRatio") instanceof String s ? s : null,
                        m.get("cameraType") instanceof String s ? s : null,
                        m.get("mood") instanceof String s ? s : null,
                        m.get("action") instanceof String s ? s : null))
                .collect(Collectors.toList());
        List<ShortVideoMaterialService.VideoResult> videos = materialService.img2videoBatch(projectId, shotListId, inputs, userId);
        return RESTResult.getSuccess(Map.of("videos", videos));
    }

    @PostMapping("/recommend-camera")
    @Operation(summary = "AI 推荐运镜 (Phase 5 知识库)")
    public RESTResult<Map<String, Object>> recommendCamera(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String sceneDescription = body.get("sceneDescription") instanceof String s ? s : "";
        var list = cinematicKnowledgeService.recommendCamera(sceneDescription);
        var recommendations = list.stream()
                .map(r -> Map.<String, Object>of(
                        "cameraType", r.cameraType().getCode(),
                        "confidence", r.confidence(),
                        "reason", r.reason() != null ? r.reason() : "",
                        "bestModel", r.bestModel() != null ? r.bestModel() : ""))
                .toList();
        return RESTResult.getSuccess(Map.of(
                "recommendations", recommendations,
                "primary", list.isEmpty() ? "zoom-in" : list.get(0).cameraType().getCode()));
    }

    @PostMapping("/evaluate-video-quality")
    @Operation(summary = "视频质量评分 (Phase 6.1)")
    public RESTResult<Map<String, Object>> evaluateVideoQuality(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        if (AuthTokenFilter.getUserId(request) == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String videoUrl = body.get("videoUrl") instanceof String s ? s : null;
        if (videoUrl == null || videoUrl.isBlank()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoUrl 不能为空");
        var report = videoQualityScoreService.evaluateVideoFromUrl(videoUrl);
        return RESTResult.getSuccess(Map.of(
                "overallScore", Math.round(report.overallScore() * 10) / 10.0,
                "grade", report.grade(),
                "sharpnessScore", Math.round(report.sharpnessScore() * 10) / 10.0,
                "motionScore", Math.round(report.motionScore() * 10) / 10.0,
                "colorScore", Math.round(report.colorScore() * 10) / 10.0,
                "noiseScore", Math.round(report.noiseScore() * 10) / 10.0,
                "exposureScore", Math.round(report.exposureScore() * 10) / 10.0,
                "issues", report.issues(),
                "suggestions", report.suggestions()));
    }

    @PostMapping("/retry-keyframe")
    @Operation(summary = "重试单个关键帧生成失败")
    public RESTResult<Map<String, Object>> retryKeyframe(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long shotListId = body.get("shotListId") instanceof Number n ? n.longValue() : null;
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        String characterRef = body.get("characterReferenceUrl") instanceof String s ? s : null;
        String sceneRef = body.get("sceneReferenceUrl") instanceof String s ? s : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> shotMap = (Map<String, Object>) body.get("shot");
        if (shotMap == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "shot 不能为空");
        ShortVideoMaterialService.KeyframeInput input = new ShortVideoMaterialService.KeyframeInput(
                shotMap.get("shotId") instanceof Number n ? n.longValue() : null,
                shotMap.get("shotNumber") instanceof Number n ? n.intValue() : null,
                shotMap.get("sceneDescription") instanceof String s ? s : "",
                shotMap.get("style") instanceof String s ? s : null,
                characterRef, sceneRef);
        List<ShortVideoMaterialService.KeyframeResult> keyframes = materialService.generateKeyframes(
                projectId, shotListId, List.of(input), userId);
        return RESTResult.getSuccess(Map.of("keyframe", keyframes.isEmpty() ? null : keyframes.get(0)));
    }
}
