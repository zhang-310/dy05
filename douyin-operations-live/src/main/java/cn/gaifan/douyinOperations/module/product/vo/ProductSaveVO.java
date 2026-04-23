package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ProductSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;
    @NotBlank(message = "商品名称不能为空")
    private String productName;
    private String productCategory;
    private String description;
    private String imageUrl;
    @NotNull(message = "售价不能为空")
    private BigDecimal price;
    private BigDecimal costPrice;
    private Long inventory;
    private String sku;
    private String barcode;
    private String manufacturer;
    private String tags;
    private Integer status;
    private Integer featured;
    /** 利润百分比：0.45=45%，与 lossPerUnit 二选一 */
    private BigDecimal profitMarginPct;
    /** 每单亏损金额（元），与 profitMarginPct 二选一 */
    private BigDecimal lossPerUnit;
    /** 控单策略：控3单、不控单1分钟下、控单憋单 等 */
    private String controlStrategy;
    /** 商品链接（抖音/淘宝等） */
    private String productLink;
    /** AI 提炼的卖点（多行文本） */
    private String aiSellingPoints;
}
