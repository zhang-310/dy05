package cn.gaifan.douyinOperations.module.benchmark.service;

import cn.gaifan.douyinOperations.module.benchmark.vo.AnalyzeVideoVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BenchmarkAnalysisVO;
import cn.gaifan.douyinOperations.module.benchmark.vo.BatchAnalyzeVideosVO;

import java.util.List;

/**
 * 深度分析服务
 * 协调9步分析流程
 */
public interface BenchmarkAnalysisService {

    /**
     * 分析单个视频
     * @param analyzeVO 分析参数
     * @param ownerId 用户ID
     * @return 分析结果
     */
    BenchmarkAnalysisVO analyzeVideo(AnalyzeVideoVO analyzeVO, Long ownerId);

    /**
     * 批量分析视频
     * @param batchVO 批量分析参数
     * @param ownerId 用户ID
     * @return 任务ID
     */
    Long batchAnalyzeVideos(BatchAnalyzeVideosVO batchVO, Long ownerId);

    /**
     * 根据视频ID获取分析结果
     */
    BenchmarkAnalysisVO getByVideoId(Long videoId, Long ownerId);

    /**
     * 合并多源内容
     * @param transcriptText ASR文案
     * @param ocrText OCR文案
     * @param apiDescription API描述
     * @return 合并后的内容
     */
    String mergeContent(String transcriptText, String ocrText, String apiDescription);
}
