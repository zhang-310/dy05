package cn.gaifan.douyinOperations.module.product.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 商品分类查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ProductCategorySearchVO extends BasicQueryDto {
    private String keyword;
    private String status;
}
