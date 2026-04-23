package cn.gaifan.douyinOperations.module.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "商品在直播场次中的使用汇总（单行）")
public class ProductLiveUsageRowVO {

    @Schema(description = "场次 ID")
    private Long sessionId;

    @Schema(description = "直播标题")
    private String liveTitle;

    @Schema(description = "场次状态：0准备中 1直播中 2已结束 3已取消")
    private Integer status;

    @Schema(description = "该场次内本商品排品 GMV 汇总（live_product.revenue）")
    private BigDecimal sessionGmv;

    @Schema(description = "该场次内本商品排品行数")
    private Integer slotCount;
}
