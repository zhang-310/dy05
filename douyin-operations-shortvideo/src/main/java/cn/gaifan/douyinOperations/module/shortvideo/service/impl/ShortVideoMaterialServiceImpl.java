package cn.gaifan.douyinOperations.module.shortvideo.service.impl;

import cn.gaifan.douyinOperations.common.constant.ErrorCode;
import cn.gaifan.douyinOperations.common.exception.BusinessException;
import cn.gaifan.douyinOperations.module.ai.domain.CameraType;
import cn.gaifan.douyinOperations.module.ai.domain.QualityLevel;
import cn.gaifan.douyinOperations.module.ai.domain.VideoAspectRatio;
import cn.gaifan.douyinOperations.module.ai.service.AiVideoProvider;
import cn.gaifan.douyinOperations.module.ai.service.CinematicKnowledgeService;
import cn.gaifan.douyinOperations.module.ai.service.CinematicPromptEngine;
import cn.gaifan.douyinOperations.module.ai.service.ComfyUIService;
import cn.gaifan.douyinOperations.module.ai.service.ImageGenerationService;
import cn.gaifan.douyinOperations.module.ai.service.IntelligentModelRouter;
import cn.gaifan.douyinOperations.module.ai.service.KlingVideoService;
import cn.gaifan.douyinOperations.module.ai.service.VideoPostProcessingService;
import cn.gaifan.douyinOperations.module.ai.service.TtsService;
import cn.gaifan.douyinOperations.module.ai.service.VideoEditService;
import cn.gaifan.douyinOperations.module.ai.service.VideoGenerationService;
import cn.gaifan.douyinOperations.module.shortvideo.entity.SvMaterial;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvMaterialRepository;
import cn.gaifan.douyinOperations.module.shortvideo.repository.SvShotRepository;
import cn.gaifan.douyinOperations.module.shortvideo.service.ShortVideoMaterialService;
import java.util.function.Consumer;
import cn.gaifan.douyinOperations.module.shortvideo.util.ShortVideoPathHelper;
import cn.gaifan.douyinOperations.module.storage.service.BosStorageService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import cn.gaifan.douyinOperations.common.util.ByteArrayMultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class ShortVideoMaterialServiceImpl implements ShortVideoMaterialService {

    private static final Logger log = LoggerFactory.getLogger(ShortVideoMaterialServiceImpl.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final HttpClient httpClient = HttpClient.newHttpClient();

    @Resource
    private ImageGenerationService imageGenerationService;
    @Resource
    private ComfyUIService comfyUIService;
    @Resource
    private TtsService ttsService;
    @Resource
    private VideoGenerationService videoGenerationService;
    @Resource
    private KlingVideoService klingVideoService;
    @Resource
    private IntelligentModelRouter intelligentModelRouter;
    @Resource
    private CinematicPromptEngine cinematicPromptEngine;
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CinematicKnowledgeService cinematicKnowledgeService;
    @Resource
    private VideoPostProcessingService videoPostProcessingService;
    @Resource
    private BosStorageService bosStorageService;
    @Resource
    private SvMaterialRepository materialRepository;
    @Resource
    private SvShotRepository shotRepository;

    @Override
    public List<KeyframeResult> generateKeyframes(Long projectId, Long shotListId, List<KeyframeInput> shots, Long ownerId) {
        return generateKeyframesWithProgress(projectId, shotListId, shots, ownerId, null);
    }

    @Override
    public List<KeyframeResult> generateKeyframesWithProgress(Long projectId, Long shotListId, List<KeyframeInput> shots,
                                                             Long ownerId, Consumer<ShortVideoMaterialService.ProgressEvent> progressCallback) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (shots == null || shots.isEmpty()) return List.of();
        List<KeyframeResult> results = new ArrayList<>();
        String date = LocalDate.now().format(DATE_FMT);
        int totalSteps = shots.size() * 2;  // 每镜 2 张：首帧 + 尾帧
        int step = 0;
        for (KeyframeInput input : shots) {
            String basePrompt = StringUtils.hasText(input.sceneDescription()) ? input.sceneDescription() : "高质量场景图";
            if (StringUtils.hasText(input.style())) basePrompt += "，风格：" + input.style();
            int sn = input.shotNumber() != null ? input.shotNumber() : (results.size() + 1);

            // 1. 首帧：动作起始（如拿起锅铲炒菜）
            if (progressCallback != null) {
                int pct = totalSteps > 0 ? (step * 100) / totalSteps : 0;
                progressCallback.accept(new ShortVideoMaterialService.ProgressEvent("关键帧", step, totalSteps, pct,
                        "分镜 " + sn + " 首帧（动作起始）"));
            }
            step++;
            String keyframeUrl = generateOneKeyframe(input, basePrompt, ownerId);
            String keyframeBosKey = null;
            if (StringUtils.hasText(keyframeUrl) && bosStorageService.isConfigured() && projectId != null) {
                byte[] bytes = downloadToBytes(keyframeUrl);
                if (bytes != null && bytes.length > 0) {
                    String key = ShortVideoPathHelper.keyframeKey(ownerId, date, projectId, sn);
                    try {
                        MultipartFile mf = new ByteArrayMultipartFile("file", "shot.jpg", "image/jpeg", bytes);
                        String url = bosStorageService.upload(key, mf);
                        if (StringUtils.hasText(url)) {
                            keyframeUrl = url;
                            keyframeBosKey = key;
                        }
                    } catch (Exception e) { log.warn("BOS上传失败: {}", e.getMessage()); }
                }
            }

            // 2. 尾帧：同场景动作延续（如炒菜进行中）
            if (progressCallback != null) {
                int pct = totalSteps > 0 ? (step * 100) / totalSteps : 0;
                progressCallback.accept(new ShortVideoMaterialService.ProgressEvent("关键帧", step, totalSteps, pct,
                        "分镜 " + sn + " 尾帧（动作延续）"));
            }
            step++;
            String endPrompt = basePrompt + "，镜头延续，动作进行中的画面";
            String endFrameUrl = generateOneKeyframe(input, endPrompt, ownerId);
            String endFrameBosKey = null;
            if (StringUtils.hasText(endFrameUrl) && bosStorageService.isConfigured() && projectId != null) {
                byte[] bytes = downloadToBytes(endFrameUrl);
                if (bytes != null && bytes.length > 0) {
                    String key = ShortVideoPathHelper.endFrameKey(ownerId, date, projectId, sn);
                    try {
                        MultipartFile mf = new ByteArrayMultipartFile("file", "shot_end.jpg", "image/jpeg", bytes);
                        String url = bosStorageService.upload(key, mf);
                        if (StringUtils.hasText(url)) {
                            endFrameUrl = url;
                            endFrameBosKey = key;
                        }
                    } catch (Exception e) { log.warn("BOS上传失败: {}", e.getMessage()); }
                }
            }

            results.add(new KeyframeResult(input.shotId(), input.shotNumber(), keyframeUrl, keyframeBosKey, basePrompt,
                    endFrameUrl, endFrameBosKey));
            if (projectId != null && StringUtils.hasText(keyframeUrl)) {
                saveMaterial(ownerId, "image", keyframeUrl, keyframeBosKey, input.shotId(), projectId, null, "ai", basePrompt, null);
            }
            if (input.shotId() != null && (StringUtils.hasText(keyframeUrl) || StringUtils.hasText(endFrameUrl))) {
                updateShotKeyframes(input.shotId(), keyframeUrl, keyframeBosKey, endFrameUrl, endFrameBosKey);
            }
        }
        if (progressCallback != null) {
            progressCallback.accept(new ShortVideoMaterialService.ProgressEvent("关键帧", totalSteps, totalSteps, 100, "完成"));
        }
        return results;
    }

    /** 生成单张关键帧（首帧或尾帧），复用 ComfyUI/图生图/文生图逻辑 */
    private String generateOneKeyframe(KeyframeInput input, String prompt, Long ownerId) {
        String imageUrl = null;
        boolean useComfy = comfyUIService != null && comfyUIService.isAvailable()
                && (StringUtils.hasText(input.characterReferenceUrl()) || StringUtils.hasText(input.sceneReferenceUrl()));
        if (useComfy) {
            try {
                String path = comfyUIService.generateKeyframe(
                        prompt, input.characterReferenceUrl(), input.sceneReferenceUrl(),
                        768, 1344, 20, 7.0);
                imageUrl = path;
            } catch (Exception e) {
                log.debug("ComfyUI 关键帧生成失败，降级: {}", e.getMessage());
                useComfy = false;
            }
        }
        if (!useComfy) {
            boolean hasRef = StringUtils.hasText(input.characterReferenceUrl()) || StringUtils.hasText(input.sceneReferenceUrl());
            String refUrl = StringUtils.hasText(input.characterReferenceUrl()) ? input.characterReferenceUrl() : input.sceneReferenceUrl();
            if (hasRef && refUrl != null) {
                try {
                    ImageGenerationService.ImageToImageRequest imgReq = new ImageGenerationService.ImageToImageRequest(
                            refUrl, prompt, null, 0.7, 20, 7.0);
                    ImageGenerationService.ImageResult ir = imageGenerationService.imageToImage(imgReq, ownerId);
                    imageUrl = ir != null ? ir.imageUrl() : null;
                } catch (Exception e) {
                    log.warn("图生图（参考图）失败，降级文生图: {}", e.getMessage());
                    hasRef = false;
                }
            }
            if (!hasRef || imageUrl == null) {
                ImageGenerationService.TextToImageRequest req = new ImageGenerationService.TextToImageRequest(
                        prompt, null, input.style(), 768, 1344, 20, 7.0, null);
                ImageGenerationService.ImageResult ir = imageGenerationService.textToImage(req, ownerId);
                imageUrl = ir != null ? ir.imageUrl() : null;
            }
        }
        return imageUrl;
    }

    @Override
    public List<VoiceResult> generateVoiceBatch(Long projectId, Long shotListId, List<VoiceInput> shots, Long ownerId) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (shots == null || shots.isEmpty()) return List.of();
        List<VoiceResult> results = new ArrayList<>();
        String date = LocalDate.now().format(DATE_FMT);
        for (VoiceInput input : shots) {
            if (!StringUtils.hasText(input.dialogue())) {
                results.add(new VoiceResult(input.shotId(), input.shotNumber(), null, null, 0.0));
                continue;
            }
            TtsService.TtsRequest req = new TtsService.TtsRequest(
                    input.dialogue(), input.voice(), "zh-CN",
                    input.speed() != null ? input.speed() : 1.0, null, "mp3");
            TtsService.AudioResult ar = ttsService.textToSpeech(req, ownerId);
            String audioUrl = ar != null ? ar.audioUrl() : null;
            Double duration = ar != null && ar.duration() != null ? ar.duration() / 1000.0 : 0.0;
            String bosKey = null;
            if (StringUtils.hasText(audioUrl) && bosStorageService.isConfigured() && projectId != null) {
                byte[] bytes = downloadToBytes(audioUrl);
                if (bytes != null && bytes.length > 0) {
                    int sn = input.shotNumber() != null ? input.shotNumber() : (results.size() + 1);
                    String key = ShortVideoPathHelper.audioKey(ownerId, date, projectId, sn);
                    try {
                        MultipartFile mf = new ByteArrayMultipartFile("file", "voice.mp3", "audio/mpeg", bytes);
                        String url = bosStorageService.upload(key, mf);
                        if (StringUtils.hasText(url)) {
                            audioUrl = url;
                            bosKey = key;
                        }
                    } catch (Exception e) { log.warn("BOS上传失败: {}", e.getMessage()); }
                }
            }
            results.add(new VoiceResult(input.shotId(), input.shotNumber(), audioUrl, bosKey, duration));
            if (projectId != null && StringUtils.hasText(audioUrl)) {
                saveMaterial(ownerId, "audio", audioUrl, bosKey, input.shotId(), projectId,
                        duration != null ? (int) Math.round(duration) : null, "ai", null, "mp3");
            }
            if (input.shotId() != null && StringUtils.hasText(audioUrl)) {
                updateShotAudio(input.shotId(), audioUrl, bosKey);
            }
        }
        return results;
    }

    /** Phase 4.1: 并发生成上限 (3 路) */
    private static final int PARALLEL_VIDEO_LIMIT = 3;

    @Override
    public List<VideoResult> img2videoBatch(Long projectId, Long shotListId, List<Img2VideoInput> keyframes, Long ownerId) {
        return img2videoBatchWithProgress(projectId, shotListId, keyframes, ownerId, null);
    }

    @Override
    public List<VideoResult> img2videoBatchWithProgress(Long projectId, Long shotListId, List<Img2VideoInput> keyframes,
                                                       Long ownerId, Consumer<ShortVideoMaterialService.ProgressEvent> progressCallback) {
        if (ownerId == null) throw new BusinessException(ErrorCode.UNAUTHORIZED, "未登录");
        if (keyframes == null || keyframes.isEmpty()) return List.of();
        String date = LocalDate.now().format(DATE_FMT);
        Semaphore semaphore = new Semaphore(PARALLEL_VIDEO_LIMIT);
        AtomicInteger completed = new AtomicInteger(0);
        List<CompletableFuture<VideoResult>> futures = new ArrayList<>();
        for (int i = 0; i < keyframes.size(); i++) {
            Img2VideoInput input = keyframes.get(i);
            int index = i;
            CompletableFuture<VideoResult> future = CompletableFuture.supplyAsync(() -> {
                try {
                    semaphore.acquire();
                    try {
                        return processOneVideo(input, index, projectId, ownerId, date);
                    } finally {
                        semaphore.release();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return new VideoResult(input.shotId(), input.shotNumber(), null, null, 0);
                }
            }).whenComplete((result, ex) -> {
                int done = completed.incrementAndGet();
                if (progressCallback != null) {
                    int total = keyframes.size();
                    int pct = total > 0 ? done * 100 / total : 100;
                    int sn = input.shotNumber() != null ? input.shotNumber() : (index + 1);
                    progressCallback.accept(new ShortVideoMaterialService.ProgressEvent("video", done, total, pct, "分镜 " + sn + " 完成"));
                }
            });
            futures.add(future);
        }
        List<VideoResult> results = new ArrayList<>();
        for (CompletableFuture<VideoResult> f : futures) {
            try {
                results.add(f.join());
            } catch (Exception e) {
                log.warn("并发生成异常: {}", e.getMessage());
                int idx = results.size();
                Img2VideoInput input = keyframes.get(idx);
                results.add(new VideoResult(input.shotId(), input.shotNumber(), null, null, 0));
            }
        }
        return results;
    }

    /** 单镜视频生成 (供并发生成调用) */
    private VideoResult processOneVideo(Img2VideoInput input, int index, Long projectId, Long ownerId, String date) {
        String startFrameUrl = input.imageUrl();
        if (!StringUtils.hasText(startFrameUrl)) {
            return new VideoResult(input.shotId(), input.shotNumber(), null, null, 0);
        }
        long startMs = System.currentTimeMillis();
        String provider = "ffmpeg";
        int sn = input.shotNumber() != null ? input.shotNumber() : (index + 1);
        String endFrameUrl = StringUtils.hasText(input.endFrameUrl()) ? input.endFrameUrl() : startFrameUrl;
        int duration = input.duration() != null && input.duration() >= 1 && input.duration() <= 10 ? input.duration() : 5;
        QualityLevel quality = QualityLevel.fromCode(input.quality());
        VideoAspectRatio aspectRatio = VideoAspectRatio.fromRatio(input.aspectRatio());
        String sceneDesc = input.sceneDescription();
        String cameraCode = StringUtils.hasText(input.cameraType()) ? input.cameraType()
                : (cinematicKnowledgeService != null ? cinematicKnowledgeService.recommendCameraCode(sceneDesc) : "zoom-in");
        CameraType cameraType = CameraType.fromCode(cameraCode);
        String mood = input.mood();
        String action = input.action();
        String motionPrompt = cinematicPromptEngine != null
                ? cinematicPromptEngine.generatePrompt(sceneDesc, cameraType, mood, action, quality)
                : buildMotionPrompt(input);

        try {
            String videoUrl = null;
            String bosKey = null;
            if (intelligentModelRouter != null && cinematicPromptEngine != null) {
                String negPrompt = cinematicPromptEngine.generateNegativePrompt(quality);
                AiVideoProvider.VideoGenerationRequest req = new AiVideoProvider.VideoGenerationRequest(
                        startFrameUrl, endFrameUrl, motionPrompt, negPrompt,
                        duration, aspectRatio.getRatio(), quality.getKlingMode());
                try {
                    AiVideoProvider.VideoGenerationResult genResult =
                            intelligentModelRouter.generateWithSmartRouting(req, quality, sceneDesc);
                    if (genResult != null) provider = genResult.provider();
                    String klingUrl = genResult != null ? genResult.videoUrl() : null;
                    if (StringUtils.hasText(klingUrl) && bosStorageService.isConfigured() && projectId != null) {
                        String key = ShortVideoPathHelper.videoClipKey(ownerId, date, projectId, sn);
                        try {
                            videoUrl = bosStorageService.putObjectFromUrl(key, klingUrl);
                            bosKey = key;
                        } catch (Exception e) {
                            log.warn("BOS 回源拉取失败，降级为下载上传: {}", e.getMessage());
                        }
                    }
                    if (videoUrl == null && StringUtils.hasText(klingUrl)) videoUrl = klingUrl;
                } catch (Exception e) {
                    log.warn("智能路由图生视频失败，降级 FFmpeg: {}", e.getMessage());
                }
            }
            if (videoUrl == null && klingVideoService != null && klingVideoService.isConfigured()) {
                provider = "kling";
                String klingUrl = klingVideoService.img2videoUrl(startFrameUrl, duration, motionPrompt, ownerId, quality.getKlingMode());
                if (StringUtils.hasText(klingUrl) && bosStorageService.isConfigured() && projectId != null) {
                    String key = ShortVideoPathHelper.videoClipKey(ownerId, date, projectId, sn);
                    try {
                        videoUrl = bosStorageService.putObjectFromUrl(key, klingUrl);
                        bosKey = key;
                    } catch (Exception e) {
                        log.warn("BOS 回源拉取失败，降级为下载上传: {}", e.getMessage());
                    }
                }
                if (videoUrl == null && StringUtils.hasText(klingUrl)) videoUrl = klingUrl;
            }
            if (videoUrl == null) {
                VideoEditService.VideoResult vr = null;
                if (klingVideoService != null && klingVideoService.isConfigured()) {
                    try {
                        vr = klingVideoService.img2video(startFrameUrl, duration, motionPrompt, ownerId);
                    } catch (Exception e) {
                        log.debug("Kling 图生视频失败，降级 FFmpeg: {}", e.getMessage());
                        try {
                            vr = videoGenerationService.generateFromFrames(startFrameUrl, endFrameUrl, duration, ownerId);
                        } catch (Exception fe) {
                            log.warn("FFmpeg 首尾帧合成失败 shotId={}: {}", input.shotId(), fe.getMessage());
                            throw fe;
                        }
                    }
                } else {
                    vr = videoGenerationService.generateFromFrames(startFrameUrl, endFrameUrl, duration, ownerId);
                }
                videoUrl = vr != null ? vr.videoUrl() : null;
                if (StringUtils.hasText(videoUrl) && videoPostProcessingService != null) {
                    File f = new File(videoUrl);
                    if (f.exists() && f.isFile()) {
                        videoUrl = videoPostProcessingService.processVideo(videoUrl,
                                VideoPostProcessingService.PostProcessConfig.defaults());
                    }
                }
                if (StringUtils.hasText(videoUrl) && bosStorageService.isConfigured() && projectId != null) {
                    File f = new File(videoUrl);
                    if (f.exists() && f.length() > 0) {
                        byte[] bytes = Files.readAllBytes(f.toPath());
                        String key = ShortVideoPathHelper.videoClipKey(ownerId, date, projectId, sn);
                        try {
                            MultipartFile mf = new ByteArrayMultipartFile("file", "shot.mp4", "video/mp4", bytes);
                            videoUrl = bosStorageService.upload(key, mf);
                            bosKey = key;
                        } catch (Exception e) { log.warn("BOS上传失败: {}", e.getMessage()); }
                        try { Files.deleteIfExists(f.toPath()); } catch (Exception e) { log.debug("临时文件删除失败: {}", e.getMessage()); }
                    }
                }
            }
            VideoResult result = new VideoResult(input.shotId(), input.shotNumber(), videoUrl, bosKey, duration);
            if (projectId != null && StringUtils.hasText(videoUrl)) {
                saveMaterial(ownerId, "video", videoUrl, bosKey, input.shotId(), projectId, duration, "ai", null, "mp4");
            }
            if (input.shotId() != null && StringUtils.hasText(videoUrl)) {
                updateShotVideo(input.shotId(), videoUrl, bosKey);
            }
            if (cinematicKnowledgeService != null) {
                long elapsed = System.currentTimeMillis() - startMs;
                cinematicKnowledgeService.logGeneration(projectId, input.shotId(), cameraCode, provider, true, null, elapsed, motionPrompt, null);
            }
            return result;
        } catch (Exception e) {
            if (cinematicKnowledgeService != null) {
                long elapsed = System.currentTimeMillis() - startMs;
                cinematicKnowledgeService.logGeneration(projectId, input.shotId(), cameraCode, provider, false, null, elapsed, motionPrompt, e.getMessage());
            }
            return new VideoResult(input.shotId(), input.shotNumber(), null, null, 0);
        }
    }

    /**
     * 根据分镜输入构建智能运镜 Prompt，提升 Kling 视频生成质量
     * 结合场景描述、运动类型等生成更精准的运镜指令
     */
    private String buildMotionPrompt(Img2VideoInput input) {
        StringBuilder prompt = new StringBuilder();

        // 1. 基础运镜指令
        String motion = input.motion() != null ? input.motion() : "zoom-in";
        String sceneDesc = input.sceneDescription();

        // 2. 根据运动类型添加专业运镜指令
        switch (motion) {
            case "zoom-in":
                prompt.append("缓慢推进镜头，聚焦画面主体，");
                break;
            case "zoom-out":
                prompt.append("缓慢拉远镜头，展现全景视野，");
                break;
            case "pan-left":
                prompt.append("平滑左移镜头，横向展示场景，");
                break;
            case "pan-right":
                prompt.append("平滑右移镜头，横向展示场景，");
                break;
            case "tilt-up":
                prompt.append("缓慢仰拍镜头，从下至上，");
                break;
            case "tilt-down":
                prompt.append("缓慢俯拍镜头，从上至下，");
                break;
            case "dolly":
                prompt.append("推轨镜头，景深变化，");
                break;
            case "static":
                prompt.append("静态固定镜头，");
                break;
            default:
                prompt.append("自然运镜，");
        }

        // 3. 添加场景相关描述
        if (StringUtils.hasText(sceneDesc)) {
            // 提取关键动作词和场景元素
            String cleanDesc = sceneDesc.trim();
            if (cleanDesc.length() > 50) {
                cleanDesc = cleanDesc.substring(0, 50); // 限制长度，避免 prompt 过长
            }
            prompt.append(cleanDesc).append("，");
        }

        // 4. 添加质量和风格要求
        prompt.append("电影级画面质感，流畅自然的动态过渡，");
        prompt.append("保持主体清晰，避免模糊失真，");
        prompt.append("高清细腻，光影层次丰富");

        return prompt.toString();
    }

    private void updateShotVideo(Long shotId, String videoUrl, String bosKey) {
        try {
            shotRepository.findById(shotId).ifPresent(s -> {
                s.setVideoUrl(videoUrl);
                s.setVideoBosKey(bosKey);
                shotRepository.save(s);
            });
        } catch (Exception e) { log.debug("updateShotVideo失败 shotId={}: {}", shotId, e.getMessage()); }
    }

    private void updateShotKeyframes(Long shotId, String keyframeUrl, String keyframeBosKey,
                                     String endFrameUrl, String endFrameBosKey) {
        try {
            shotRepository.findById(shotId).ifPresent(s -> {
                if (StringUtils.hasText(keyframeUrl)) {
                    s.setKeyframeUrl(keyframeUrl);
                    s.setKeyframeBosKey(keyframeBosKey);
                }
                if (StringUtils.hasText(endFrameUrl)) {
                    s.setEndFrameUrl(endFrameUrl);
                    s.setEndFrameBosKey(endFrameBosKey);
                }
                shotRepository.save(s);
            });
        } catch (Exception e) { log.debug("updateShotKeyframes失败 shotId={}: {}", shotId, e.getMessage()); }
    }

    private void updateShotAudio(Long shotId, String audioUrl, String bosKey) {
        try {
            shotRepository.findById(shotId).ifPresent(s -> {
                s.setAudioUrl(audioUrl);
                s.setAudioBosKey(bosKey);
                shotRepository.save(s);
            });
        } catch (Exception e) { log.debug("updateShotAudio失败 shotId={}: {}", shotId, e.getMessage()); }
    }

    private void saveMaterial(Long ownerId, String materialType, String url, String bosKey,
                              Long shotId, Long projectId, Integer duration, String generationType,
                              String aiPrompt, String formatType) {
        try {
            SvMaterial m = new SvMaterial();
            m.setOwnerId(ownerId);
            m.setMaterialType(materialType);
            m.setUrl(url);
            m.setBosKey(bosKey);
            m.setShotId(shotId);
            m.setProjectId(projectId);
            m.setDuration(duration);
            m.setGenerationType(generationType);
            m.setAiPrompt(aiPrompt);
            m.setFormatType(formatType);
            materialRepository.save(m);
        } catch (Exception e) { log.debug("saveMaterial失败: {}", e.getMessage()); }
    }

    private byte[] downloadToBytes(String url) {
        if (!StringUtils.hasText(url)) return null;
        try {
            if (url.startsWith("/") && !url.startsWith("//")) {
                Path p = Path.of(url);
                if (Files.exists(p)) return Files.readAllBytes(p);
                p = Path.of(System.getProperty("user.dir", "."), "uploads", url.replaceFirst("^/uploads/", ""));
                if (Files.exists(p)) return Files.readAllBytes(p);
                return null;
            }
            if (url.startsWith("http://") || url.startsWith("https://")) {
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
                HttpResponse<byte[]> resp = httpClient.send(req, HttpResponse.BodyHandlers.ofByteArray());
                if (resp.statusCode() == 200) return resp.body();
            }
            // 本地文件路径（如 ComfyUI 输出）
            Path p = Path.of(url);
            if (Files.exists(p)) return Files.readAllBytes(p);
        } catch (Exception e) { log.debug("downloadToBytes失败 url={}: {}", url, e.getMessage()); }
        return null;
    }
}
