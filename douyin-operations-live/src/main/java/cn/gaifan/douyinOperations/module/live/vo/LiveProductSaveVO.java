package cn.gaifan.douyinOperations.module.live.vo;

import lombok.Data;

import jakarta.validation.constraints.NotNull;

/**
 * 直播产品保存/更新 VO
 */
@Data
public class LiveProductSaveVO {

    private Long id;

    @NotNull(message = "直播场次 ID 不能为空")
    private Long sessionId;

    @NotNull(message = "产品 ID 不能为空")
    private Long productId;

    private String productName;

    private Integer saleQuantity = 0;

    private Integer position;

    private String productType;

    private String scriptSource;

    private Long productScriptId;
}
