package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * AI 视频生成统一接口
 * 所有模型 (Kling3/Seedance2/Veo/MiniMax/Runway/Luma/Wan/Pika) 实现此接口
 */
public interface AiVideoProvider {

    /** 提供者名称 */
    String name();

    /** 是否已配置可用 */
    boolean isConfigured();

    /** 是否支持指定宽高比 */
    default boolean supportsAspectRatio(String ratio) { return true; }

    /** 是否支持 negative prompt */
    default boolean supportsNegativePrompt() { return false; }

    /** 是否支持首尾帧 */
    default boolean supportsEndFrame() { return false; }

    /** 是否支持音视频联合生成 */
    default boolean supportsAudioVideoJoint() { return false; }

    /** 是否支持多参考图输入 */
    default boolean supportsMultiReference() { return false; }

    /** 最大参考图数量 */
    default int maxReferenceImages() { return 1; }

    /** 擅长的内容类型标签 (用于智能路由) */
    default String[] contentStrengths() { return new String[]{}; }

    /**
     * 图生视频 - 核心方法
     */
    VideoGenerationResult generateVideo(VideoGenerationRequest request);

    /** 生成请求 */
    record VideoGenerationRequest(
        String imageUrl,           // 首帧图片 URL (必需)
        String endFrameUrl,        // 尾帧图片 URL (可选, 仅部分模型支持)
        String prompt,             // 正向 prompt
        String negativePrompt,     // 负向 prompt (仅部分模型支持)
        int duration,              // 时长 (秒, 5 或 10)
        String aspectRatio,        // 宽高比: "9:16", "16:9", "1:1"
        String quality,            // 质量模式: "standard", "pro", "max"
        String dialogueText,       // 角色对话文本 (音视频联合生成用)
        String sfxHints,           // 音效提示
        List<String> referenceImageUrls, // 多参考图 URL 列表
        String characterPromptTags // 角色特征加权标签
    ) {
        /** 兼容旧版构造 (7参数) */
        public VideoGenerationRequest(String imageUrl, String endFrameUrl, String prompt,
                                       String negativePrompt, int duration, String aspectRatio,
                                       String quality) {
            this(imageUrl, endFrameUrl, prompt, negativePrompt, duration, aspectRatio, quality,
                 null, null, null, null);
        }
    }

    /** 生成结果 */
    record VideoGenerationResult(
        String videoUrl,           // 视频 URL (远程或本地)
        String provider,           // 提供者名称
        int durationMs,            // 实际时长 (毫秒)
        boolean isRemoteUrl,       // 是否远程 URL
        boolean hasAudio           // 是否包含音频 (联合生成时为 true)
    ) {
        /** 兼容旧版构造 (4参数) */
        public VideoGenerationResult(String videoUrl, String provider, int durationMs, boolean isRemoteUrl) {
            this(videoUrl, provider, durationMs, isRemoteUrl, false);
        }
    }
}
