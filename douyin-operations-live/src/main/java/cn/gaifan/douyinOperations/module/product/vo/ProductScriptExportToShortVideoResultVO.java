package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 商品话术导出为短视频项目结果。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductScriptExportToShortVideoResultVO {

    private Long scriptId;
    private Long projectId;
    private String projectName;
}
