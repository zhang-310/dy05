package cn.gaifan.douyinOperations.module.benchmark.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 按URL分析账号请求VO
 */
@Data
public class AnalyzeAccountByUrlVO {

    /**
     * 账号URL
     */
    @NotBlank(message = "账号URL不能为空")
    private String accountUrl;

    /**
     * 最小点赞数（默认1000）
     */
    private Integer minLikeCount = 1000;

    /**
     * 最大视频数（默认50）
     */
    private Integer maxVideos = 50;

    /**
     * Cookie ID（可选）
     */
    private Long cookieId;

    /**
     * 是否自动分析视频（默认true）
     */
    private Boolean autoAnalyze = true;
}
