package cn.gaifan.douyinOperations.module.ai.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class AiModelSaveVO {
    private Long id;

    @NotBlank(message = "模型名称不能为空")
    private String modelName;

    @NotBlank(message = "提供商不能为空")
    private String provider;

    private String endpoint;

    /** 可选；自定义 API Base URL（OpenAI 兼容根地址）。编辑时传入空字符串可清除覆盖 */
    private String apiBaseUrl;

    private String apiKey;

    @NotNull(message = "maxTokens 不能为空")
    private Integer maxTokens;

    @NotNull(message = "temperature 不能为空")
    private BigDecimal temperature;

    private Integer status;

    private Integer isDefault;
}
