package cn.gaifan.douyinOperations.module.ai.service;

import cn.gaifan.douyinOperations.module.ai.domain.QualityLevel;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 音视频联合生成服务 (Phase 8)
 *
 * 模式A: 联合生成 (Seedance 2.0 / Kling 3.0 / Veo 3.1)
 *   输入: 场景描述 + 角色对话文本 + 音效提示
 *   输出: 含音频的完整视频
 *
 * 模式B: 分离管线 (MiniMax / Runway / Luma / Wan / Pika)
 *   输出: 无声视频 → TTS 配音 → BGM → SFX → FFmpeg 混合
 */
@Service
public class AudioVideoJointService {

    private static final Logger log = LoggerFactory.getLogger(AudioVideoJointService.class);

    @Resource
    private IntelligentModelRouter modelRouter;

    /**
     * 智能选择联合生成或分离管线
     */
    public AudioVideoResult generateWithAudio(
            AiVideoProvider.VideoGenerationRequest request,
            QualityLevel quality,
            String sceneDesc
    ) {
        if (request.dialogueText() != null || request.sfxHints() != null) {
            AiVideoProvider jointProvider = findJointCapableProvider();
            if (jointProvider != null) {
                try {
                    log.info("使用联合生成模式: provider={}", jointProvider.name());
                    var result = jointProvider.generateVideo(request);
                    return new AudioVideoResult(result.videoUrl(), result.provider(),
                            result.durationMs(), true, result.hasAudio());
                } catch (Exception e) {
                    log.warn("联合生成失败，降级为分离管线: {}", e.getMessage());
                }
            }
        }
        log.info("使用分离管线模式");
        var videoResult = modelRouter.generateWithSmartRouting(request, quality, sceneDesc);
        return new AudioVideoResult(videoResult.videoUrl(), videoResult.provider(),
                videoResult.durationMs(), videoResult.isRemoteUrl(), videoResult.hasAudio());
    }

    private AiVideoProvider findJointCapableProvider() {
        for (AiVideoProvider p : modelRouter.getProviders()) {
            if (p.isConfigured() && p.supportsAudioVideoJoint()) {
                return p;
            }
        }
        return null;
    }

    public record AudioVideoResult(
            String videoUrl,
            String provider,
            int durationMs,
            boolean isRemoteUrl,
            boolean hasIntegratedAudio
    ) {}
}
