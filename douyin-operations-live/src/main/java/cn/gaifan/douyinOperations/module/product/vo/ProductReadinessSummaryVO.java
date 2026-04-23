package cn.gaifan.douyinOperations.module.product.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 商品库就绪度汇总（与 {@link ProductSearchVO} 同一套筛选条件，忽略分页）。
 */
@Data
@Schema(description = "商品就绪度汇总")
public class ProductReadinessSummaryVO {

    @Schema(description = "当前筛选下商品总数")
    private long totalProducts;

    @Schema(description = "已填写商品链接的数量")
    private long withProductLink;

    @Schema(description = "已提取/填写 AI 卖点的数量")
    private long withSellingPoints;

    @Schema(description = "至少有一条主话术（dy_product_script，未删除）的商品数")
    private long withAtLeastOneScript;

    @Schema(description = "至少有一条已激活主话术的商品数")
    private long withActiveScript;
}
