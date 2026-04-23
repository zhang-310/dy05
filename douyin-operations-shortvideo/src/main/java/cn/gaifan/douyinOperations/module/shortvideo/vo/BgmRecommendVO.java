package cn.gaifan.douyinOperations.module.shortvideo.vo;

import lombok.Data;

/**
 * BGM 推荐结果（Phase 2.7）
 */
@Data
public class BgmRecommendVO {
    private String style;
    private String bpmRange;
    private String mood;
    private int matchScore;
    private String reason;
}
