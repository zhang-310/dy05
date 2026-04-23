package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

/**
 * 批量添加产品单项 VO
 */
@Data
public class LiveProductBatchItemVO {
    private Long productId;
    private String productName;
    private String productType;

    /** 绑定的商品库话术 ID（dy_product_script.id），可选 */
    private Long productScriptId;
}
