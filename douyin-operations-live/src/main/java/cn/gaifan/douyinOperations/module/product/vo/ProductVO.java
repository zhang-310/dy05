package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.sql.Timestamp;

@Data
public class ProductVO {
    private Long id;
    private Long userId;
    private String productName;
    private String productCategory;
    private String description;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal costPrice;
    private Long inventory;
    private String sku;
    private String barcode;
    private String manufacturer;
    private String tags;
    private Integer status;
    private Integer featured;
    private BigDecimal profitMarginPct;
    private BigDecimal lossPerUnit;
    private String controlStrategy;
    private String productLink;
    private String aiSellingPoints;
    private Timestamp createTime;
    private Timestamp updateTime;
}
