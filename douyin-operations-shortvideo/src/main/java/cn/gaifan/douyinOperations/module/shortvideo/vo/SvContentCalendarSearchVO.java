package cn.gaifan.douyinOperations.module.shortvideo.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SvContentCalendarSearchVO extends BasicQueryDto {
    private String planDateFrom;
    private String planDateTo;
    private Long personaId;
    private Long accountId;
    private String contentType;
    private Integer status;
}
