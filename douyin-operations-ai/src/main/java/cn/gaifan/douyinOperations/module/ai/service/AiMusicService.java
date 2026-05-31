package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * AI 音乐生成服务 (Phase 7)
 * 聚合多个 AiMusicProvider，按优先级选择可用实现
 */
public interface AiMusicService {

    /**
     * 生成 BGM
     *
     * @param styleDescription 风格描述 (如 "dark cinematic, suspenseful")
     * @param durationSec      时长 (秒)
     * @param instrumental    是否纯器乐
     * @return 音乐 URL，未配置时抛出异常
     */
    AiMusicProvider.MusicGenerationResult generateBgm(
            String styleDescription,
            int durationSec,
            boolean instrumental);

    /**
     * 获取已配置的 Provider 列表
     */
    List<String> getAvailableProviders();
}
