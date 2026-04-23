package cn.gaifan.douyinOperations.module.config.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigSearchVO extends BasicQueryDto {

    private String configKey;
    @JsonProperty("configType")  // 前端使用 configType，后端映射到 configGroup
    private String configGroup;
    private String keyword;
}
