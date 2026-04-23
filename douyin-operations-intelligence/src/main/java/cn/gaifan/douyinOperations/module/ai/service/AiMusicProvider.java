package cn.gaifan.douyinOperations.module.ai.service;

/**
 * AI 音乐生成统一接口 (Phase 7)
 * 实现: SunoMusicProvider, UdioMusicProvider
 */
public interface AiMusicProvider {

    String name();

    boolean isConfigured();

    /**
     * 生成 BGM
     *
     * @param request 音乐生成请求
     * @return 音乐文件 URL
     */
    MusicGenerationResult generateMusic(MusicGenerationRequest request);

    record MusicGenerationRequest(
            String styleDescription,
            int bpm,
            int durationSec,
            boolean instrumental,
            String referenceUrl
    ) {}

    record MusicGenerationResult(
            String musicUrl,
            String provider,
            int durationMs,
            int bpm
    ) {}
}
