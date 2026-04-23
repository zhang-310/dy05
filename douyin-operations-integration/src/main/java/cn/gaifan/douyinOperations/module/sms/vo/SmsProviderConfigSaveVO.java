package cn.gaifan.douyinOperations.module.sms.vo;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class SmsProviderConfigSaveVO {
    private Long id;
    @NotNull(message = "用户 ID 不能为空")
    private Long ownerId;
    @NotBlank(message = "服务商代码不能为空")
    private String providerCode;
    @NotBlank(message = "服务商名称不能为空")
    private String providerName;
    @NotBlank(message = "API Key 不能为空")
    private String apiKey;
    @NotBlank(message = "API Secret 不能为空")
    private String apiSecret;
    private String appId;
    private String signName;
    private String region;
    private Integer status;
    private Integer isDefault;
    private Integer dailyQuota;
}
