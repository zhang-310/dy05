package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 数字人口播统一接口 (Phase 8)
 * HeyGen Avatar IV 等实现
 */
public interface DigitalHumanProvider {

    String name();

    boolean isConfigured();

    /**
     * 生成数字人口播视频
     *
     * @param avatarId  Avatar ID
     * @param scriptText 口播文本
     * @param voiceId 音色 ID（可选）
     * @return 视频 URL
     */
    String generateTalkingHead(String avatarId, String scriptText, String voiceId);
}
