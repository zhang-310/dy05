package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.util.ShortVideoPathHelper;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 短视频剪辑 API
 * 路径：/api/v1/short-video/edit
 */
@RestController
@RequestMapping("/api/v1/short-video/edit")
@Tag(name = "短视频剪辑", description = "自动剪辑、字幕生成")
public class ShortVideoEditController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private VideoEditService videoEditService;
    @Resource
    private BosStorageService bosStorageService;
    @Resource
    private SvProjectService projectService;

    @PostMapping("/auto-compose")
    @Operation(summary = "自动剪辑成片")
    public RESTResult<Map<String, Object>> autoCompose(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> materials = (Map<String, Object>) body.get("materials");
        if (materials == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "materials 不能为空");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> videosList = (List<Map<String, Object>>) materials.get("videos");
        List<String> videoUrls = new ArrayList<>();
        if (videosList != null) {
            for (Map<String, Object> v : videosList) {
                Object url = v.get("videoUrl");
                if (url instanceof String s && !s.isBlank()) videoUrls.add(s);
            }
        }
        if (videoUrls.isEmpty()) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videos 不能为空");
        String bgmUrl = materials.get("bgmUrl") instanceof String s ? s : null;
        String scriptText = materials.get("scriptText") instanceof String s ? s : null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subtitlesList = (List<Map<String, Object>>) materials.get("subtitles");
        List<String> voiceClipUrls = new ArrayList<>();
        if (materials.get("voiceClipUrls") instanceof List<?> vcl) {
            for (Object o : vcl) {
                if (o instanceof String s && !s.isBlank()) voiceClipUrls.add(s);
            }
        }
        List<VideoEditService.SubtitleItem> subtitleItems = new ArrayList<>();
        if (subtitlesList != null) {
            for (Map<String, Object> st : subtitlesList) {
                Double start = st.get("startTime") instanceof Number n ? n.doubleValue() : null;
                Double end = st.get("endTime") instanceof Number n ? n.doubleValue() : null;
                String text = st.get("text") instanceof String s ? s : null;
                if (start != null && end != null && text != null) {
                    subtitleItems.add(new VideoEditService.SubtitleItem(start, end, text));
                }
            }
        }
        List<String> imageUrls = materials.get("imageUrls") instanceof List<?> i ? (List<String>) i : null;
        VideoEditService.AutoComposeRequest req = new VideoEditService.AutoComposeRequest(
                videoUrls, imageUrls, scriptText, bgmUrl, "default", voiceClipUrls, subtitleItems);
        VideoEditService.VideoResult result = videoEditService.autoCompose(req, userId);
        String finalVideoUrl = result != null ? result.videoUrl() : null;
        String bosKey = null;
        if (StringUtils.hasText(finalVideoUrl) && bosStorageService.isConfigured() && projectId != null) {
            try {
                Path p = Path.of(finalVideoUrl);
                if (Files.exists(p)) {
                    byte[] bytes = Files.readAllBytes(p);
                    String date = LocalDate.now().format(DATE_FMT);
                    String key = ShortVideoPathHelper.finalVideoKey(userId, date, projectId);
                    var mf = new cn.gaifan.douyinOperations.common.util.ByteArrayMultipartFile("file", "final.mp4", "video/mp4", bytes);
                    finalVideoUrl = bosStorageService.upload(key, mf);
                    bosKey = key;
                }
            } catch (Exception ignored) {}
        }
        int durationSec = result != null && result.duration() != null ? (int) (result.duration() / 1000) : 0;
        if (projectId != null && StringUtils.hasText(finalVideoUrl)) {
            try {
                var vo = projectService.get(projectId, userId, java.util.List.of(userId));
                if (vo != null) {
                    SvProjectSaveVO save = new SvProjectSaveVO();
                    save.setId(projectId);
                    save.setTitle(vo.getTitle());
                    save.setProjectType(vo.getProjectType());
                    save.setFinalVideoUrl(finalVideoUrl);
                    save.setDuration(durationSec > 0 ? durationSec : null);
                    projectService.save(save, userId);
                }
            } catch (Exception ignored) {}
        }
        Map<String, Object> data = Map.of(
                "finalVideoUrl", finalVideoUrl != null ? finalVideoUrl : "",
                "bosKey", bosKey != null ? bosKey : "",
                "duration", durationSec,
                "thumbnail", "");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/generate-subtitles")
    @Operation(summary = "生成字幕")
    public RESTResult<Map<String, Object>> generateSubtitles(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        String videoUrl = body.get("videoUrl") instanceof String s ? s : null;
        String script = body.get("script") instanceof String s ? s : null;
        String language = body.get("language") instanceof String s ? s : "zh";
        if (!StringUtils.hasText(videoUrl) && !StringUtils.hasText(script)) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoUrl 或 script 至少填一个");
        }
        List<Map<String, Object>> subtitles = new ArrayList<>();
        if (StringUtils.hasText(script)) {
            String[] parts = script.split("[，。！？、；]");
            double start = 0;
            for (int i = 0; i < parts.length; i++) {
                String p = parts[i].trim();
                if (p.isEmpty()) continue;
                double duration = 1.5 + p.length() * 0.1;
                subtitles.add(Map.of(
                        "text", p,
                        "startTime", start,
                        "duration", duration));
                start += duration;
            }
        }
        Map<String, Object> data = Map.of(
                "subtitles", subtitles,
                "srtUrl", "");
        RESTResult<Map<String, Object>> r = RESTResult.getSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
