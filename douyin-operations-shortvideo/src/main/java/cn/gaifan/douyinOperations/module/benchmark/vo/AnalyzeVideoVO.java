package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 分析视频请求VO
 */
@Data
public class AnalyzeVideoVO {

    /**
     * 视频ID
     */
    @NotNull(message = "视频ID不能为空")
    private Long benchmarkVideoId;

    /**
     * 是否强制重新分析（默认false）
     */
    private Boolean forceReanalyze = false;

    /**
     * 是否启用OCR（默认true）
     */
    private Boolean enableOcr = true;

    /**
     * 是否启用ASR（默认true）
     */
    private Boolean enableAsr = true;

    /**
     * 是否调用抖音API（默认true）
     */
    private Boolean enableApi = true;

    /**
     * AI模型（可选，不指定则使用默认）
     */
    private String aiModel;
}
