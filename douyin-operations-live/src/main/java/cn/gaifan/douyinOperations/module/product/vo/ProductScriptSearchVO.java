package cn.gaifan.douyinOperations.module.product.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 产品话术版本查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductScriptSearchVO extends BasicQueryDto {

    /** 产品 ID（必填） */
    private Long productId;

    /** 风格过滤（可选） */
    private String style;

    /** 版本号过滤（可选） */
    private Integer versionNumber;

    /** 是否为当前版本：0=历史 1=当前（可选） */
    private Integer isActive;

    /** 最小效果评分（可选） */
    private java.math.BigDecimal minEffectivenessScore;

    /** 最大效果评分（可选） */
    private java.math.BigDecimal maxEffectivenessScore;
}
