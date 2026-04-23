package cn.gaifan.douyinOperations.module.ai.service;

/**
 * Kling（快手可灵）图生视频服务
 * 配置 KLING_API_KEY 后可用，支持电影级运镜
 */
public interface KlingVideoService {

    /**
     * 根据图片 URL 生成视频
     *
     * @param imageUrl  关键帧图片 URL（需公网可访问，BOS 公网 URL 可直接传）
     * @param duration  视频时长（秒，1-10）
     * @param prompt    运镜提示词（可选，如「自然运镜，电影级质感」）
     * @param ownerId   用户 ID
     * @return 生成后的视频本地路径（临时文件，调用方负责清理或上传 BOS）
     */
    VideoEditService.VideoResult img2video(
            String imageUrl,
            int duration,
            String prompt,
            Long ownerId
    );

    /**
     * 根据图片 URL 生成视频，返回 Kling 生成结果的公网 URL（用于 BOS 回源拉取，避免下载-上传双倍流量）
     *
     * @param imageUrl  关键帧图片 URL（需公网可访问）
     * @param duration  视频时长（秒，1-10）
     * @param prompt    运镜提示词
     * @param ownerId   用户 ID
     * @return Kling 返回的视频公网 URL，失败返回 null
     */
    String img2videoUrl(String imageUrl, int duration, String prompt, Long ownerId);

    /**
     * 同上，支持指定 mode（fast/standard/pro/max），null 时默认 pro
     */
    String img2videoUrl(String imageUrl, int duration, String prompt, Long ownerId, String mode);

    /**
     * 是否已配置 Kling API Key
     */
    boolean isConfigured();
}
