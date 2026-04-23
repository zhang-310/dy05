package cn.gaifan.douyinOperations.module.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "商品跨模块关联（直播使用等）")
public class ProductRelationsVO {

    @Schema(description = "商品 ID")
    private Long productId;

    @Schema(description = "商品名称")
    private String productName;

    @Schema(description = "直播场次使用（按场次 GMV 降序）")
    private List<ProductLiveUsageRowVO> liveSessions = new ArrayList<>();
}
