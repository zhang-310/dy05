package cn.gaifan.douyinOperations.module.ai.service.impl;

import cn.gaifan.douyinOperations.module.ai.exception.VideoGenerationException;
import cn.gaifan.douyinOperations.module.ai.service.AiVideoProvider;
import cn.gaifan.douyinOperations.module.ai.service.KlingVideoService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

/**
 * Kling 视频提供者 - 适配现有 KlingVideoServiceImpl
 *
 * Kling API 实际参数 (submitTask 方法):
 *   POST /v1/videos/image2video
 *   Body: { "image_url", "duration", "prompt", "mode":"pro", "fps":24 }
 *
 * 附录 A.3: ownerId 暂传 null（或从 RequestContext 获取）
 */
@Component
public class KlingVideoProvider implements AiVideoProvider {

    @Resource
    private KlingVideoService klingVideoService;

    @Override
    public String name() { return "kling"; }

    @Override
    public boolean isConfigured() {
        return klingVideoService != null && klingVideoService.isConfigured();
    }

    @Override
    public boolean supportsNegativePrompt() { return false; }

    @Override
    public boolean supportsEndFrame() { return false; }

    @Override
    public VideoGenerationResult generateVideo(VideoGenerationRequest request) {
        String mode = request.quality() != null ? request.quality() : "pro";
        String videoUrl = klingVideoService.img2videoUrl(
                request.imageUrl(),
                request.duration(),
                request.prompt(),
                null, // 附录 A.3: ownerId 暂传 null
                mode
        );

        if (videoUrl == null || videoUrl.isBlank()) {
            throw new VideoGenerationException(name(), "Kling 返回视频 URL 为空");
        }

        return new VideoGenerationResult(
                videoUrl,
                "kling",
                request.duration() * 1000,
                true
        );
    }
}
