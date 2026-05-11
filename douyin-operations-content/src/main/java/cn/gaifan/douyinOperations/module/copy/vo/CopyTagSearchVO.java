package cn.gaifan.douyinOperations.module.copy.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 文案标签查询 VO
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class CopyTagSearchVO extends BasicQueryDto {
    private String keyword;
    private String category;
}
