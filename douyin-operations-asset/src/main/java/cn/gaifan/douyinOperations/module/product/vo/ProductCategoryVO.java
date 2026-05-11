package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 商品分类 VO
 */
@Data
public class ProductCategoryVO {
    private Long id;
    private String name;
    private String description;
    private Integer productCount;
    private String status;
    private LocalDateTime createTime;
}
