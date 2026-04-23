package cn.gaifan.douyinOperations.module.copy.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class CopyTemplateSearchVO extends BasicQueryDto {
    private String keyword;
    private String category;
    private Integer status;
    private Long userId;
}
