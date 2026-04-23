package cn.gaifan.douyinOperations.module.product.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品话术版本保存参数对象
 * 用于创建或更新话术版本
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptVersionSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 主键 ID（更新时使用）
     */
    private Long id;

    /**
     * 产品 ID（必填）
     */
    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    /**
     * 关联话术 ID（可选）
     */
    private Long scriptId;

    /**
     * 话术内容（必填）
     */
    @NotBlank(message = "话术内容不能为空")
    @Size(min = 1, max = 10000, message = "话术内容长度不能超过 10000 字符")
    private String content;

    /**
     * 话术风格（可选）
     */
    @Size(max = 64, message = "话术风格长度不能超过 64 字符")
    private String style;

    /**
     * 效果评分（可选，0-100）
     */
    private BigDecimal effectivenessScore;

    /**
     * 转化率（可选，百分比）
     */
    private BigDecimal conversionRate;

    /**
     * 是否启用（默认 true）
     */
    private Boolean isActive;

    /**
     * 是否推荐（默认 false）
     */
    private Boolean isRecommended;

    /**
     * 备注或描述
     */
    @Size(max = 500, message = "备注长度不能超过 500 字符")
    private String remark;
}
