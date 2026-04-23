package cn.gaifan.douyinOperations.module.wecom.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class WcPushRuleSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long ownerId;
    @NotNull(message = "机器人 ID 不能为空")
    private Long robotId;
    @NotBlank(message = "规则名称不能为空")
    private String ruleName;
    @NotBlank(message = "触发类型不能为空")
    private String triggerType;
    @NotBlank(message = "触发配置不能为空")
    private String triggerConfig;
    @NotBlank(message = "消息模板不能为空")
    private String messageTemplate;
    private Integer status;
}
