package cn.gaifan.douyinOperations.module.system.vo;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 外部 API 配置保存/更新 VO
 */
@Data
public class ExternalApiConfigSaveVO {

    /** 更新时传入 id，新增时为 null */
    private Long id;

    @NotBlank(message = "供应商编码不能为空")
    @Size(max = 64, message = "供应商编码最多64字符")
    private String providerCode;

    @NotBlank(message = "供应商名称不能为空")
    @Size(max = 128, message = "供应商名称最多128字符")
    private String providerName;

    private String category;

    @Size(max = 512, message = "基础URL最多512字符")
    private String baseUrl;

    /** API Key（明文传入，后端加密存储） */
    private String apiKey;

    /** API Secret（明文传入，后端加密存储） */
    private String apiSecret;

    private Boolean isEnabled = true;

    private Integer priority = 0;

    private Integer rateLimitPerMin;

    private Integer dailyQuota;

    private Integer monthlyQuota;

    /** 扩展配置 JSON 字符串 */
    private String extraConfig;
}
