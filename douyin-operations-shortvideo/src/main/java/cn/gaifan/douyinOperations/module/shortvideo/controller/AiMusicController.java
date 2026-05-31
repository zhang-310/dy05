package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicProvider;
import cn.gaifan.douyinOperations.module.ai.service.AiMusicService;
import cn.gaifan.douyinOperations.module.ai.service.SfxGenerationService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvGenerationLog;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvGenerationLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.PageRequest;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AI 音乐与音效 API (Phase 7)
 * 路径：/api/v1/short-video/music
 */
@RestController
@RequestMapping("/api/v1/short-video/music")
@Tag(name = "AI 音乐与音效", description = "BGM 生成 (Suno/Udio) + 音效生成 (ElevenLabs)")
public class AiMusicController {

    @Resource
    private AiMusicService aiMusicService;
    @Resource
    private SfxGenerationService sfxGenerationService;
    @Resource
    private SvGenerationLogRepository generationLogRepository;

    @PostMapping("/generate-bgm")
    @Operation(summary = "AI 生成 BGM")
    public RESTResult<Map<String, Object>> generateBgm(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String styleDescription = body.get("styleDescription") instanceof String s ? s : "cinematic background music";
        int durationSec = body.get("durationSec") instanceof Number n ? n.intValue() : 30;
        boolean instrumental = body.get("instrumental") != Boolean.FALSE;

        AiMusicProvider.MusicGenerationResult result = aiMusicService.generateBgm(styleDescription, durationSec, instrumental);
        saveMusicGenerationLog(userId, "bgm", styleDescription, result.musicUrl(),
                result.provider(), result.durationMs(), "instrumental=" + instrumental);
        return RESTResult.success("生成成功", Map.of(
                "musicUrl", result.musicUrl(),
                "provider", result.provider(),
                "durationMs", result.durationMs(),
                "bpm", result.bpm()));
    }

    @PostMapping("/providers")
    @Operation(summary = "获取可用的 BGM 生成 Provider")
    public RESTResult<List<String>> providers(HttpServletRequest request) {
        AuthTokenFilter.getUserId(request);
        List<String> list = aiMusicService.getAvailableProviders();
        return RESTResult.success("查询成功", list);
    }

    @PostMapping("/generate-sfx")
    @Operation(summary = "AI 生成音效 (从场景描述)")
    public RESTResult<List<Map<String, Object>>> generateSfx(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String sceneDescription = body.get("sceneDescription") instanceof String s ? s : "";
        double durationSec = body.get("durationSec") instanceof Number n ? n.doubleValue() : 5.0;

        List<SfxGenerationService.SfxResult> results = sfxGenerationService.generateSfxFromScene(sceneDescription, durationSec, userId);
        results.forEach(r -> saveMusicGenerationLog(userId, "sfx", r.description(), r.audioUrl(),
                "elevenlabs", Math.round(r.durationSec() * 1000), "scene=" + sceneDescription));
        List<Map<String, Object>> list = results.stream()
                .map(r -> Map.<String, Object>of(
                        "description", r.description(),
                        "audioUrl", r.audioUrl(),
                        "durationSec", r.durationSec()))
                .collect(Collectors.toList());
        return RESTResult.success("生成成功", list);
    }

    @PostMapping("/history")
    @Operation(summary = "AI 配乐/音效生成历史")
    public RESTResult<List<Map<String, Object>>> history(@RequestBody(required = false) Map<String, Object> body,
                                                         HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        int rows = body != null && body.get("rows") instanceof Number n ? n.intValue() : 20;
        List<SvGenerationLog> logs = generationLogRepository.findByOwnerIdAndContentTypeInOrderByCreateTimeDesc(
                userId, List.of("bgm", "sfx"), PageRequest.of(0, Math.max(1, Math.min(rows, 100))));
        List<Map<String, Object>> data = new ArrayList<>();
        for (SvGenerationLog log : logs) {
            String type = log.getContentType() != null ? log.getContentType() : "bgm";
            data.add(Map.of(
                    "id", log.getId(),
                    "name", log.getPrompt() != null ? log.getPrompt() : type,
                    "duration", formatDuration(log.getGenerationTimeMs()),
                    "url", log.getVideoUrl() != null ? log.getVideoUrl() : "",
                    "type", type,
                    "provider", log.getAiProvider() != null ? log.getAiProvider() : "",
                    "source", "sv_generation_log",
                    "degraded", false,
                    "createTime", log.getCreateTime() != null ? log.getCreateTime().toString() : ""));
        }
        return RESTResult.success("查询成功", data);
    }

    private void saveMusicGenerationLog(Long ownerId, String contentType, String prompt, String mediaUrl,
                                        String provider, long durationMs, String routeReason) {
        if (ownerId == null) {
            return;
        }
        SvGenerationLog log = new SvGenerationLog();
        log.setOwnerId(ownerId);
        log.setContentType(contentType);
        log.setPrompt(prompt);
        log.setVideoUrl(mediaUrl);
        log.setAiProvider(provider);
        log.setGenerationTimeMs(durationMs);
        log.setHasAudio(true);
        log.setSuccess(true);
        log.setRouteReason(routeReason);
        log.setCreateTime(new Timestamp(System.currentTimeMillis()));
        generationLogRepository.save(log);
    }

    private String formatDuration(Long durationMs) {
        if (durationMs == null || durationMs <= 0) {
            return "";
        }
        long seconds = Math.max(1, Math.round(durationMs / 1000.0));
        return seconds + "秒";
    }
}
