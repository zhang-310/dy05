package cn.gaifan.douyinOperations.module.system.vo;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * P1-6: 告警规则更新 VO（强类型，避免 Map 手动转换）
 */
@Data
public class AlertRuleUpdateVO {

    @NotNull(message = "规则 ID 不能为空")
    private Long ruleId;

    @NotBlank(message = "规则名称不能为空")
    private String name;

    @NotBlank(message = "指标名称不能为空")
    private String metricName;

    @NotBlank(message = "告警类型不能为空")
    private String type;

    @NotNull(message = "阈值不能为空")
    @Min(value = 0, message = "阈值必须大于等于 0")
    private Double threshold;

    @NotBlank(message = "比较操作符不能为空")
    private String operator;

    @NotNull(message = "持续时间不能为空")
    @Min(value = 1, message = "持续时间必须大于 0")
    private Integer duration;

    @NotBlank(message = "告警级别不能为空")
    private String severity;

    private String description;

    private Boolean enabled;
}
