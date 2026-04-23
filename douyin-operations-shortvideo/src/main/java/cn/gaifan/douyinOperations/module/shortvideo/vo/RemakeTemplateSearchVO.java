package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 二创模板查询 VO（Phase 5）
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RemakeTemplateSearchVO extends BasicQueryDto {

    private String remakeType;
    private String templateName;
}
