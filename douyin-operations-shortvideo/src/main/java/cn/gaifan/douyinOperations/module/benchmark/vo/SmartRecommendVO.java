package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

/**
 * 智能推荐脚本 VO
 */
@Data
public class SmartRecommendVO {

    /**
     * 过滤条件
     */
    private Filters filters;

    /**
     * 参考文本（可选）
     */
    private String referenceText;

    /**
     * 返回前 K 个结果
     */
    private Integer topK = 10;

    /**
     * 过滤条件
     */
    @Data
    public static class Filters {
        /**
         * 行业
         */
        private String industry;

        /**
         * 场景类型
         */
        private String sceneType;

        /**
         * 脚本类型
         */
        private String scriptType;

        /**
         * 最低质量评分
         */
        private Double minQualityScore;
    }
}
