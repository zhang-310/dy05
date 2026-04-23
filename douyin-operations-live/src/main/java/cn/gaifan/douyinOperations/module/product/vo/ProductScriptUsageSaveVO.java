package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 产品话术使用记录保存 VO
 */
@Data
public class ProductScriptUsageSaveVO {

    /** 商品话术 ID（必填） */
    @NotNull(message = "商品话术 ID 不能为空")
    private Long productScriptId;

    /** 直播脚本 ID（可选） */
    private Long liveScriptId;

    /** 直播场次 ID（可选） */
    private Long sessionId;

    /** 效果评分（可选） */
    private BigDecimal effectivenessScore;

    /** 转化率（可选） */
    private BigDecimal conversionRate;

    /** 销售金额（可选） */
    private BigDecimal salesAmount;
}
