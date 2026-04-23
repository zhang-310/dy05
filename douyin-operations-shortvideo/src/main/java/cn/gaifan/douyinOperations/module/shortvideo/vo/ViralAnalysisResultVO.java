package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;
import java.util.List;

/**
 * 爆款分析结构化结果（Phase 1 任务 1.5）
 */
@Data
public class ViralAnalysisResultVO {
    private String summary;
    private OpeningAnalysis opening;
    private ClimaxAnalysis climax;
    private EndingAnalysis ending;
    private List<EmotionPoint> emotionCurve;
    private BgmAnalysis bgm;
    private List<ViralElement> viralElements;
    private List<TransitionItem> transitions;
    private CopywritingAnalysis copywriting;
    private Double viralScore;
    private List<String> remakeAdvice;

    @Data
    public static class OpeningAnalysis {
        private String hook;
        private String suspenseType;
        private String emotionBase;
    }

    @Data
    public static class ClimaxAnalysis {
        private String timePoint;
        private String emotionPeak;
        private String turningTechnique;
    }

    @Data
    public static class EndingAnalysis {
        private String ctaType;
        private String suspenseCliffhanger;
        private String emotionLanding;
    }

    @Data
    public static class EmotionPoint {
        private Integer time;
        private Double value;
    }

    @Data
    public static class BgmAnalysis {
        private String style;
        private String bpm;
        private String emotionMatch;
    }

    @Data
    public static class ViralElement {
        private String name;
        private String timePoint;
        private Double score;
    }

    @Data
    public static class TransitionItem {
        private String type;
        private String timePoint;
    }

    @Data
    public static class CopywritingAnalysis {
        private List<String> goldenSentences;
        private List<String> techniques;
        private Double persuasionScore;
    }
}
