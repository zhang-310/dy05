package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 推荐改进脚本 VO
 */
@Data
public class RecommendImprovementVO {

    /**
     * 分析结果 ID
     */
    @NotNull(message = "分析结果 ID 不能为空")
    private Long analysisId;

    /**
     * 返回前 K 个结果
     */
    private Integer topK = 10;
}
