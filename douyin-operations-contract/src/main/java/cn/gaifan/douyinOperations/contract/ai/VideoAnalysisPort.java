package cn.gaifan.douyinOperations.contract.ai;

/**
 * 视频分析 SPI — shortvideo ↔ intelligence 去耦
 */
public interface VideoAnalysisPort {

    /** 分析单条视频 */
    VideoAnalysisResult analyze(String videoUrl);

    /** 批量对比分析 */
    java.util.List<VideoAnalysisResult> batchAnalyze(java.util.List<String> videoUrls);

    record VideoAnalysisResult(
            String videoUrl,
            String structure,
            String scriptPattern,
            double viralScore,
            java.util.List<String> highlights
    ) {}
}
