package cn.gaifan.douyinOperations.module.sms.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class SmsSendSaveVO {
    @NotNull(message = "用户 ID 不能为空")
    private Long ownerId;
    @NotBlank(message = "手机号不能为空")
    private String phoneNumber;
    @NotBlank(message = "模板代码不能为空")
    private String templateCode;
    private String bizType;
    private String bizId;
}
