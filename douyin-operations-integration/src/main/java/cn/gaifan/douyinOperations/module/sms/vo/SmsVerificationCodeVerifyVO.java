package cn.gaifan.douyinOperations.module.sms.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class SmsVerificationCodeVerifyVO {
    @NotNull(message = "用户 ID 不能为空")
    private Long ownerId;
    @NotBlank(message = "手机号不能为空")
    private String phoneNumber;
    @NotBlank(message = "业务类型不能为空")
    private String bizType;
    @NotBlank(message = "验证码不能为空")
    private String code;
    private String verifiedIp;
}
