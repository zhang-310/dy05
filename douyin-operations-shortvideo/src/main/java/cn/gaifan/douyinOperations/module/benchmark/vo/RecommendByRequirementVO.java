package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 根据需求推荐脚本 VO
 */
@Data
public class RecommendByRequirementVO {

    /**
     * 需求描述
     */
    @NotBlank(message = "需求描述不能为空")
    private String requirement;

    /**
     * 返回前 K 个结果
     */
    private Integer topK = 10;
}
