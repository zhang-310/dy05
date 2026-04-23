package cn.gaifan.douyinOperations.module.config.dto;

/**
 * SysConfig Projection DTO - 仅包含必要字段
 */
public interface ConfigDTO {
    Long getId();
    String getConfigKey();
    String getConfigValue();
    String getValueType();
    Integer getIsSensitive();
    String getConfigGroup();
    String getRemark();
}
