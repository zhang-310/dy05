package cn.gaifan.douyinOperations.module.shortvideo.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvProject;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvProjectRepository;
import cn.gaifan.douyinOperations.module.shortvideo.util.ShortVideoPathHelper;
import cn.gaifan.douyinOperations.module.storage.service.BosFileMetadataService;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import cn.gaifan.douyinOperations.module.storage.vo.StorageFileVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.MDC;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 短视频素材上传 API（BOS 存储）
 * 路径规范：{userId}/{date}/{projectId}/{keyframes|videos|audios|thumbnails}/
 * 参考：docs/design/BAIDU-BOS-STORAGE-INTEGRATION.md
 */
@RestController
@RequestMapping("/api/v1/short-video/upload")
@Tag(name = "短视频上传 / Short Video Upload", description = "关键帧、视频、配音、封面等素材上传到 BOS")
public class ShortVideoUploadController {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private static final long MAX_IMAGE = 10 * 1024 * 1024;      // 10MB
    private static final long MAX_VIDEO = 500 * 1024 * 1024;    // 500MB
    private static final long MAX_AUDIO = 50 * 1024 * 1024;      // 50MB

    private static final Set<String> IMAGE_EXTS = Set.of(".jpg", ".jpeg", ".png", ".gif", ".webp");
    private static final Set<String> VIDEO_EXTS = Set.of(".mp4", ".mov", ".avi", ".webm");
    private static final Set<String> AUDIO_EXTS = Set.of(".mp3", ".wav", ".m4a");

    @Resource
    private BosStorageService bosStorageService;
    @Resource
    private BosFileMetadataService bosFileMetadataService;
    @Resource
    private SvProjectRepository projectRepository;

