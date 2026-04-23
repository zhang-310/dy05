package cn.gaifan.douyinOperations.module.benchmark.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;

/**
 * 根据脚本类型推荐脚本 VO
 */
@Data
public class RecommendByScriptTypeVO {

    /**
     * 脚本类型
     */
    @NotBlank(message = "脚本类型不能为空")
    private String scriptType;

    /**
     * 参考脚本 ID（可选）
     */
    private Long referenceScriptId;

    /**
     * 返回前 K 个结果
     */
    private Integer topK = 10;
}
