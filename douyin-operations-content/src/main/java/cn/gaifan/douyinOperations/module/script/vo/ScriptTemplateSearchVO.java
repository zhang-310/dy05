package cn.gaifan.douyinOperations.module.script.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ScriptTemplateSearchVO extends BasicQueryDto {
    private String keyword;
    private String templateType;
    private String scene;
    private Integer status;
    private Long userId;
}
