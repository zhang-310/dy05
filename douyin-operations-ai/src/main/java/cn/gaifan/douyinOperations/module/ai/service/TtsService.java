package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 语音合成服务
 */
public interface TtsService {

    /**
     * 文本转语音
     */
    AudioResult textToSpeech(TtsRequest request, Long userId);

    /**
     * 获取可用音色列表
     */
    List<VoiceInfo> getAvailableVoices();

    /**
     * 获取合成历史
     */
    List<TtsHistory> getHistory(Long userId, int page, int size);

    /**
     * TTS 请求
     * @param voiceId 克隆音色 ID（ElevenLabs 等），优先于 voice 使用
     */
    record TtsRequest(
            String text,
            String voice,
            String voiceId,
            String language,
            Double speed,
            Double pitch,
            String format
    ) {
        /** 兼容旧版 6 参数构造 */
        public TtsRequest(String text, String voice, String language, Double speed, Double pitch, String format) {
            this(text, voice, null, language, speed, pitch, format);
        }
    }

    /**
     * 音频结果
     */
    record AudioResult(
            String audioUrl,
            String text,
            Long duration,
            Long fileSize
    ) {}

    /**
     * 音色信息
     */
    record VoiceInfo(
            String id,
            String name,
            String language,
            String gender,
            String description
    ) {}

    /**
     * TTS 历史
     */
    record TtsHistory(
            Long id,
            String audioUrl,
            String text,
            String voice,
            Long createTime
    ) {}
}
