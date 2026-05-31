package cn.gaifan.douyinOperations.module.ai.service;

import java.util.List;

/**
 * 视频编辑服务
 */
public interface VideoEditService {

    /**
     * 视频剪辑
     */
    VideoResult trimVideo(TrimRequest request, Long userId);

    /**
     * 视频合并
     */
    VideoResult mergeVideos(MergeRequest request, Long userId);

    /**
     * 添加字幕
     */
    VideoResult addSubtitles(SubtitleRequest request, Long userId);

    /**
     * 添加背景音乐
     */
    VideoResult addBackgroundMusic(MusicRequest request, Long userId);

    /**
     * 视频转码
     */
    VideoResult transcodeVideo(TranscodeRequest request, Long userId);

    /**
     * 自动成片
     */
    VideoResult autoCompose(AutoComposeRequest request, Long userId);

    /**
     * 3轨混音 (TTS+BGM+SFX) - Phase 8
     * 将视频原音、TTS 配音、BGM、SFX 混合为单轨输出
     */
    VideoResult addTtsBgmSfxMix(TtsBgmSfxMixRequest request, Long userId);

    /**
     * 剪辑请求
     */
    record TrimRequest(
            String videoUrl,
            Double startTime,
            Double endTime
    ) {}

    /**
     * 合并请求
     */
    record MergeRequest(
            List<String> videoUrls,
            String transition
    ) {}

    /**
     * 字幕请求
     */
    record SubtitleRequest(
            String videoUrl,
            List<SubtitleItem> subtitles,
            String fontFamily,
            Integer fontSize,
            String fontColor
    ) {}

    /**
     * 字幕项
     */
    record SubtitleItem(
            Double startTime,
            Double endTime,
            String text
    ) {}

    /**
     * 音乐请求
     */
    record MusicRequest(
            String videoUrl,
            String musicUrl,
            Double volume,
            Boolean fadeIn,
            Boolean fadeOut
    ) {}

    /**
     * 转码请求
     */
    record TranscodeRequest(
            String videoUrl,
            String format,
            String resolution,
            Integer bitrate
    ) {}

    /**
     * 自动成片请求
     * @param voiceClipUrls 配音 URL 列表（与视频一一对应）
     * @param sfxUrls 音效 URL 列表（可选，3轨混音用）
     * @param subtitles 字幕列表（可选，无则从 scriptText 简单切分生成）
     */
    record AutoComposeRequest(
            List<String> videoClips,
            List<String> imageUrls,
            String scriptText,
            String musicUrl,
            String template,
            List<String> voiceClipUrls,
            List<String> sfxUrls,
            List<SubtitleItem> subtitles
    ) {
        /** 兼容旧版 7 参数构造（无 sfxUrls） */
        public AutoComposeRequest(List<String> videoClips, List<String> imageUrls, String scriptText,
                String musicUrl, String template, List<String> voiceClipUrls, List<SubtitleItem> subtitles) {
            this(videoClips, imageUrls, scriptText, musicUrl, template, voiceClipUrls, null, subtitles);
        }
    }

    /**
     * 3轨混音请求 (TTS+BGM+SFX)
     */
    record TtsBgmSfxMixRequest(
            String videoUrl,
            List<String> ttsUrls,
            String bgmUrl,
            List<String> sfxUrls,
            Double bgmVolume,
            Boolean bgmFadeIn,
            Boolean bgmFadeOut
    ) {}

    /**
     * 视频结果
     */
    record VideoResult(
            String videoUrl,
            Long duration,
            Long fileSize,
            String format
    ) {}
}
