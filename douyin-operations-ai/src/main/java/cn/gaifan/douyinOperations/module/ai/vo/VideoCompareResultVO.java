package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 视频对比分析结果 VO
 */
@Data
public class VideoCompareResultVO {

    /** 主视频信息 */
    private VideoInfo mainVideo;

    /** 对比视频列表 */
    private List<VideoInfo> compareVideos;

    /** 数据对比分析 */
    private DataComparison dataComparison;

    /** 内容结构对比 */
    private ContentComparison contentComparison;

    /** AI 分析报告 */
    private String analysisReport;

    /** 改进建议列表 */
    private List<Suggestion> suggestions;

    /** 生成时间（毫秒） */
    private Long generationTime;

    /** Token 使用量 */
    private Integer tokenUsage;

    @Data
    public static class VideoInfo {
        private Long videoId;
        private String title;
        private String coverUrl;
        private Long playCount;
        private Long likeCount;
        private Long commentCount;
        private Long shareCount;
        private Double completionRate;
        private String publishTime;
        private Integer duration;
    }

    @Data
    public static class DataComparison {
        /** 播放量对比 */
        private MetricComparison playCount;

        /** 点赞率对比 */
        private MetricComparison likeRate;

        /** 评论率对比 */
        private MetricComparison commentRate;

        /** 分享率对比 */
        private MetricComparison shareRate;

        /** 完播率对比 */
        private MetricComparison completionRate;
    }

    @Data
    public static class MetricComparison {
        private String metricName;
        private Double mainValue;
        private Double avgCompareValue;
        private Double maxCompareValue;
        private Double minCompareValue;
        private String trend; // higher/lower/similar
        private String analysis;
    }

    @Data
    public static class ContentComparison {
        /** 标题对比 */
        private TextComparison title;

        /** 封面对比 */
        private String coverAnalysis;

        /** 发布时间对比 */
        private String publishTimeAnalysis;

        /** 时长对比 */
        private String durationAnalysis;
    }

    @Data
    public static class TextComparison {
        private String mainText;
        private List<String> compareTexts;
        private String analysis;
        private List<String> keywords;
    }

    @Data
    public static class Suggestion {
        private String category; // title/cover/content/timing
        private String suggestion;
        private Integer priority; // 1-5, 5最高
        private String reason;
    }
}
