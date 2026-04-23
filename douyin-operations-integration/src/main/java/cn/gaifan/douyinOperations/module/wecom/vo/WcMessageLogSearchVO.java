package cn.gaifan.douyinOperations.module.wecom.vo;

import cn.gaifan.douyinOperations.common.vo.BasicQueryDto;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class WcMessageLogSearchVO extends BasicQueryDto {
    private Long ownerId;
    private Long robotId;
    private Long ruleId;
    private Integer status;
}
