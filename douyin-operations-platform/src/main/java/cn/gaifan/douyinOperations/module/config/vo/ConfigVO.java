package cn.gaifan.douyinOperations.module.config.vo;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.io.Serializable;

@Data
public class ConfigVO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String configKey;
    /** 展示用：敏感项已脱敏 */
    private String configValue;
    private String valueType;
    private Integer isSensitive;
    @JsonProperty("configType")  // 前端使用 configType
    private String configGroup;
    @JsonProperty("configName")  // 前端使用 configName，映射 remark
    private String remark;
}
