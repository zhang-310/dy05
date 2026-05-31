package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvSubtitleSegment;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvSubtitleSegmentRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvProjectService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvScriptService;
import cn.gaifan.douyinOperations.module.shortvideo.service.SvShotListService;
import cn.gaifan.douyinOperations.module.shortvideo.util.ShortVideoPathHelper;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectSaveVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvProjectVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvScriptVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotListVO;
import cn.gaifan.douyinOperations.module.shortvideo.vo.SvShotVO;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

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
    @Resource
    private SvShotListService shotListService;
    @Resource
    private SvScriptService scriptService;
    @Resource
    private SvSubtitleSegmentRepository subtitleSegmentRepository;

    @PostMapping("/auto-compose")
    @Operation(summary = "自动剪辑成片")
    public RESTResult<Map<String, Object>> autoCompose(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long projectId = body.get("projectId") instanceof Number n ? n.longValue() : null;
        @SuppressWarnings("unchecked")
        Map<String, Object> materials = (Map<String, Object>) body.get("materials");
        SvProjectVO project = null;
        if (materials == null && projectId != null) {
            project = projectService.get(projectId, userId, java.util.List.of(userId));
            materials = buildMaterialsFromProject(project, userId);
        }
        if (materials == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "materials 不能为空，或项目未关联可合成素材");
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
            } catch (Exception ignored) {
                // 视频上传失败，不影响返回结果
            }
        }
        int durationSec = result != null && result.duration() != null ? (int) (result.duration() / 1000) : 0;
        if (projectId != null && StringUtils.hasText(finalVideoUrl)) {
            try {
                var vo = project != null ? project : projectService.get(projectId, userId, java.util.List.of(userId));
                if (vo != null) {
                    SvProjectSaveVO save = new SvProjectSaveVO();
                    save.setId(projectId);
                    save.setTitle(vo.getTitle());
                    save.setProjectType(vo.getProjectType());
                    save.setFinalVideoUrl(finalVideoUrl);
                    save.setDuration(durationSec > 0 ? durationSec : null);
                    projectService.save(save, userId);
                }
            } catch (Exception ignored) {
                // 项目更新失败，不影响返回结果
            }
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

    private Map<String, Object> buildMaterialsFromProject(SvProjectVO project, Long userId) {
        if (project == null) {
            return null;
        }
        Long shotListId = project.getShotListId();
        SvShotListVO shotList = null;
        if (shotListId != null && shotListId > 0) {
            shotList = shotListService.get(shotListId, userId);
        } else if (project.getScriptId() != null && project.getScriptId() > 0) {
            shotList = shotListService.getLatestByScriptId(project.getScriptId(), userId);
        }
        if (shotList == null || shotList.getShots() == null || shotList.getShots().isEmpty()) {
            return null;
        }
        List<Map<String, Object>> videos = new ArrayList<>();
        List<String> voiceClipUrls = new ArrayList<>();
        for (SvShotVO shot : shotList.getShots()) {
            if (!StringUtils.hasText(shot.getVideoUrl())) {
                continue;
            }
            videos.add(Map.of(
                    "shotId", shot.getId() != null ? shot.getId() : 0,
                    "shotNumber", shot.getShotNumber() != null ? shot.getShotNumber() : 0,
                    "videoUrl", shot.getVideoUrl()
            ));
            if (StringUtils.hasText(shot.getAudioUrl())) {
                voiceClipUrls.add(shot.getAudioUrl());
            }
        }
        if (videos.isEmpty()) {
            return null;
        }
        Map<String, Object> materials = new java.util.LinkedHashMap<>();
        materials.put("videos", videos);
        if (!voiceClipUrls.isEmpty() && voiceClipUrls.size() == videos.size()) {
            materials.put("voiceClipUrls", voiceClipUrls);
        }
        if (project.getScriptId() != null && project.getScriptId() > 0) {
            try {
                SvScriptVO script = scriptService.get(project.getScriptId(), userId);
                if (script != null && StringUtils.hasText(script.getContent())) {
                    materials.put("scriptText", script.getContent());
                }
            } catch (Exception ignored) {
                // 缺失脚本时仍可仅按视频片段合成。
            }
        }
        return materials;
    }

    @PostMapping("/subtitles/get")
    @Operation(summary = "查询字幕段")
    public RESTResult<List<Map<String, Object>>> getSubtitles(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long videoId = body.get("videoId") instanceof Number n ? n.longValue() : null;
        if (videoId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoId 必填");
        List<Map<String, Object>> data = subtitleSegmentRepository
                .findByOwnerIdAndVideoIdAndDeletedOrderByStartTimeAsc(userId, videoId, 0)
                .stream()
                .map(this::toSubtitleMap)
                .toList();
        return RESTResult.success("查询成功", data);
    }

    @PostMapping("/subtitles/save")
    @Operation(summary = "保存字幕段")
    @Transactional(rollbackFor = Exception.class)
    public RESTResult<Void> saveSubtitles(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) return RESTResult.error(ErrorCode.UNAUTHORIZED, "未登录");
        Long videoId = body.get("videoId") instanceof Number n ? n.longValue() : null;
        if (videoId == null) return RESTResult.error(ErrorCode.VALIDATION_FAIL, "videoId 必填");

        for (SvSubtitleSegment existing : subtitleSegmentRepository
                .findByOwnerIdAndVideoIdAndDeletedOrderByStartTimeAsc(userId, videoId, 0)) {
            existing.setDeleted(1);
            subtitleSegmentRepository.save(existing);
        }

        if (body.get("segments") instanceof List<?> segments) {
            for (Object raw : segments) {
                if (!(raw instanceof Map<?, ?> segment)) {
                    continue;
                }
                String text = segment.get("text") instanceof String s ? s : null;
                Double start = readDouble(segment.get("startTime"));
                Double end = readDouble(segment.get("endTime"));
                if (!StringUtils.hasText(text) || start == null || end == null || end <= start) {
                    continue;
                }
                SvSubtitleSegment entity = new SvSubtitleSegment();
                entity.setOwnerId(userId);
                entity.setVideoId(videoId);
                entity.setSegmentKey(segment.get("id") instanceof String s ? s : java.util.UUID.randomUUID().toString());
                entity.setStartTime(start);
                entity.setEndTime(end);
                entity.setText(text.trim());
                entity.setFontSize(segment.get("fontSize") instanceof Number n ? n.intValue() : null);
                entity.setColor(segment.get("color") instanceof String s ? s : null);
                entity.setFontFamily(segment.get("fontFamily") instanceof String s ? s : null);
                entity.setPosition(segment.get("position") instanceof String s ? s : null);
                subtitleSegmentRepository.save(entity);
            }
        }
        return RESTResult.success();
    }

    @PostMapping("/subtitles/export-srt")
    @Operation(summary = "导出字幕 SRT")
    public RESTResult<String> exportSubtitlesSrt(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        RESTResult<List<Map<String, Object>>> subtitles = getSubtitles(body, request);
        if (subtitles.getStatus() != 200) {
            return RESTResult.error(subtitles.getStatus(), subtitles.getMessage());
        }
        return RESTResult.success("导出成功", toSrt(subtitles.getData()));
    }

    private Map<String, Object> toSubtitleMap(SvSubtitleSegment segment) {
        Map<String, Object> out = new java.util.LinkedHashMap<>();
        out.put("id", segment.getSegmentKey());
        out.put("startTime", segment.getStartTime());
        out.put("endTime", segment.getEndTime());
        out.put("text", segment.getText());
        if (segment.getFontSize() != null) out.put("fontSize", segment.getFontSize());
        if (StringUtils.hasText(segment.getColor())) out.put("color", segment.getColor());
        if (StringUtils.hasText(segment.getFontFamily())) out.put("fontFamily", segment.getFontFamily());
        if (StringUtils.hasText(segment.getPosition())) out.put("position", segment.getPosition());
        out.put("source", "sv_subtitle_segment");
        out.put("degraded", false);
        return out;
    }

    private Double readDouble(Object value) {
        return value instanceof Number n ? n.doubleValue() : null;
    }

    private String toSrt(List<Map<String, Object>> segments) {
        StringBuilder sb = new StringBuilder();
        int index = 1;
        for (Map<String, Object> segment : segments) {
            Double start = readDouble(segment.get("startTime"));
            Double end = readDouble(segment.get("endTime"));
            String text = segment.get("text") instanceof String s ? s : "";
            if (start == null || end == null || !StringUtils.hasText(text)) {
                continue;
            }
            sb.append(index++).append('\n')
                    .append(formatSrtTime(start)).append(" --> ").append(formatSrtTime(end)).append('\n')
                    .append(text).append("\n\n");
        }
        return sb.toString();
    }

    private String formatSrtTime(double seconds) {
        int totalMillis = (int) Math.round(seconds * 1000);
        int hours = totalMillis / 3_600_000;
        int minutes = (totalMillis % 3_600_000) / 60_000;
        int secs = (totalMillis % 60_000) / 1000;
        int millis = totalMillis % 1000;
        return String.format("%02d:%02d:%02d,%03d", hours, minutes, secs, millis);
    }
}
