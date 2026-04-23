package cn.gaifan.douyinOperations.module.ai.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.sql.Timestamp;

/**
 * 管理端模型列表/详情：不返回明文 apiKey，附带解析后的 Base URL 说明（与 OpenAiCompatibleLlmClient 一致）。
 */
@Data
public class AiModelAdminVO {
    private Long id;
    private String modelName;
    private String modelProvider;
    /** 对应请求体中的 model，存于表字段 model_version */
    private String modelVersion;
    /** 库内保存的自定义 Base URL（未填则为空，编辑表单回显用） */
    private String apiBaseUrl;
    /** 掩码后的密钥，如 ****abcd */
    private String apiKeyMasked;
    private Integer maxTokens;
    private BigDecimal temperature;
    private Integer status;
    private Integer isDefault;
    private BigDecimal costPer1kTokens;
    private Long quotaLimit;
    private Long quotaUsed;
    private Timestamp createTime;
    private Timestamp updateTime;
    /** 实际请求使用的 Base URL（有自定义则优先，否则与系统配置解析一致） */
    private String resolvedBaseUrl;
}
