package cn.gaifan.douyinOperations.module.product.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnsureOptimizationVersionResultVO {

    /** dy_product_script.id */
    private Long scriptId;

    /** product_script_version.id，供 analyze/suggestions 等接口使用 */
    private Long scriptVersionId;

    /** true 表示本次新建了镜像行 */
    private boolean created;
}
