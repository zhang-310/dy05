package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 话术相似度检测结果
 */
@Data
public class SimilarityItemVO {
    private Long scriptId1;
    private Long scriptId2;
    private String type1;
    private String type2;
    /** high/medium */
    private String similarityLevel;
    private String suggestion;
}
