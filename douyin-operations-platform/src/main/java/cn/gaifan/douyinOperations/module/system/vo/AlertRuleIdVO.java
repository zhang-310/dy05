package cn.gaifan.douyinOperations.module.system.vo;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * P1-6: 告警规则 ID 请求 VO（强类型，避免 Map 手动转换）
 */
@Data
public class AlertRuleIdVO {

    @NotNull(message = "规则 ID 不能为空")
    private Long ruleId;
}
