package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

/**
 * 根据行业和场景推荐脚本 VO
 */
@Data
public class RecommendByIndustrySceneVO {

    /**
     * 行业
     */
    private String industry;

    /**
     * 场景类型
     */
    private String sceneType;

    /**
     * 返回前 K 个结果
     */
    private Integer topK = 10;
}
