package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * AI 音效 (SFX) 生成服务 (Phase 7)
 *
 * 功能:
 * 1. 从场景描述自动提取音效关键词 ("雨中奔跑" → rain, footsteps, thunder)
 * 2. 调用 ElevenLabs SFX API 生成音效
 * 3. 输出音效文件 URL 列表，供合成阶段使用
 */
public interface SfxGenerationService {

    /**
     * 从场景描述提取音效关键词并生成音效
     *
     * @param sceneDescription 场景描述 (中文)
     * @param durationSec      音效时长 (秒)
     * @param userId           用户 ID（用于 BOS 路径隔离，可 null）
     * @return 音效结果列表
     */
    List<SfxResult> generateSfxFromScene(String sceneDescription, double durationSec, Long userId);

    /**
     * 按描述直接生成音效
     *
     * @param description 音效描述 (英文，如 "rain falling on pavement")
     * @param durationSec 时长 (秒)
     * @param userId      用户 ID（用于 BOS 路径隔离，可 null）
     * @return 音效结果
     */
    SfxResult generateSfx(String description, double durationSec, Long userId);

    record SfxResult(String description, String audioUrl, double durationSec) {}
}
