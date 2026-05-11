package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 批量添加产品项 VO
 * Batch Add Product Item VO
 */
@Data
public class LiveProductBatchAddItemVO {

    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    private String productName;

    private String productType;

    private String imageUrl;

    private Double price;

    private Long productScriptId;
}
