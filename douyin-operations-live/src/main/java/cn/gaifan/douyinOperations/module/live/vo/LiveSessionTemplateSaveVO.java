package cn.gaifan.douyinOperations.module.live.vo;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LiveSessionTemplateSaveVO {

    private Long id;

    private Long ownerId;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    @NotBlank(message = "模板编码不能为空")
    private String code;

    private String description;

    /** JSON 数组：[{ "scriptType":"opening", "requirement":null, "durationLimitSec":null }, ...] */
    @NotBlank(message = "槽位结构 JSON 不能为空")
    private String structureJson;
}
