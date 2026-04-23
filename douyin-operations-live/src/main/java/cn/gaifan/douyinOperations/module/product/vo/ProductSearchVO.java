package cn.gaifan.douyinOperations.module.product.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProductSearchVO extends BasicQueryDto {
    private String keyword;
    private String productCategory;
    private Integer status;
    private Integer featured;
    private Long userId;
    private List<Long> userIds;
}