    private Long requireUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }

    /** 校验项目归属，非 owner 无权操作 */
    private void requireProjectOwner(Long projectId, Long userId) {
        if (projectId == null || userId == null) return;
        SvProject p = projectRepository.findById(projectId).orElse(null);
        if (p == null) throw new BusinessException(ErrorCode.NOT_FOUND, "项目不存在");
        if (p.getOwnerId() == null || !p.getOwnerId().equals(userId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "无权操作该项目");
        }
    }

    private String parseDate(Long projectId, String dateParam, Long userId) {
        if (StringUtils.hasText(dateParam)) return dateParam.trim();
        if (projectId != null && userId != null) {
            return projectRepository.findById(projectId)
                    .filter(p -> p.getOwnerId() != null && p.getOwnerId().equals(userId))
                    .map(SvProject::getCreateTime)
                    .filter(t -> t != null)
                    .map(t -> t.toLocalDateTime().toLocalDate().format(DATE_FMT))
                    .orElse(LocalDate.now().format(DATE_FMT));
        }
        return LocalDate.now().format(DATE_FMT);
    }

    private String getExt(String filename) {
        if (filename == null) return "";
        int i = filename.lastIndexOf('.');
        return i >= 0 ? filename.substring(i).toLowerCase() : "";
    }

    private void validateImage(MultipartFile file) {
        String ext = getExt(file.getOriginalFilename());
        if (!IMAGE_EXTS.contains(ext)) {
            throw new BusinessException(
                    ErrorCode.STORAGE_FILE_TYPE_NOT_ALLOWED, "仅支持图片：jpg/png/gif/webp");
        }
        if (file.getSize() > MAX_IMAGE) {
            throw new BusinessException(
                    ErrorCode.STORAGE_FILE_TOO_LARGE, "图片大小不能超过 10MB");
        }
    }

    private void validateVideo(MultipartFile file) {
        String ext = getExt(file.getOriginalFilename());
        if (!VIDEO_EXTS.contains(ext)) {
            throw new BusinessException(
                    ErrorCode.STORAGE_FILE_TYPE_NOT_ALLOWED, "仅支持视频：mp4/mov/avi/webm");
        }
        if (file.getSize() > MAX_VIDEO) {
            throw new BusinessException(
                    ErrorCode.STORAGE_FILE_TOO_LARGE, "视频大小不能超过 500MB");
        }
    }

    private void validateAudio(MultipartFile file) {
        String ext = getExt(file.getOriginalFilename());
        if (!AUDIO_EXTS.contains(ext)) {
            throw new BusinessException(
                    ErrorCode.STORAGE_FILE_TYPE_NOT_ALLOWED, "仅支持音频：mp3/wav/m4a");
        }
        if (file.getSize() > MAX_AUDIO) {
            throw new BusinessException(
                    ErrorCode.STORAGE_FILE_TOO_LARGE, "音频大小不能超过 50MB");
        }
    }

    @PostMapping("/keyframe")
    @Operation(summary = "上传关键帧图片", description = "上传到 {userId}/{date}/{projectId}/keyframes/shot_XXX.jpg，日期自动从项目创建时间获取")
    public RESTResult<Map<String, String>> uploadKeyframe(
            HttpServletRequest request,
            @Parameter(description = "项目 ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "分镜序号", required = true) @RequestParam Integer shotNumber,
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        Long userId = requireUserId(request);
        requireProjectOwner(projectId, userId);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        validateImage(file);
        String d = parseDate(projectId, null, userId);
        String key = ShortVideoPathHelper.keyframeKey(userId, d, projectId, shotNumber);
        String url = bosStorageService.upload(key, file);
        bosFileMetadataService.recordUpload(key, userId, projectId, "keyframe", file.getSize(), null, null);
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/keyframes/batch")
    @Operation(summary = "批量上传关键帧", description = "并发上传多张关键帧，日期自动从项目创建时间获取")
    public RESTResult<Map<String, Object>> batchUploadKeyframes(
            HttpServletRequest request,
            @Parameter(description = "项目 ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "图片文件列表", required = true) @RequestParam("files") MultipartFile[] files,
            @Parameter(description = "分镜序号列表，与 files 一一对应", required = true) @RequestParam("shotNumbers") Integer[] shotNumbers) {
        Long userId = requireUserId(request);
        requireProjectOwner(projectId, userId);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        if (files == null || files.length == 0 || shotNumbers == null || shotNumbers.length != files.length) {
            return RESTResult.error(ErrorCode.VALIDATION_FAIL, "files 与 shotNumbers 数量必须一致且非空");
        }
        String d = parseDate(projectId, null, userId);
        List<Map<String, String>> success = new ArrayList<>();
        List<Map<String, Object>> failed = new ArrayList<>();
        @SuppressWarnings("unchecked")
        CompletableFuture<Map<String, Object>>[] futures = new CompletableFuture[files.length];
        for (int i = 0; i < files.length; i++) {
            final int idx = i;
            final MultipartFile f = files[i];
            final int sn = i < shotNumbers.length ? shotNumbers[i] : (i + 1);
            futures[i] = CompletableFuture.supplyAsync(() -> {
                try {
                    validateImage(f);
                    String key = ShortVideoPathHelper.keyframeKey(userId, d, projectId, sn);
                    String url = bosStorageService.upload(key, f);
                    bosFileMetadataService.recordUpload(key, userId, projectId, "keyframe", f.getSize(), null, null);
                    Map<String, Object> r = new HashMap<>();
                    r.put("shotNumber", sn);
                    r.put("key", key);
                    r.put("url", url);
                    r.put("success", true);
                    return r;
                } catch (Exception e) {
                    Map<String, Object> r = new HashMap<>();
                    r.put("shotNumber", sn);
                    r.put("success", false);
                    r.put("error", e.getMessage() != null ? e.getMessage() : "上传失败");
                    return r;
                }
            });
        }
        CompletableFuture.allOf(futures).join();
        for (CompletableFuture<Map<String, Object>> cf : futures) {
            Map<String, Object> r = cf.getNow(new HashMap<>());
            if (Boolean.TRUE.equals(r.get("success"))) {
                Map<String, String> s = new HashMap<>();
                s.put("shotNumber", String.valueOf(r.get("shotNumber")));
                s.put("key", (String) r.get("key"));
                s.put("url", (String) r.get("url"));
                success.add(s);
            } else {
                failed.add(r);
            }
        }
        Map<String, Object> data = new HashMap<>();
        data.put("success", success);
        data.put("failed", failed);
        data.put("total", files.length);
        data.put("successCount", success.size());
        RESTResult<Map<String, Object>> result = RESTResult.addSuccess(data);
        result.setTraceId(MDC.get("traceId"));
        return result;
    }

    @PostMapping("/video")
    @Operation(summary = "上传视频片段", description = "上传到 {userId}/{date}/{projectId}/videos/shot_XXX.mp4，日期自动从项目创建时间获取")
    public RESTResult<Map<String, String>> uploadVideo(
            HttpServletRequest request,
            @Parameter(description = "项目 ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "分镜序号", required = true) @RequestParam Integer shotNumber,
            @Parameter(description = "视频文件", required = true) @RequestParam("file") MultipartFile file) {
        Long userId = requireUserId(request);
        requireProjectOwner(projectId, userId);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        validateVideo(file);
        String d = parseDate(projectId, null, userId);
        String key = ShortVideoPathHelper.videoClipKey(userId, d, projectId, shotNumber);
        String url = bosStorageService.upload(key, file);
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/audio")
    @Operation(summary = "上传配音文件", description = "上传到 {userId}/{date}/{projectId}/audios/voice_XXX.mp3，日期自动从项目创建时间获取")
    public RESTResult<Map<String, String>> uploadAudio(
            HttpServletRequest request,
            @Parameter(description = "项目 ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "分镜序号", required = true) @RequestParam Integer shotNumber,
            @Parameter(description = "音频文件", required = true) @RequestParam("file") MultipartFile file) {
        Long userId = requireUserId(request);
        requireProjectOwner(projectId, userId);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        validateAudio(file);
        String d = parseDate(projectId, null, userId);
        String key = ShortVideoPathHelper.audioKey(userId, d, projectId, shotNumber);
        String url = bosStorageService.upload(key, file);
        bosFileMetadataService.recordUpload(key, userId, projectId, "video", file.getSize(), null, null);
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/thumbnail")
    @Operation(summary = "上传封面图片", description = "上传到 {userId}/{date}/{projectId}/thumbnails/cover_N.jpg，日期自动从项目创建时间获取")
    public RESTResult<Map<String, String>> uploadThumbnail(
            HttpServletRequest request,
            @Parameter(description = "项目 ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "封面序号", required = true) @RequestParam Integer index,
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file) {
        Long userId = requireUserId(request);
        requireProjectOwner(projectId, userId);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        validateImage(file);
        String d = parseDate(projectId, null, userId);
        String key = ShortVideoPathHelper.thumbnailKey(userId, d, projectId, index);
        String url = bosStorageService.upload(key, file);
        bosFileMetadataService.recordUpload(key, userId, projectId, "thumbnail", file.getSize(), null, null);
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/final-video")
    @Operation(summary = "上传成片视频", description = "上传到 {userId}/{date}/{projectId}/videos/final.mp4，日期自动从项目创建时间获取")
    public RESTResult<Map<String, String>> uploadFinalVideo(
            HttpServletRequest request,
            @Parameter(description = "项目 ID", required = true) @RequestParam Long projectId,
            @Parameter(description = "视频文件", required = true) @RequestParam("file") MultipartFile file) {
        Long userId = requireUserId(request);
        requireProjectOwner(projectId, userId);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        validateVideo(file);
        String d = parseDate(projectId, null, userId);
        String key = ShortVideoPathHelper.finalVideoKey(userId, d, projectId);
        String url = bosStorageService.upload(key, file);
        bosFileMetadataService.recordUpload(key, userId, projectId, "video", file.getSize(), null, null);
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/reference/character")
    @Operation(summary = "上传人物参考图", description = "上传到 {userId}/references/characters/{characterId}/，可选 projectId 时保存到项目")
    public RESTResult<Map<String, String>> uploadCharacterReference(
            HttpServletRequest request,
            @Parameter(description = "人物 ID", required = true) @RequestParam String characterId,
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "项目 ID（可选，传入时保存到项目）") @RequestParam(required = false) Long projectId) {
        Long userId = requireUserId(request);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        validateImage(file);
        String ext = getExt(file.getOriginalFilename());
        if (ext.isEmpty()) ext = ".jpg";
        String key = ShortVideoPathHelper.characterReferenceKey(userId, characterId, ext);
        String url = bosStorageService.upload(key, file);
        bosFileMetadataService.recordUpload(key, userId, projectId, "reference", file.getSize(), null, null);
        if (projectId != null) {
            requireProjectOwner(projectId, userId);
            SvProject p = projectRepository.findById(projectId).orElse(null);
            if (p != null) {
                p.setCharacterReferenceUrl(url);
                projectRepository.save(p);
            }
        }
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/reference/scene")
    @Operation(summary = "上传场景参考图", description = "上传到 {userId}/references/scenes/{sceneId}/，可选 projectId 时保存到项目")
    public RESTResult<Map<String, String>> uploadSceneReference(
            HttpServletRequest request,
            @Parameter(description = "场景 ID", required = true) @RequestParam String sceneId,
            @Parameter(description = "图片文件", required = true) @RequestParam("file") MultipartFile file,
            @Parameter(description = "项目 ID（可选，传入时保存到项目）") @RequestParam(required = false) Long projectId) {
        Long userId = requireUserId(request);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "请先在「系统配置」中配置 BOS");
        }
        validateImage(file);
        String ext = getExt(file.getOriginalFilename());
        if (ext.isEmpty()) ext = ".jpg";
        String key = ShortVideoPathHelper.sceneReferenceKey(userId, sceneId, ext);
        String url = bosStorageService.upload(key, file);
        bosFileMetadataService.recordUpload(key, userId, projectId, "reference", file.getSize(), null, null);
        if (projectId != null) {
            requireProjectOwner(projectId, userId);
            SvProject p = projectRepository.findById(projectId).orElse(null);
            if (p != null) {
                p.setSceneReferenceUrl(url);
                projectRepository.save(p);
            }
        }
        Map<String, String> data = new HashMap<>(2);
        data.put("key", key);
        data.put("url", url);
        RESTResult<Map<String, String>> r = RESTResult.addSuccess(data);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }

    @PostMapping("/reference/list")
    @Operation(summary = "列出已有人物/场景参考图", description = "列出当前用户已上传的参考图，支持选择复用，避免重复上传产生垃圾素材")
    public RESTResult<List<Map<String, Object>>> listReferenceImages(HttpServletRequest request) {
        Long userId = requireUserId(request);
        if (!bosStorageService.isConfigured()) {
            return RESTResult.error(ErrorCode.STORAGE_NOT_CONFIGURED, "BOS 未配置");
        }
        List<Map<String, Object>> result = new ArrayList<>();
        String userPrefix = userId + "/";
        for (String type : List.of("references/characters", "references/scenes")) {
            String prefix = userPrefix + type + "/";
            List<String> keys = bosStorageService.listAllObjectKeys(prefix);
            for (String key : keys) {
                if (key == null || key.isEmpty() || key.equals(prefix)) continue;
                String lower = key.toLowerCase();
                if (!lower.endsWith(".jpg") && !lower.endsWith(".jpeg") && !lower.endsWith(".png") && !lower.endsWith(".gif") && !lower.endsWith(".webp")) {
                    continue;
                }
                String url = bosStorageService.getPublicUrl(key);
                result.add(Map.<String, Object>of(
                        "key", key,
                        "url", url,
                        "type", type.contains("characters") ? "character" : "scene"
                ));
            }
        }
        RESTResult<List<Map<String, Object>>> r = RESTResult.getSuccess(result);
        r.setTraceId(MDC.get("traceId"));
        return r;
    }
}
