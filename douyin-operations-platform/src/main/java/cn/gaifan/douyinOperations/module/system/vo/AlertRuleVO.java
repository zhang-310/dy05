package cn.gaifan.douyinOperations.module.system.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 告警规则 VO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlertRuleVO {

    private Long id;

    @NotBlank(message = "规则名称不能为空")
    private String name;                // 规则名称

    @NotBlank(message = "指标名称不能为空")
    private String metricName;          // 指标名称

    @NotBlank(message = "告警类型不能为空")
    private String type;                // 阈值/异常/趋势

    @NotNull(message = "阈值不能为空")
    @Min(value = 0, message = "阈值必须大于等于 0")
    private Double threshold;           // 阈值

    @NotBlank(message = "比较操作符不能为空")
    private String operator;            // >/</=/>=/<=

    @NotNull(message = "持续时间不能为空")
    @Min(value = 1, message = "持续时间必须大于 0")
    private Integer duration;           // 持续时间（秒）

    @NotBlank(message = "告警级别不能为空")
    private String severity;            // warning/critical

    private String description;         // 规则描述

    private Boolean enabled;            // 是否启用
}
