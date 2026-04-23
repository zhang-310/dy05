package cn.gaifan.douyinOperations.common.service;

import java.util.List;

/**
 * 公共情绪曲线服务：解析情绪曲线、生成分段、推荐模板、BGM 节奏建议。
 * 供直播、短视频等模块复用。
 */
public interface EmotionCurveService {

    /** 情绪点：时间比例、情绪值、强度标签、BGM 音量、语速提示 */
    record EmotionPoint(
            double timeRatio,
            int emotionValue,
            String intensityLabel,
            int bgmVolumePercent,
            String speedHint
    ) {}

    /** 情绪分段：起止秒数、情绪值、描述 */
    record EmotionSegment(int startSec, int endSec, int emotionValue, String description) {}

    /** 情绪曲线模板 */
    record EmotionCurveTemplate(String code, String name, String curveExpression, String description) {}

    /** BGM 节奏建议 */
    record BgmRhythmSuggestion(String bpmRange, String rhythmDesc, String volumeHint) {}

    /**
     * 解析情绪曲线字符串，如 "100→80→60→120→90"
     */
    List<EmotionPoint> parseCurve(String curveExpression);

    /**
     * 根据视频时长和情绪曲线生成分段情绪标注
     */
    List<EmotionSegment> generateSegments(int durationSeconds, List<EmotionPoint> curve);

    /**
     * 推荐情绪曲线模板（基于内容类型）
     */
    List<EmotionCurveTemplate> recommendTemplates(String contentType);

    /**
     * 情绪曲线 → BGM 节奏建议
     */
    BgmRhythmSuggestion suggestBgmRhythm(List<EmotionPoint> curve);

    /**
     * 获取某个时间进度（0.0~1.0）对应的插值情绪点
     */
    EmotionPoint getEmotionAt(List<EmotionPoint> curve, double progress);
}
