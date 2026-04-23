package cn.gaifan.douyinOperations.module.config.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@Data
public class ConfigSaveVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "配置键不能为空")
    @Size(max = 128)
    private String configKey;

    private String configValue;

    @Size(max = 16)
    private String valueType = "string";

    private Integer isSensitive = 0;

    @Size(max = 64)
    @JsonProperty("configType")  // 前端使用 configType
    private String configGroup;

    @Size(max = 256)
    @JsonProperty("configName")  // 前端使用 configName
    private String remark;
}
