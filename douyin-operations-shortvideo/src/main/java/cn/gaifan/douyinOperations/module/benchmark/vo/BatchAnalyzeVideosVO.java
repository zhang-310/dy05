package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 批量分析视频请求VO
 */
@Data
public class BatchAnalyzeVideosVO {

    /**
     * 视频ID列表
     */
    @NotNull(message = "视频ID列表不能为空")
    private List<Long> videoIds;

    /**
     * 是否强制重新分析
     */
    private Boolean forceReanalyze = false;

    /**
     * 最大并发数（默认3）
     */
    private Integer maxConcurrency = 3;

    /**
     * AI模型
     */
    private String aiModel;
}
