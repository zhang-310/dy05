package cn.gaifan.douyinOperations.module.wecom.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class WcRobotConfigSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long ownerId;
    @NotBlank(message = "机器人名称不能为空")
    private String robotName;
    @NotBlank(message = "Webhook 地址不能为空")
    private String webhookUrl;
    private String robotType;
    private Integer status;
    private String description;
}
