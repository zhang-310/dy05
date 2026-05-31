package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.exception.VideoGenerationException;
import cn.gaifan.douyinOperations.module.ai.service.AiVideoProvider;
import cn.gaifan.douyinOperations.module.ai.service.KlingVideoService;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Kling 3.0 (快手) 视频生成
 *
 * 核心优势 (相比 Kling 2.x):
 *   - 最长 3 分钟视频
 *   - 多角色原生音频 (对话+音效)
 *   - 架构级跨镜头主体一致性 (MultiShotMaster)
 *
 * 当前实现复用 KlingVideoService，仅当 app.ai.kling.api-version=v3 时启用。
 * 未来可扩展 v3 专用 API (Motion Brush、6-cut Storyboard 等)。
 */
@Component
public class Kling3VideoProvider implements AiVideoProvider {

    private static final Logger log = LoggerFactory.getLogger(Kling3VideoProvider.class);

    @Resource
    private KlingVideoService klingVideoService;

    @Value("${app.ai.kling.api-version:v1}")
    private String apiVersion;

    @Override
    public String name() { return "kling3"; }

    @Override
    public boolean isConfigured() {
        return klingVideoService != null && klingVideoService.isConfigured()
                && "v3".equalsIgnoreCase(StringUtils.hasText(apiVersion) ? apiVersion.trim() : "v1");
    }

    @Override
    public boolean supportsAudioVideoJoint() { return true; }

    @Override
    public boolean supportsMultiReference() { return true; }

    @Override
    public int maxReferenceImages() { return 6; }

    @Override
    public String[] contentStrengths() {
        return new String[]{"portrait", "dialogue", "lip_sync", "multi_shot_consistency"};
    }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        // 复用 Kling 服务，支持更长时长 (Kling 3 最长 180 秒)
        int duration = Math.min(Math.max(request.duration(), 1), 180);
        String videoUrl = klingVideoService.img2videoUrl(
                request.imageUrl(),
                duration,
                request.prompt(),
                null,
                request.quality() != null ? request.quality() : "pro"
        );

        if (videoUrl == null || videoUrl.isBlank()) {
            throw new VideoGenerationException(name(), "Kling 3.0 返回视频 URL 为空");
        }

        return new VideoGenerationResult(
                videoUrl,
                "kling3",
                duration * 1000,
                true,
                request.dialogueText() != null || request.sfxHints() != null
        );
    }
}
