package cn.gaifan.douyinOperations.module.product.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 商品话术版本查询参数对象
 * 继承 BasicQueryDto 支持分页和排序
 *
 * @author Claude Code
 * @since 2026-03-06
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductScriptVersionSearchVO extends BasicQueryDto implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 产品 ID
     */
    private Long productId;

    /**
     * 话术风格
     */
    private String style;

    /**
     * 是否启用
     */
    private Boolean isActive;

    /**
     * 是否推荐
     */
    private Boolean isRecommended;

    /**
     * 是否归档
     */
    private Boolean archived;

    /**
     * 最小效果评分
     */
    private BigDecimal minEffectivenessScore;

    /**
     * 最大效果评分
     */
    private BigDecimal maxEffectivenessScore;

    /**
     * 搜索关键词
     */
    private String keyword;
}
