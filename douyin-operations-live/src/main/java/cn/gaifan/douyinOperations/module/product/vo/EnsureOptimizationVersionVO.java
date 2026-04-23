package cn.gaifan.douyinOperations.module.product.vo;

import lombok.Data;

/**
 * 为商品库主话术（dy_product_script）确保存在可参与「优化链路」的 product_script_version 镜像
 */
@Data
public class EnsureOptimizationVersionVO {

    /** dy_product_script.id */
    private Long scriptId;

    /** 是否强制新建一条镜像（默认 false：已存在则复用） */
    private Boolean forceNew;
}
