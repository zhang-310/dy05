package cn.gaifan.douyinOperations.module.script.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class ViolationWordSearchVO extends BasicQueryDto {
    private String keyword;
    private Integer level;
    private String reason;
    private Integer status;
}
