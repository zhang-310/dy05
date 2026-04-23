package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 视频生成服务：首尾帧图片 → 视频片段
 * 用于 AI 多媒体生成（如首尾帧过渡视频）
 */
public interface VideoGenerationService {

    /** 首尾帧生成视频请求 */
    record GenerateFromFramesRequest(String startFrameUrl, String endFrameUrl, Integer durationSec) {}

    /**
     * 根据首尾帧图片生成过渡视频片段
     *
     * @param startFrameUrl 首帧图片 URL
     * @param endFrameUrl   尾帧图片 URL
     * @param durationSec   持续时间（秒）
     * @param userId        用户 ID
     * @return 生成后的视频 URL
     */
    VideoEditService.VideoResult generateFromFrames(
            String startFrameUrl,
            String endFrameUrl,
            int durationSec,
            Long userId
    );
}
