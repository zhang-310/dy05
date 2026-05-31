package cn.gaifan.douyinOperations.module.ai.service;

/**
 * 声音克隆服务 (Phase 7/8)
 * 5秒样本 → 固定音色，用于短剧角色配音
 * 基于 ElevenLabs Voice Cloning API
 */
public interface VoiceCloneService {

    /**
     * 从音频样本创建克隆音色
     *
     * @param sampleUrl 样本音频 URL（5-30秒，需公网可访问）
     * @param voiceName 音色名称（用于展示）
     * @param userId    用户 ID
     * @return 克隆后的 voice_id，可用于 TTS
     */
    String cloneFromSample(String sampleUrl, String voiceName, Long userId);

    /**
     * 使用克隆音色进行 TTS
     *
     * @param voiceId 克隆得到的 voice_id
     * @param text    待合成文本
     * @param userId  用户 ID
     * @return 合成后的音频 URL
     */
    TtsService.AudioResult synthesizeWithClonedVoice(String voiceId, String text, Long userId);

    /** 是否已配置 ElevenLabs API */
    boolean isConfigured();
}
