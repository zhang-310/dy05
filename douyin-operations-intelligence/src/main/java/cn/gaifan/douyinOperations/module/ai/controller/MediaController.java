package cn.gaifan.douyinOperations.module.ai.controller;

import cn.gaifan.douyinOperations.common.config.AuthTokenFilter;
import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.common.vo.RESTResult;
import cn.gaifan.douyinOperations.module.ai.service.AiCallLogService;
import cn.gaifan.douyinOperations.module.ai.service.AiQuotaService;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService;
import cn.gaifan.douyinOperations.module.ai.service.TtsService;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.ai.service.VideoGenerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Tag(name = "AI 多媒体")
@RestController
@RequestMapping("/api/v1/ai/media")
public class MediaController {

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private TtsService ttsService;

    @Resource
    private VideoEditService videoEditService;

    @Resource
    private VideoGenerationService videoGenerationService;

    @Resource
    private AiQuotaService aiQuotaService;

    @Resource
    private AiCallLogService aiCallLogService;

    // ========== 图像生成 ==========

    @Operation(summary = "文生图")
    @PostMapping("/image/text2img")
    public RESTResult<ImageGenerationService.ImageResult> textToImage(
            @RequestBody ImageGenerationService.TextToImageRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        ImageGenerationService.ImageResult result = withAiCall(userId, "text2img", "stable-diffusion",
                () -> imageGenerationService.textToImage(request, userId),
                truncate(request.prompt(), 256));
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "图生图")
    @PostMapping("/image/img2img")
    public RESTResult<ImageGenerationService.ImageResult> imageToImage(
            @RequestBody ImageGenerationService.ImageToImageRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        ImageGenerationService.ImageResult result = withAiCall(userId, "img2img", "stable-diffusion",
                () -> imageGenerationService.imageToImage(request, userId), null);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "图像编辑")
    @PostMapping("/image/edit")
    public RESTResult<ImageGenerationService.ImageResult> editImage(
            @RequestBody ImageGenerationService.ImageEditRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        ImageGenerationService.ImageResult result = withAiCall(userId, "edit_image", "stable-diffusion",
                () -> imageGenerationService.editImage(request, userId),
                truncate(request.prompt(), 256));
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "获取图像生成历史")
    @PostMapping("/image/history")
    public RESTResult<List<ImageGenerationService.ImageGenerationHistory>> getImageHistory(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        int page = body != null && body.get("page") instanceof Number n ? n.intValue() : 0;
        int size = body != null && body.get("size") instanceof Number n ? Math.min(Math.max(n.intValue(), 1), 100) : 20;
        List<ImageGenerationService.ImageGenerationHistory> history = imageGenerationService.getHistory(userId, page, size);
        return RESTResult.getSuccess(history);
    }

    // ========== 语音合成 ==========

    @Operation(summary = "文本转语音")
    @PostMapping("/tts/generate")
    public RESTResult<TtsService.AudioResult> textToSpeech(
            @RequestBody TtsService.TtsRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        TtsService.AudioResult result = withAiCall(userId, "tts", "tts",
                () -> ttsService.textToSpeech(request, userId),
                truncate(request.text(), 256));
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "获取可用音色")
    @PostMapping("/tts/voices")
    public RESTResult<List<TtsService.VoiceInfo>> getVoices(@RequestBody(required = false) Map<String, Object> body) {
        List<TtsService.VoiceInfo> voices = ttsService.getAvailableVoices();
        return RESTResult.getSuccess(voices);
    }

    @Operation(summary = "获取语音合成历史")
    @PostMapping("/tts/history")
    public RESTResult<List<TtsService.TtsHistory>> getTtsHistory(
            @RequestBody(required = false) Map<String, Object> body,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        int page = body != null && body.get("page") instanceof Number n ? n.intValue() : 0;
        int size = body != null && body.get("size") instanceof Number n ? Math.min(Math.max(n.intValue(), 1), 100) : 20;
        List<TtsService.TtsHistory> history = ttsService.getHistory(userId, page, size);
        return RESTResult.getSuccess(history);
    }

    // ========== 视频编辑 ==========

    @Operation(summary = "视频剪辑")
    @PostMapping("/video/trim")
    public RESTResult<VideoEditService.VideoResult> trimVideo(
            @RequestBody VideoEditService.TrimRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        VideoEditService.VideoResult result = videoEditService.trimVideo(request, userId);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "视频合并")
    @PostMapping("/video/merge")
    public RESTResult<VideoEditService.VideoResult> mergeVideos(
            @RequestBody VideoEditService.MergeRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        VideoEditService.VideoResult result = videoEditService.mergeVideos(request, userId);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "添加字幕")
    @PostMapping("/video/subtitle")
    public RESTResult<VideoEditService.VideoResult> addSubtitles(
            @RequestBody VideoEditService.SubtitleRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        VideoEditService.VideoResult result = videoEditService.addSubtitles(request, userId);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "添加背景音乐")
    @PostMapping("/video/music")
    public RESTResult<VideoEditService.VideoResult> addBackgroundMusic(
            @RequestBody VideoEditService.MusicRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        VideoEditService.VideoResult result = videoEditService.addBackgroundMusic(request, userId);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "视频转码")
    @PostMapping("/video/transcode")
    public RESTResult<VideoEditService.VideoResult> transcodeVideo(
            @RequestBody VideoEditService.TranscodeRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        VideoEditService.VideoResult result = videoEditService.transcodeVideo(request, userId);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "首尾帧生成视频")
    @PostMapping("/video/generate-from-frames")
    public RESTResult<VideoEditService.VideoResult> generateFromFrames(
            @RequestBody VideoGenerationService.GenerateFromFramesRequest req,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        int durationSec = (req.durationSec() != null && req.durationSec() > 0) ? req.durationSec() : 5;
        VideoEditService.VideoResult result = withAiCall(userId, "video_generate_frames", "video_gen",
                () -> videoGenerationService.generateFromFrames(req.startFrameUrl(), req.endFrameUrl(), durationSec, userId), null);
        return RESTResult.getSuccess(result);
    }

    @Operation(summary = "自动成片")
    @PostMapping("/video/auto-compose")
    public RESTResult<VideoEditService.VideoResult> autoCompose(
            @RequestBody VideoEditService.AutoComposeRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = getUserId(httpRequest);
        VideoEditService.VideoResult result = withAiCall(userId, "video_auto_compose", "video_gen",
                () -> videoEditService.autoCompose(request, userId), null);
        return RESTResult.getSuccess(result);
    }

    private <T> T withAiCall(Long userId, String callType, String modelCode, Supplier<T> supplier, String inputSummary) {
        aiQuotaService.ensureQuota(userId);
        long start = System.currentTimeMillis();
        try {
            T result = supplier.get();
            aiQuotaService.consume(userId);
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, modelCode, inputSummary, null, null, null, System.currentTimeMillis() - start, 1, null, false, null, null));
            return result;
        } catch (Exception e) {
            aiCallLogService.log(new AiCallLogService.LogEntry(userId, callType, null, modelCode, inputSummary, null, null, null, System.currentTimeMillis() - start, 0, e.getMessage(), false, null, null));
            throw e;
        }
    }

    private static String truncate(String s, int maxLen) {
        if (s == null) return null;
        return s.length() <= maxLen ? s : s.substring(0, maxLen);
    }

    private Long getUserId(HttpServletRequest request) {
        Long userId = AuthTokenFilter.getUserId(request);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        }
        return userId;
    }
}
