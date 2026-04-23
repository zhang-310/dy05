package cn.gaifan.douyinOperations.module.messaging.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class MsgPlatformConfigSaveVO {

    private Long id;

    private Long ownerId;

    @NotBlank(message = "平台不能为空")
    private String platform;

    private String appId;
    private String corpId;
    private String secret;
    private String callbackToken;
    private String callbackEncodingAesKey;
    private Long agentId;
    private Integer status;
}
