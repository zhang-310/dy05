package cn.gaifan.douyinOperations.module.product.vo;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

/**
 * 商品话术导出为短视频项目请求。
 */
@Data
public class ProductScriptExportToShortVideoVO {

    @NotNull(message = "商品 ID 不能为空")
    @Positive(message = "商品 ID 必须为正数")
    private Long productId;

    /**
     * 指定 product_script_version.id；为空时使用 dy_product_script 的激活话术。
     */
    private Long versionId;

    /**
     * 短视频风格，覆盖源话术风格。
     */
    private String style;

    /**
     * 目标视频时长，秒。
     */
    private Integer duration;

    private Long personaId;
}
