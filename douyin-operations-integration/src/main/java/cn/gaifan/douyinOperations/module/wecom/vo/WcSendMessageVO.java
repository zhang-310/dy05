package cn.gaifan.douyinOperations.module.wecom.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class WcSendMessageVO {
    @NotNull(message = "机器人 ID 不能为空")
    private Long robotId;
    private Long ruleId;
    @NotBlank(message = "消息内容不能为空")
    private String messageContent;
    private String messageType = "text";
}
