package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 效果评分权重配置 - 保存入参
 */
@Data
public class LiveEffectivenessConfigSaveVO {

    /** 配置 ID（为空则新建，非空则更新） */
    private Long id;

    /** 配置名称 */
    @NotBlank(message = "配置名称不能为空")
    @Size(max = 128, message = "配置名称最长 128 个字符")
    private String configName;

    /** 转化率权重 0.00 ~ 1.00 */
    @NotNull(message = "转化率权重不能为空")
    @DecimalMin(value = "0.00", message = "权重不能小于 0")
    @DecimalMax(value = "1.00", message = "权重不能大于 1")
    private BigDecimal conversionWeight;

    /** 互动量权重 0.00 ~ 1.00 */
    @NotNull(message = "互动量权重不能为空")
    @DecimalMin(value = "0.00", message = "权重不能小于 0")
    @DecimalMax(value = "1.00", message = "权重不能大于 1")
    private BigDecimal interactionWeight;

    /** 留存率权重 0.00 ~ 1.00 */
    @NotNull(message = "留存率权重不能为空")
    @DecimalMin(value = "0.00", message = "权重不能小于 0")
    @DecimalMax(value = "1.00", message = "权重不能大于 1")
    private BigDecimal retentionWeight;

    /** GMV 权重 0.00 ~ 1.00 */
    @NotNull(message = "GMV 权重不能为空")
    @DecimalMin(value = "0.00", message = "权重不能小于 0")
    @DecimalMax(value = "1.00", message = "权重不能大于 1")
    private BigDecimal gmvWeight;

    /** 观看量权重（可选，兼容旧接口） */
    @DecimalMin(value = "0.00", message = "权重不能小于 0")
    @DecimalMax(value = "1.00", message = "权重不能大于 1")
    private BigDecimal viewerWeight;
}
