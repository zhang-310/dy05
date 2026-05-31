package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;
import java.util.List;

@Data
public class VideoStyleAnalysisVO {
    private VisualStyle visualStyle;
    private List<String> contentTags;
    private List<EmotionPoint> emotionCurve;
    private double paceScore;           // 0-100, higher = faster
    private double engagementPrediction; // 0-100, predicted engagement

    @Data
    public static class VisualStyle {
        private String colorTone;       // warm, cool, neutral
        private String composition;     // center, rule-of-thirds, symmetric
        private String rhythm;          // fast, slow, variable
    }

    @Data
    public static class EmotionPoint {
        private double timestamp;       // seconds
        private String emotion;         // calm, rising, climax, cooldown
        private double intensity;       // 0-1
    }
}
